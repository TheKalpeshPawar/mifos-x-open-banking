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

/** Where a submission has got to. Distinct from [SendMoneyStep]: nothing is editable here. */
enum class SendMoneyStage {
    StagingConsent,
    AwaitingAuthorisation,
    SubmittingPayment,
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

/** One selectable row — an account to pay from, or a payee to pay. */
data class SendMoneyPickerRow(
    val id: String,
    val initials: String,
    val headline: String,
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
        val debtorRows: List<SendMoneyPickerRow>,
        val debtorAccountId: String? = null,
        val creditor: CreditorSelection? = null,
        val creditorLabel: String = "",
        val creditorSupporting: String = "",
        val debtorAccountLabel: String = "",
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

    data class Success(
        val paymentId: String,
        val statusLabel: String,
        val amountLabel: String,
        val creditorName: String,
    ) : SendMoneyUiState

    data class Error(
        val kind: SendMoneyErrorKind,
        val supportReference: String? = null,
    ) : SendMoneyUiState
}

/**
 * @property idempotencyKey Generated once, when the form is first completed, and reused across every
 *   retry. It lives here rather than being minted at call time because a retry that mints a fresh
 *   key is not a retry — it is a second payment instruction.
 * @property draft The instruction as it was staged. Retained so a retry resubmits byte-identical
 *   content rather than rebuilding it.
 */
data class SendMoneyState(
    val uiState: SendMoneyUiState = SendMoneyUiState.Loading,
    val idempotencyKey: String = "",
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
    data object SubmitPayment : SendMoneyAction
    data object RetrySubmit : SendMoneyAction
    data object CancelPayment : SendMoneyAction
    data object BackStep : SendMoneyAction
    data object RetryLoad : SendMoneyAction
}

/**
 * One-shot effects the Screen owns rather than the ViewModel.
 *
 * Launching a browser and navigating are both things only the composition can do, so they leave as
 * events instead of becoming state the screen has to interpret.
 */
sealed interface SendMoneyEvent {
    data class LaunchAuthorisation(val url: String) : SendMoneyEvent
    data class PaymentSucceeded(val paymentId: String) : SendMoneyEvent
}

/**
 * Resolves a failure to the kind that decides the recovery offered.
 *
 * The OBIE error code is consulted before the HTTP status because four of these arrive as `400` and
 * only the code separates them — "outside your limits" and "diverged from what you approved" want
 * different actions from the PSU.
 */
internal fun classifySendMoneyError(throwable: Throwable): SendMoneyErrorKind {
    throwable.obieErrorCode()?.let { code ->
        obieCodeToKind(code)?.let { return it }
    }
    return when ((throwable as? RemoteException)?.networkError) {
        is NetworkError.Client.Unauthorized -> SendMoneyErrorKind.TokenExpired
        is NetworkError.Client.Forbidden -> SendMoneyErrorKind.ConsentRevoked
        is NetworkError.Client.RateLimited -> SendMoneyErrorKind.RateLimited
        else -> SendMoneyErrorKind.NetworkError
    }
}

private fun obieCodeToKind(code: String): SendMoneyErrorKind? = when {
    code.endsWith("U019") -> SendMoneyErrorKind.SignatureMissing
    code.endsWith("U009") -> SendMoneyErrorKind.ConsentNotAuthorised
    code.endsWith("U008") -> SendMoneyErrorKind.ConsentMismatch
    code.endsWith("U014") -> SendMoneyErrorKind.OutsideControlParameters
    code.endsWith("U002") -> SendMoneyErrorKind.InvalidField
    else -> null
}

/** The bank's own reference for a failure, surfaced so a support call has something to quote. */
internal fun supportReferenceOf(throwable: Throwable): String? = throwable.obieSupportReference()
