/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.paymentconsent

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.mifosx.openbanking.core.data.banking.PaymentHistoryRepository
import org.mifosx.openbanking.core.data.banking.PaymentInitiationRepository
import org.mifosx.openbanking.core.data.callback.PaymentAuthRepository
import org.mifosx.openbanking.core.data.callback.PaymentAuthValidation
import org.mifosx.openbanking.core.model.banking.BankAccount
import org.mifosx.openbanking.core.model.banking.BeneficiaryScheme
import org.mifosx.openbanking.core.model.banking.payment.ConsentType
import org.mifosx.openbanking.core.model.banking.payment.CreditorSelection
import org.mifosx.openbanking.core.model.banking.payment.PaymentDraft
import org.mifosx.openbanking.core.model.banking.payment.PaymentHistoryItem
import org.mifosx.openbanking.core.model.banking.payment.PaymentReceipt
import org.mifosx.openbanking.core.model.banking.payment.PaymentStageTimestamps
import org.mifosx.openbanking.core.model.banking.payment.PaymentStatus
import org.mifosx.openbanking.core.model.banking.payment.StagedConsent
import org.mifosx.openbanking.feature.paymentconsent.ui.PaymentConsentErrorKind
import org.mifosx.openbanking.feature.paymentconsent.ui.PaymentConsentState
import org.mifosx.openbanking.feature.paymentconsent.ui.PaymentConsentUiState
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

object PaymentConsentFixtures {

    const val CONSENT_ID = "812774903"
    const val PAYMENT_ID = "58923-002"
    const val CODE = "auth-code-1"
    const val REDIRECT_URL = "https://callback.example/?code=$CODE&state=state-1"

    fun validatingState(): PaymentConsentState =
        PaymentConsentState(uiState = PaymentConsentUiState.Validating, consentId = CONSENT_ID)

    fun exchangingState(): PaymentConsentState =
        PaymentConsentState(uiState = PaymentConsentUiState.Exchanging, consentId = CONSENT_ID)

    fun checkingState(canCheckAgain: Boolean = false): PaymentConsentState = PaymentConsentState(
        uiState = PaymentConsentUiState.Checking(canCheckAgain = canCheckAgain),
        consentId = CONSENT_ID,
    )

    fun approvedState(): PaymentConsentState =
        PaymentConsentState(uiState = PaymentConsentUiState.Approved, consentId = CONSENT_ID)

    fun alreadySubmittedState(): PaymentConsentState =
        PaymentConsentState(uiState = PaymentConsentUiState.AlreadySubmitted, consentId = CONSENT_ID)

    fun confirmingFundsState(): PaymentConsentState =
        PaymentConsentState(uiState = PaymentConsentUiState.ConfirmingFunds, consentId = CONSENT_ID)

    fun submittingState(): PaymentConsentState =
        PaymentConsentState(uiState = PaymentConsentUiState.Submitting, consentId = CONSENT_ID)

    fun errorState(
        kind: PaymentConsentErrorKind = PaymentConsentErrorKind.StateMismatch,
    ): PaymentConsentState =
        PaymentConsentState(uiState = PaymentConsentUiState.Error(kind), consentId = CONSENT_ID)

    /**
     * A payer with no nickname, as HSBC actually returns them. Keeping the blank there stops a
     * fixture from quietly asserting a field the live bank does not populate.
     */
    fun draft(): PaymentDraft = PaymentDraft(
        debtorAccount = BankAccount(
            accountId = "acc-1",
            nickname = "",
            accountSubType = "CurrentAccount",
            currency = "GBP",
            sortCode = "802001",
            accountNumber = "10203349",
            rawIdentification = "80200110203349",
        ),
        creditor = CreditorSelection(
            name = "Liam Walker",
            scheme = BeneficiaryScheme.SortCode,
            identification = "40120965872310",
        ),
        amountMinorUnits = 50_000L,
        currency = "GBP",
        reference = "Invoice 2026-05",
        instructionIdentification = "MFX20260805T1042330001",
        endToEndIdentification = "E2E-RENT-FLAT12-202608",
        consentIdempotencyKey = "consent-key-1",
        paymentIdempotencyKey = "payment-key-1",
    )

    fun receipt(paymentId: String = PAYMENT_ID): PaymentReceipt = PaymentReceipt(
        domesticPaymentId = paymentId,
        consentId = CONSENT_ID,
        status = PaymentStatus.AcceptedSettlementInProcess,
        creationDateTime = "2026-08-05T10:42:33Z",
        statusUpdateDateTime = "2026-08-05T10:42:33Z",
        amountLabel = "£500.00",
        creditorName = "Liam Walker",
    )
}

class FakePaymentAuthRepository(
    private var validation: PaymentAuthValidation =
        PaymentAuthValidation.Valid(PaymentConsentFixtures.CODE, PaymentConsentFixtures.CONSENT_ID),
    private var exchange: NetworkResult<Unit, NetworkError> = NetworkResult.Success(Unit),
    private var status: NetworkResult<String, NetworkError> = NetworkResult.Success("AUTH"),
) : PaymentAuthRepository {

    val exchangedCodes = mutableListOf<String>()
    val statusChecks = mutableListOf<String>()

    /** How many times the authorisation was discarded — the session-clearing assertion hangs on it. */
    var discardCount: Int = 0
        private set

    override fun isPaymentRedirect(redirectUrl: String): Boolean = true

    override fun validateCallback(redirectUrl: String): PaymentAuthValidation = validation

    override suspend fun exchangeCode(code: String): NetworkResult<Unit, NetworkError> {
        exchangedCodes += code
        return exchange
    }

    override suspend fun consentStatus(consentId: String): NetworkResult<String, NetworkError> {
        statusChecks += consentId
        return status
    }

    var approvedRecordedCount: Int = 0
        private set

    override fun recordApproved() {
        approvedRecordedCount++
    }

    override fun discardAuthorisation() {
        discardCount++
    }

    fun validationReturns(result: PaymentAuthValidation) {
        validation = result
    }

    fun exchangeReturns(result: NetworkResult<Unit, NetworkError>) {
        exchange = result
    }

    fun statusReturns(result: NetworkResult<String, NetworkError>) {
        status = result
    }
}

/**
 * The payment write path, recorded rather than performed.
 *
 * [stagedDraft] defaults to a real draft because the callback's whole job now depends on finding
 * one; the null case is the interesting exception, not the baseline.
 */
class FakePaymentInitiationRepository(
    private var staged: PaymentDraft? = PaymentConsentFixtures.draft(),
    private var funds: NetworkResult<Boolean, NetworkError> = NetworkResult.Success(true),
    private var submission: NetworkResult<PaymentReceipt, NetworkError> =
        NetworkResult.Success(PaymentConsentFixtures.receipt()),
) : PaymentInitiationRepository {

    val submittedDrafts = mutableListOf<PaymentDraft>()
    val fundsChecks = mutableListOf<String>()

    /**
     * Runs at the moment the staged draft is read.
     *
     * That is the one observable instant between the consent being reported authorised and the funds
     * check starting, which is what makes [PaymentConsentUiState.Approved] — a state the flow passes
     * straight through — assertable at all.
     */
    var onStagedDraft: (() -> Unit)? = null

    override suspend fun stagePayment(draft: PaymentDraft): NetworkResult<StagedConsent, NetworkError> =
        throw UnsupportedOperationException("The callback leg never stages a payment")

    override suspend fun confirmFunds(consentId: String): NetworkResult<Boolean, NetworkError> {
        fundsChecks += consentId
        return funds
    }

    override suspend fun submitPayment(
        draft: PaymentDraft,
        consentId: String,
    ): NetworkResult<PaymentReceipt, NetworkError> {
        submittedDrafts += draft
        return submission
    }

    override fun stagedDraft(): PaymentDraft? {
        onStagedDraft?.invoke()
        return staged
    }

    override suspend fun paymentStatus(
        domesticPaymentId: String,
    ): NetworkResult<PaymentReceipt, NetworkError> = submission

    fun stagedDraftReturns(draft: PaymentDraft?) {
        staged = draft
    }

    fun fundsReturn(result: NetworkResult<Boolean, NetworkError>) {
        funds = result
    }

    fun submissionReturns(result: NetworkResult<PaymentReceipt, NetworkError>) {
        submission = result
    }
}

/**
 * The payment history write path, recorded rather than persisted.
 *
 * The callback leg writes a row on both outcomes — a submitted payment and a pre-submission failure
 * — so both are captured separately rather than counted, which is what lets a test assert that a
 * failure was recorded once and not also saved as a submission.
 */
class FakePaymentHistoryRepository : PaymentHistoryRepository {

    val submitted = mutableListOf<Pair<PaymentReceipt, PaymentDraft>>()
    val failures = mutableListOf<Triple<PaymentDraft, String, String>>()
    var refreshCount: Int = 0
        private set

    override fun observeRecent(): Flow<List<PaymentHistoryItem>> = flowOf(emptyList())

    override suspend fun saveSubmitted(receipt: PaymentReceipt, draft: PaymentDraft) {
        submitted += receipt to draft
    }

    override suspend fun saveFailed(draft: PaymentDraft, errorKind: String, errorDescription: String) {
        failures += Triple(draft, errorKind, errorDescription)
    }

    override suspend fun refreshStatuses() {
        refreshCount++
    }

    /** The callback leg never reads a rail back; it always has the draft in hand. */
    override suspend fun consentTypeOf(paymentId: String): ConsentType? = null

    /** This fake keeps no rows, so it has no stage times to report. */
    override suspend fun stageTimestampsOf(paymentId: String): PaymentStageTimestamps? = null
}
