/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.paymentconsent.ui

import org.mifosx.openbanking.core.data.util.RemoteException
import org.mifosx.openbanking.core.data.util.obieErrorCode
import org.mifosx.openbanking.core.data.util.obieMessage
import org.mifosx.openbanking.core.data.util.obieSupportReference
import org.mifosx.openbanking.core.model.banking.payment.ConsentType
import template.core.base.network.NetworkError
import template.core.base.ui.viewmodel.BackgroundEvent

/**
 * Why an authorisation return failed.
 *
 * [StateMismatch] and [NoPendingAuthorisation] used to be one signal and are deliberately no longer.
 * A callback whose `state` does not match the one this app issued is a replay signal and must not be
 * trusted at all. A callback with *nothing* pending is not: there is simply no authorisation left to
 * check it against. Both are terminal, but only the first is a security event, and saying so of the
 * second would accuse the customer of an attack for something they did not do.
 */
enum class PaymentConsentErrorKind {
    StateMismatch,
    NoPendingAuthorisation,
    CodeExpired,
    ConsentRejected,
    AuthorisationTimedOut,
    NetworkError,

    /**
     * The authorisation succeeded but the staged instruction is not in storage, so there is nothing
     * to submit.
     *
     * Terminal for *this* consent: it is never re-submitted against, because rebuilding an
     * `Initiation` here would send one the consent was not granted for. Starting a fresh payment is
     * a different thing and is still offered — that stages a new consent under new keys, and it is
     * what this kind's own copy tells the customer to do.
     */
    NoStagedPayment,

    /**
     * The bank answered the funds check negatively. Submitting anyway would knowingly send a payment
     * it has just said the account cannot cover.
     */
    InsufficientFunds,

    /** The submission itself was refused. The consent is spent either way. */
    SubmissionFailed,

    /**
     * The bank refused the request outright — a `400`, with an OBIE code saying which field it
     * objected to.
     *
     * Kept apart from [NetworkError] because the two need opposite advice. A connection failure is
     * worth retrying; a refusal is not, and will be refused identically for as long as the request
     * says the same thing. This also catches the synthetic `BadRequest("no ConsentId")` the
     * repository manufactures when a `201` arrives without one.
     */
    RequestRejected,

    /**
     * The bank answered, and the app could not read the answer.
     *
     * Nothing had been submitted at this point, so the money is provably still where it was. It is
     * not a network failure — the request and the response both completed — and it is not the bank's
     * refusal either; it is ours. Retrying cannot help: the same reply would fail to decode again.
     */
    ResponseUnreadable,

    /**
     * The submission returned success and the app could not read the reply.
     *
     * The dangerous one. A `2xx` here means the bank almost certainly created the payment and we
     * merely lost the identifier, so this must never claim the money has not moved — unlike
     * [ResponseUnreadable], which sits before the submission and can say so honestly. Its copy sends
     * the customer to check their recent payments rather than reassuring them or inviting a retry
     * that could pay twice.
     */
    SubmissionUnconfirmed,
}

/**
 * What the bank said about a failure, as opposed to what this app decided to call it.
 *
 * Carried separately from [PaymentConsentErrorKind] because the kind chooses the panel while these
 * describe the individual failure. All three are already parsed by `ObieErrorCodes`; before this they
 * were read off the wire and thrown away, so a refused payment left nothing to diagnose it with.
 */
data class PaymentConsentErrorDetail(
    /** The bank's own explanation. Preferred to app-authored copy — it knows why it refused. */
    val message: String? = null,
    /** The OBIE code, e.g. `U005`. */
    val code: String? = null,
    /** The envelope's `Id`, which is what makes a support call traceable. */
    val supportReference: String? = null,
) {
    val isEmpty: Boolean get() = message == null && code == null && supportReference == null
}

internal fun PaymentConsentErrorKind.description(): String = when (this) {
    PaymentConsentErrorKind.StateMismatch -> "Authorisation verification failed"
    PaymentConsentErrorKind.NoPendingAuthorisation -> "No payment in progress"
    PaymentConsentErrorKind.CodeExpired -> "Authorisation code expired"
    PaymentConsentErrorKind.ConsentRejected -> "Payment declined by HSBC"
    PaymentConsentErrorKind.AuthorisationTimedOut -> "Authorisation timed out"
    PaymentConsentErrorKind.NetworkError -> "Connection failed"
    PaymentConsentErrorKind.NoStagedPayment -> "Payment instruction lost"
    PaymentConsentErrorKind.InsufficientFunds -> "Insufficient funds"
    PaymentConsentErrorKind.SubmissionFailed -> "Payment refused by HSBC"
    PaymentConsentErrorKind.RequestRejected -> "Request rejected by HSBC"
    PaymentConsentErrorKind.ResponseUnreadable -> "Could not read the bank's reply"
    PaymentConsentErrorKind.SubmissionUnconfirmed -> "Submission not confirmed"
}

/**
 * What gets written to payment history.
 *
 * The bank's code and message are appended rather than replaced, because the two answer different
 * questions later: the kind says what the app did about it, the code and message say what the bank
 * objected to. Recording only the first left every rejected payment looking alike.
 */
internal fun PaymentConsentErrorKind.description(detail: PaymentConsentErrorDetail?): String =
    listOfNotNull(
        description(),
        detail?.code?.let { "[$it]" },
        detail?.message,
    ).joinToString(" ")

/**
 * The progress states are separate rather than one `loading` so the copy can say which stage is
 * running. A payment that appears to hang is otherwise indistinguishable from one that has already
 * failed — and this screen now runs the whole tail of the journey, from validating the redirect
 * through to the bank accepting the payment, which is several seconds of waiting to account for.
 *
 * Deliberately **rail-agnostic**: nothing here names a domestic payment, an international one or a
 * consent type. Every rail that ends in a redirect back from the bank — scheduled payments, standing
 * orders, VRP — returns through the same states, so a new rail should need a new producer, not a new
 * state model.
 */
sealed interface PaymentConsentUiState {

    /** Checking the callback's `state` and `nonce` against the authorisation that was launched. */
    data object Validating : PaymentConsentUiState

    /** Trading the authorization code for the payments-scoped PSU token. */
    data object Exchanging : PaymentConsentUiState

    /**
     * Polling until the consent reports `AUTH`.
     *
     * Load-bearing, not cosmetic: the bank refuses a submission against a consent that has not
     * reached `AUTH`, so the submission is gated on this. No OBIE error code is named — the one
     * this comment used to cite appears in no captured response. It offers Check again rather than
     * spinning indefinitely, because a bank that is slow to authorise is a normal outcome.
     */
    data class Checking(val canCheckAgain: Boolean = false) : PaymentConsentUiState

    /**
     * The bank has authorised the consent.
     *
     * Short-lived by design — the funds check starts immediately after — but it is the one moment
     * the customer learns their approval landed, and it is the only positive outcome this screen
     * ever shows, since a submitted payment leaves for the receipt. Without it the screen goes from
     * "waiting for your bank" straight to "checking the money is available" and never confirms that
     * the thing the customer just did at the bank actually worked.
     */
    data object Approved : PaymentConsentUiState

    /**
     * The bank reports the consent as already consumed — it has created the payment.
     *
     * Modelled as its own state rather than as an [Error] because it is not one: the payment exists.
     * It is reached when the app loses the thread after a submission the bank accepted — the process
     * dies between the submission returning and the session being cleared, and the redirect is
     * re-validated on restore.
     *
     * The whole point of naming it is that the alternative was silence: with no case for this
     * status the poll simply ran out and reported a timeout, whose copy promises no money has been
     * moved. Saying that about a payment the bank has already created is the one claim on this
     * screen that must never be wrong.
     */
    data object AlreadySubmitted : PaymentConsentUiState

    /** Asking the bank whether the debtor account can cover the staged amount. */
    data object ConfirmingFunds : PaymentConsentUiState

    /**
     * The payment itself is with the bank.
     *
     * The one stage the customer must not interrupt: the instruction has left, and until the
     * response arrives its outcome is genuinely unknown to the app.
     */
    data object Submitting : PaymentConsentUiState

    /**
     * [detail] is what the bank said, when it said anything. Null for failures the app decided by
     * itself — a `state` mismatch, a missing staged draft — which have no bank response behind them.
     */
    data class Error(
        val kind: PaymentConsentErrorKind,
        val detail: PaymentConsentErrorDetail? = null,
    ) : PaymentConsentUiState
}

/**
 * @property consentType the rail being authorised, which selects the screen's copy. Null until the
 *   callback validates, and on a rail this screen cannot name — the copy then falls back to the
 *   immediate-payment wording. It lives here rather than on [PaymentConsentUiState] so that the
 *   state model stays rail-agnostic.
 */
data class PaymentConsentState(
    val uiState: PaymentConsentUiState = PaymentConsentUiState.Validating,
    val consentId: String = "",
    val consentType: ConsentType? = null,
)

sealed interface PaymentConsentAction {
    data object CheckAgain : PaymentConsentAction
    data object RetryAuthorisation : PaymentConsentAction
    data object AbandonPayment : PaymentConsentAction
}

/**
 * Where the journey goes once this screen is finished with it.
 *
 * Only [PaymentSubmitted] carries anything, because it is the only outcome with something to show:
 * a payment the bank has accepted, identified by the id its receipt is read back under.
 */
/**
 * Every event here is a [BackgroundEvent], and that is load-bearing rather than defensive.
 *
 * `EventsEffect` filters events out — permanently, not queued — unless the screen is `RESUMED` or the
 * event opts out. That default exists to stop a backgrounded screen navigating twice, and it is right
 * for a screen the customer is looking at. This screen is the opposite case: the authorisation leg
 * sends them to the bank and the whole tail of the journey — code exchange, consent read, funds
 * check, submission — runs here while the app may still be behind the browser. Observed live, that
 * tail took 25 seconds.
 *
 * Without this, a payment that fully succeeded emitted [PaymentSubmitted] into a screen that was not
 * `RESUMED`, the event was dropped, and the customer sat on a progress panel forever while their
 * money had already moved and the receipt existed. All three cases are terminal handoffs, so losing
 * any of them strands the journey.
 *
 * The duplicate-navigation risk the default guards against does not apply: each is emitted once, at
 * the end of a flow that then clears its session, and the channel delivers it once.
 */
sealed interface PaymentConsentEvent : BackgroundEvent {
    data class PaymentSubmitted(val paymentId: String) : PaymentConsentEvent
    data object RestartAuthorisation : PaymentConsentEvent
    data object Abandoned : PaymentConsentEvent
}

/**
 * Classifies a failure on the authorisation leg — the code exchange, the consent read, the funds
 * check. Everything up to, but not including, the submission.
 *
 * Every outcome here is one where the payment provably has not left, which is what lets all of them
 * carry the "no money has been moved" reassurance:
 *  - `401` — the code or the PSU token is spent, so the exchange never happened.
 *  - `403` — the bank refused the consent outright.
 *  - `404` — the bank does not recognise the consent. Reported as expiry, not as
 *    [PaymentConsentErrorKind.NoPendingAuthorisation]: from this side a consent the bank has lost
 *    track of is indistinguishable from one that aged out, and the two must not share a state.
 *    "Nothing was waiting" describes an app with no session, which is a different event and — unlike
 *    this one — is a dead end rather than something to start again from.
 *  - `429` and `5xx` — the bank did not answer in a usable time. Read as a timeout rather than a
 *    connection failure, because the request did reach it.
 *
 * A `400` and a decode failure are named rather than left to fall through. Both used to land on
 * [PaymentConsentErrorKind.NetworkError], so a refusal the bank explained and a reply the app could
 * not read were each reported as "Connection failed", under a Retry that could never have worked.
 * `send-money` had already been corrected for exactly this; this leg never was.
 *
 * Anything else, including transport failures, is reported as a connection failure.
 *
 * The submission has its own, more conservative classifier: see [classifySubmissionError].
 */
internal fun classifyPaymentConsentError(throwable: Throwable): PaymentConsentErrorKind =
    when ((throwable as? RemoteException)?.networkError) {
        is NetworkError.Client.Unauthorized -> PaymentConsentErrorKind.CodeExpired
        is NetworkError.Client.Forbidden -> PaymentConsentErrorKind.ConsentRejected
        is NetworkError.Client.NotFound -> PaymentConsentErrorKind.CodeExpired
        is NetworkError.Client.RateLimited -> PaymentConsentErrorKind.AuthorisationTimedOut
        is NetworkError.Server -> PaymentConsentErrorKind.AuthorisationTimedOut
        is NetworkError.Client.BadRequest -> PaymentConsentErrorKind.RequestRejected
        is NetworkError.Serialization -> PaymentConsentErrorKind.ResponseUnreadable
        else -> PaymentConsentErrorKind.NetworkError
    }

/**
 * The bank's own account of a failure, or null when it gave none.
 *
 * Every one of these is already parsed by `ObieErrorCodes` and was being discarded. Returns null
 * rather than an empty object so callers can tell "the bank said nothing" from "the bank said this".
 */
internal fun errorDetailOf(throwable: Throwable): PaymentConsentErrorDetail? =
    PaymentConsentErrorDetail(
        message = throwable.obieMessage(),
        code = throwable.obieErrorCode(),
        supportReference = throwable.obieSupportReference(),
    ).takeUnless { it.isEmpty }

/**
 * Classifies a failure of the submission itself, which cannot use [classifyPaymentConsentError].
 *
 * The difference is what may be promised afterwards. Once the instruction is with the bank, a
 * timeout or a dropped connection says nothing about whether the payment was taken — so those keep
 * [PaymentConsentErrorKind.SubmissionFailed], whose copy asks the customer to check their recent
 * transactions rather than telling them nothing happened.
 *
 * Only a credential the bank rejected before it could process anything — `401` or `403` — is safe to
 * report as a clean failure. A `400` joins them: the bank declined to create the payment, so nothing
 * was created and the reassurance still holds.
 *
 * A decode failure is the one case that must never reassure, and the reason this classifier cannot
 * share [classifyPaymentConsentError]'s answer for it. A `2xx` the app could not read means the bank
 * accepted the instruction and only the identifier was lost, so the money may well have gone.
 * Reporting it as [PaymentConsentErrorKind.ResponseUnreadable] — which promises nothing has moved —
 * would be the most expensive wrong answer this screen is capable of.
 */
internal fun classifySubmissionError(throwable: Throwable): PaymentConsentErrorKind =
    when ((throwable as? RemoteException)?.networkError) {
        is NetworkError.Client.Unauthorized -> PaymentConsentErrorKind.CodeExpired
        is NetworkError.Client.Forbidden -> PaymentConsentErrorKind.ConsentRejected
        is NetworkError.Client.BadRequest -> PaymentConsentErrorKind.RequestRejected
        is NetworkError.Serialization -> PaymentConsentErrorKind.SubmissionUnconfirmed
        else -> PaymentConsentErrorKind.SubmissionFailed
    }
