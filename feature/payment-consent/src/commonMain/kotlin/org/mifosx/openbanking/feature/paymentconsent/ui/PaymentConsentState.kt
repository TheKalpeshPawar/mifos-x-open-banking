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
import template.core.base.network.NetworkError

/**
 * Why an authorisation return failed.
 *
 * [StateMismatch] and [NoPendingAuthorisation] are both replay signals: a callback arriving with no
 * matching pending authorisation is not something to retry through, it is something that must not be
 * trusted at all.
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
     * to submit. Terminal, and not offered a retry: resubmitting would mean rebuilding an
     * `Initiation` the consent was not granted against.
     */
    NoStagedPayment,

    /**
     * The bank answered the funds check negatively. Submitting anyway would knowingly send a payment
     * it has just said the account cannot cover.
     */
    InsufficientFunds,

    /** The submission itself was refused. The consent is spent either way. */
    SubmissionFailed,
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
}

/**
 * The progress states are separate rather than one `loading` so the copy can say which stage is
 * running. A payment that appears to hang is otherwise indistinguishable from one that has already
 * failed — and this screen now runs the whole tail of the journey, from validating the redirect
 * through to the bank accepting the payment, which is several seconds of waiting to account for.
 */
sealed interface PaymentConsentUiState {

    /** Checking the callback's `state` and `nonce` against the authorisation that was launched. */
    data object Validating : PaymentConsentUiState

    /** Trading the authorization code for the payments-scoped PSU token. */
    data object Exchanging : PaymentConsentUiState

    /**
     * Polling until the consent reports `Authorised`.
     *
     * Load-bearing, not cosmetic: submitting against a consent that has not reached `AUTH` returns
     * `400 U009`, so the submission is gated on this. It offers Check again rather than spinning
     * indefinitely, because a bank that is slow to authorise is a normal outcome.
     */
    data class Checking(val canCheckAgain: Boolean = false) : PaymentConsentUiState

    /** Asking the bank whether the debtor account can cover the staged amount. */
    data object ConfirmingFunds : PaymentConsentUiState

    /**
     * The payment itself is with the bank.
     *
     * The one stage the customer must not interrupt: the instruction has left, and until the
     * response arrives its outcome is genuinely unknown to the app.
     */
    data object Submitting : PaymentConsentUiState

    data class Error(val kind: PaymentConsentErrorKind) : PaymentConsentUiState
}

data class PaymentConsentState(
    val uiState: PaymentConsentUiState = PaymentConsentUiState.Validating,
    val consentId: String = "",
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
sealed interface PaymentConsentEvent {
    data class PaymentSubmitted(val paymentId: String) : PaymentConsentEvent
    data object RestartAuthorisation : PaymentConsentEvent
    data object Abandoned : PaymentConsentEvent
}

internal fun classifyPaymentConsentError(throwable: Throwable): PaymentConsentErrorKind =
    when ((throwable as? RemoteException)?.networkError) {
        is NetworkError.Client.Unauthorized -> PaymentConsentErrorKind.CodeExpired
        is NetworkError.Client.Forbidden -> PaymentConsentErrorKind.ConsentRejected
        else -> PaymentConsentErrorKind.NetworkError
    }
