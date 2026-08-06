/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.paymentstatus

import org.mifosx.openbanking.core.data.banking.PaymentInitiationRepository
import org.mifosx.openbanking.core.model.banking.payment.PaymentCharge
import org.mifosx.openbanking.core.model.banking.payment.PaymentDisposition
import org.mifosx.openbanking.core.model.banking.payment.PaymentDraft
import org.mifosx.openbanking.core.model.banking.payment.PaymentReceipt
import org.mifosx.openbanking.core.model.banking.payment.PaymentStatus
import org.mifosx.openbanking.core.model.banking.payment.StagedConsent
import org.mifosx.openbanking.feature.paymentstatus.ui.PaymentStatusErrorKind
import org.mifosx.openbanking.feature.paymentstatus.ui.PaymentStatusState
import org.mifosx.openbanking.feature.paymentstatus.ui.PaymentStatusUiState
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

/** The same payment the rest of the PISP spec is told through. */
object PaymentStatusFixtures {

    const val PAYMENT_ID = "PMT-812774903-01"

    /**
     * Shaped like HSBC's real answer: all three timestamps equal, and a non-zero `CHAPSOut` charge
     * on a Faster Payments instruction. Both of those look like mistakes and are not — see
     * [receiptWithDistinctTimestamps] for the case where the dates genuinely differ.
     */
    fun receipt(
        status: PaymentStatus = PaymentStatus.AcceptedSettlementInProcess,
        charges: List<PaymentCharge> = listOf(
            PaymentCharge(
                bearer = "BorneByDebtor",
                typeLabel = "UK.OBIE.CHAPSOut",
                amountLabel = "£0.05",
            ),
        ),
    ): PaymentReceipt =
        PaymentReceipt(
            domesticPaymentId = PAYMENT_ID,
            consentId = "812774903",
            status = status,
            creationDateTime = "2026-08-03T14:22:00+00:00",
            statusUpdateDateTime = "2026-08-03T14:22:00+00:00",
            settlementDateTime = "2026-08-03T14:22:00+00:00",
            amountLabel = "£850.00",
            creditorName = "Jameson Lettings",
            reference = "RENT-FLAT12",
            debtorIdentification = "40051512345678",
            charges = charges,
        )

    /** An ASPSP that maintains the fields properly, so the three rows are proven independent. */
    fun receiptWithDistinctTimestamps(): PaymentReceipt = receipt().copy(
        creationDateTime = "2026-08-03T14:22:00+00:00",
        statusUpdateDateTime = "2026-08-03T16:40:00+00:00",
        settlementDateTime = "2026-08-04T09:00:00+00:00",
    )

    fun contentState(
        disposition: PaymentDisposition = PaymentDisposition.InProgress,
        reference: String = "RENT-FLAT12",
        refreshing: Boolean = false,
        status: PaymentStatus = PaymentStatus.AcceptedSettlementInProcess,
        settledAt: String = "3 Aug 2026, 14:22",
        charges: List<PaymentCharge> = listOf(
            PaymentCharge(
                bearer = "BorneByDebtor",
                typeLabel = "UK.OBIE.CHAPSOut",
                amountLabel = "£0.05",
            ),
        ),
        lastCheckedAt: String = "14:25",
    ): PaymentStatusState = PaymentStatusState(
        paymentId = PAYMENT_ID,
        uiState = PaymentStatusUiState.Content(
            paymentId = PAYMENT_ID,
            status = status,
            disposition = disposition,
            amountLabel = "£850.00",
            creditorName = "Jameson Lettings",
            reference = reference,
            debtorLabel = "40-05-15 12345678",
            submittedAt = "3 Aug 2026, 14:22",
            settledAt = settledAt,
            statusChangedAt = "3 Aug 2026, 14:22",
            charges = charges,
            lastCheckedAt = lastCheckedAt,
            refreshing = refreshing,
        ),
    )

    fun loadingState(): PaymentStatusState =
        PaymentStatusState(paymentId = PAYMENT_ID, uiState = PaymentStatusUiState.Loading)

    fun errorState(
        kind: PaymentStatusErrorKind = PaymentStatusErrorKind.NetworkError,
    ): PaymentStatusState =
        PaymentStatusState(paymentId = PAYMENT_ID, uiState = PaymentStatusUiState.Error(kind))
}

/**
 * Only `paymentStatus` is exercised here — this screen never writes, which is the property that
 * lets it read on a client-credentials token after the PSU token has expired.
 */
class FakePaymentInitiationRepository(
    receipt: PaymentReceipt = PaymentStatusFixtures.receipt(),
    private var statusResult: NetworkResult<PaymentReceipt, NetworkError> =
        NetworkResult.Success(receipt),
) : PaymentInitiationRepository {

    val statusReads = mutableListOf<String>()

    override suspend fun stagePayment(draft: PaymentDraft): NetworkResult<StagedConsent, NetworkError> =
        error("send-money stages payments; payment-status never does")

    override suspend fun confirmFunds(consentId: String): NetworkResult<Boolean, NetworkError> =
        error("send-money confirms funds; payment-status never does")

    override suspend fun submitPayment(
        draft: PaymentDraft,
        consentId: String,
    ): NetworkResult<PaymentReceipt, NetworkError> =
        error("the callback leg submits payments; payment-status never does")

    override fun stagedDraft(): PaymentDraft? = null

    override suspend fun paymentStatus(
        domesticPaymentId: String,
    ): NetworkResult<PaymentReceipt, NetworkError> {
        statusReads += domesticPaymentId
        return statusResult
    }

    fun statusReturns(result: NetworkResult<PaymentReceipt, NetworkError>) {
        statusResult = result
    }

    /** Convenience for the common case of a later read returning a moved-on receipt. */
    fun receiptReturns(receipt: PaymentReceipt) {
        statusResult = NetworkResult.Success(receipt)
    }
}
