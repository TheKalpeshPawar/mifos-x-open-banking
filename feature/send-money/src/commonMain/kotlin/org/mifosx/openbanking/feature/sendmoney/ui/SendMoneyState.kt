/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.sendmoney.ui

import org.mifosx.openbanking.core.data.util.RemoteException
import org.mifosx.openbanking.core.data.util.isDebtorAccountRefusal
import org.mifosx.openbanking.core.data.util.obieErrorCode
import org.mifosx.openbanking.core.data.util.obieSupportReference
import org.mifosx.openbanking.core.model.banking.BankAccount
import org.mifosx.openbanking.core.model.banking.payment.CreditorSelection
import org.mifosx.openbanking.core.model.banking.payment.PaymentDraft
import template.core.base.network.NetworkError

/**
 * The three steps the form moves through inside a single `Content` state.
 *
 * Modelled as data rather than as separate screen states because the step is a property of one form
 * — the selections made in earlier steps stay live, and going back must not discard them.
 */
enum class SendMoneyStep {
    Recipient,
    Amount,
    Review,
}

/**
 * How far the hand-off to the bank has got. Distinct from [SendMoneyStep]: nothing is editable here.
 *
 * It stops at [AwaitingAuthorisation] because that is where this screen's part ends: the customer
 * leaves for the bank, and the leg that returns — payment-consent — confirms funds and submits.
 */
enum class SendMoneyStage {
    StagingConsent,
    AwaitingAuthorisation,
}

/**
 * Why a payment failed, and therefore what to offer next.
 *
 * Ten kinds and four recoveries, deliberately not collapsed: retrying a revoked consent will never
 * succeed, and re-authorising an insufficient balance does not add money to the account.
 */
enum class SendMoneyErrorKind {
    /** `U019` — the JWS was missing or rejected. A defect in this app; the PSU can do nothing. */
    SignatureMissing,

    /** `U009` — submitted before the PSU finished authorising. */
    ConsentNotAuthorised,

    /** `U008` — the submitted instruction diverged from the staged one. Must be re-staged. */
    ConsentMismatch,

    /** `U014` — outside the limits agreed on the consent. */
    OutsideControlParameters,

    /** `U002` — a field the bank would not accept. */
    InvalidField,

    /** The consent was withdrawn. */
    ConsentRevoked,

    /** The payments token expired before the payment was submitted. */
    TokenExpired,

    RateLimited,

    /** The funds confirmation came back false, so nothing was submitted. */
    InsufficientFunds,

    /**
     * The bank refused the account the payment would come FROM.
     *
     * Distinct from [InvalidField] because there is nothing to correct: the details are right, the
     * account simply cannot send payments. Telling someone to check them would send them looking
     * for a mistake they did not make. Observed on a credit card (`U021`) and a Global Money wallet
     * (`U002`), both on `Data.Initiation.DebtorAccount.Identification`.
     */
    PayerNotSupported,

    NetworkError,
}

/**
 * Whether offering Retry makes sense.
 *
 * Retry replays the same idempotency key against the same consent, so it only helps when the
 * failure was transport or credential shaped. Everything else needs a different action.
 */
internal val SendMoneyErrorKind.isRetryable: Boolean
    get() = this == SendMoneyErrorKind.NetworkError ||
        this == SendMoneyErrorKind.RateLimited ||
        this == SendMoneyErrorKind.TokenExpired

/** Whether the PSU should be sent back to re-authorise at the bank. */
internal val SendMoneyErrorKind.needsReauthorisation: Boolean
    get() = this == SendMoneyErrorKind.ConsentNotAuthorised || this == SendMoneyErrorKind.TokenExpired

/** Whether the amount is the thing to change. */
internal val SendMoneyErrorKind.needsAmountChange: Boolean
    get() = this == SendMoneyErrorKind.OutsideControlParameters ||
        this == SendMoneyErrorKind.InsufficientFunds

/**
 * Which manual-entry fields are currently wrong.
 *
 * Per-field rather than one message, because both can be wrong at once and a single slot would hide
 * one of them. Flags rather than text: the wording is a string resource the composable resolves.
 */
data class SendMoneyFieldErrors(
    val sortCodeInvalid: Boolean = false,
    val accountNumberInvalid: Boolean = false,
) {
    val isEmpty: Boolean
        get() = !sortCodeInvalid && !accountNumberInvalid
}

/** Why the entered amount is not yet payable. */
enum class SendMoneyAmountProblem {
    NotANumber,
    NotPositive,
    ExceedsAvailableBalance,
}

/** One selectable payee row. Its name comes from the bank and needs no resolving. */
data class SendMoneyPickerRow(
    val id: String,
    val initials: String,
    val headline: String,
    val supporting: String,
)

/**
 * One selectable account row, carrying the raw fields rather than a finished label.
 *
 * HSBC leaves `Nickname` blank on most accounts, so the readable name — "Current account ·· 3349" —
 * has to be derived. That derivation lives in `core/ui`'s `accountDisplayName`, which is
 * `@Composable` because it resolves a string resource per account type, so it cannot run in the
 * ViewModel. Passing the ingredients up and resolving them at render keeps this list showing exactly
 * what Home, Accounts and account-detail show, instead of a second, emptier answer.
 */
data class SendMoneyAccountRow(
    val id: String,
    val nickname: String,
    val accountSubType: String,
    val accountNumber: String,
    val rawIdentification: String,
    val supporting: String,
)

sealed interface SendMoneyUiState {

    data object Loading : SendMoneyUiState

    /**
     * The form. [step] moves through it; everything else survives the whole way, so returning to an
     * earlier step from an error keeps what the PSU already chose.
     *
     * @property availableBalanceMinorUnits Advisory only. The binding check is the bank's funds
     *   confirmation, which can refuse a payment this comparison allows.
     */
    data class Content(
        val step: SendMoneyStep,
        val debtorAccounts: List<BankAccount>,
        val beneficiaries: List<SendMoneyPickerRow>,
        val debtorRows: List<SendMoneyAccountRow>,
        val debtorAccountId: String? = null,
        val creditor: CreditorSelection? = null,
        val creditorLabel: String = "",
        val creditorSupporting: String = "",
        val debtorAccountRow: SendMoneyAccountRow? = null,
        val manualEntryVisible: Boolean = false,
        val manualSortCode: String = "",
        val manualAccountNumber: String = "",
        val manualName: String = "",
        val amountMinorUnits: String = "",
        val amountLabel: String = "",
        val reference: String = "",
        val amountProblem: SendMoneyAmountProblem? = null,
        val fieldErrors: SendMoneyFieldErrors = SendMoneyFieldErrors(),
        val availableBalanceMinorUnits: Long? = null,
    ) : SendMoneyUiState {

        val stepIndex: Int
            get() = step.ordinal + 1

        /** Review is reachable only once the amount is present and unobjectionable. */
        val canReview: Boolean
            get() = amountProblem == null && amountMinorUnits.isNotBlank() && creditor != null

        val hasBeneficiaries: Boolean
            get() = beneficiaries.isNotEmpty()
    }

    /** In flight. There is no CTA at all here — see the note on [SendMoneyStage]. */
    data class Submitting(
        val stage: SendMoneyStage,
        val amountLabel: String,
        val creditorName: String,
        val consentId: String? = null,
    ) : SendMoneyUiState

    data class Error(
        val kind: SendMoneyErrorKind,
        val supportReference: String? = null,
    ) : SendMoneyUiState
}

/**
 * @property draft The instruction as it was staged, carrying both idempotency keys. Retained so a
 *   retry resubmits byte-identical content rather than rebuilding it.
 */
data class SendMoneyState(
    val uiState: SendMoneyUiState = SendMoneyUiState.Loading,
    val draft: PaymentDraft? = null,
    val consentId: String? = null,
)

sealed interface SendMoneyAction {
    data class SelectDebtorAccount(val accountId: String) : SendMoneyAction
    data class SelectCreditor(val beneficiaryId: String) : SendMoneyAction
    data object ShowManualCreditorEntry : SendMoneyAction
    data class EnterManualSortCode(val sortCode: String) : SendMoneyAction
    data class EnterManualAccountNumber(val accountNumber: String) : SendMoneyAction
    data class EnterManualName(val name: String) : SendMoneyAction
    data object ConfirmManualCreditor : SendMoneyAction
    data class EnterAmount(val minorUnits: String) : SendMoneyAction
    data class EnterReference(val reference: String) : SendMoneyAction
    data object ReviewPayment : SendMoneyAction
    data object ConfirmAndStageConsent : SendMoneyAction

    /** Re-stages after a staging failure. Nothing reached the bank, so there is nothing to replay. */
    data object RetryStaging : SendMoneyAction

    /** Returns to the payer step after the bank refused the account the payment came from. */
    data object ChangePayer : SendMoneyAction
    data object BackStep : SendMoneyAction
    data object RetryLoad : SendMoneyAction
}

/**
 * The one-shot effect the Screen owns rather than the ViewModel.
 *
 * Opening the bank's authorisation page is something only the composition can do, so it leaves as an
 * event instead of becoming state the screen has to interpret.
 */
sealed interface SendMoneyEvent {
    data class LaunchAuthorisation(val url: String) : SendMoneyEvent
}

/**
 * Resolves a failure to the kind that decides the recovery offered.
 *
 * The OBIE error code is consulted before the HTTP status because four of these arrive as `400` and
 * only the code separates them — "outside your limits" and "diverged from what you approved" want
 * different actions from the PSU.
 */
internal fun classifySendMoneyError(throwable: Throwable): SendMoneyErrorKind =
    throwable.payerRefusalKind()
        ?: throwable.obieErrorCode()?.let(::obieCodeToKind)
        ?: throwable.transportKind()

/**
 * The payer case, checked before the code.
 *
 * One code covers many fields — `U002` is only "Invalid Field" — so the code alone cannot tell a
 * refused payer from a refused reference, and only the payer has a different recovery. Reading the
 * path first also means a future code on the same path lands correctly without being enumerated.
 */
private fun Throwable.payerRefusalKind(): SendMoneyErrorKind? =
    SendMoneyErrorKind.PayerNotSupported.takeIf { isDebtorAccountRefusal() }

private fun Throwable.transportKind(): SendMoneyErrorKind =
    when ((this as? RemoteException)?.networkError) {
        is NetworkError.Client.Unauthorized -> SendMoneyErrorKind.TokenExpired
        is NetworkError.Client.Forbidden -> SendMoneyErrorKind.ConsentRevoked
        is NetworkError.Client.RateLimited -> SendMoneyErrorKind.RateLimited
        else -> SendMoneyErrorKind.NetworkError
    }

private fun obieCodeToKind(code: String): SendMoneyErrorKind? = when {
    code.endsWith("U019") -> SendMoneyErrorKind.SignatureMissing
    code.endsWith("U009") -> SendMoneyErrorKind.ConsentNotAuthorised
    code.endsWith("U008") -> SendMoneyErrorKind.ConsentMismatch
    code.endsWith("U014") -> SendMoneyErrorKind.OutsideControlParameters
    code.endsWith("U002") -> SendMoneyErrorKind.InvalidField
    // U021 names a field the bank would not accept, so it belongs with U002 rather than falling
    // through to NetworkError. Observed live when a credit card was sent as DebtorAccount: the app
    // told the customer to check their connection and offered a Retry that could only fail again.
    code.endsWith("U021") -> SendMoneyErrorKind.InvalidField
    else -> null
}

/** The bank's own reference for a failure, surfaced so a support call has something to quote. */
internal fun supportReferenceOf(throwable: Throwable): String? = throwable.obieSupportReference()
