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

    fun receipt(status: PaymentStatus = PaymentStatus.AcceptedSettlementInProcess): PaymentReceipt =
        PaymentReceipt(
            domesticPaymentId = PAYMENT_ID,
            consentId = "812774903",
            status = status,
            statusUpdateDateTime = "2026-08-03T14:22:00+00:00",
            amountLabel = "£850.00",
            creditorName = "Jameson Lettings",
            reference = "RENT-FLAT12",
            debtorIdentification = "40051512345678",
        )

    fun contentState(
        disposition: PaymentDisposition = PaymentDisposition.InProgress,
        reference: String = "RENT-FLAT12",
        refreshing: Boolean = false,
    ): PaymentStatusState = PaymentStatusState(
        paymentId = PAYMENT_ID,
        uiState = PaymentStatusUiState.Content(
            paymentId = PAYMENT_ID,
            statusLabel = "AcceptedSettlementInProcess",
            disposition = disposition,
            amountLabel = "£850.00",
            creditorName = "Jameson Lettings",
            reference = reference,
            debtorLabel = "40-05-15 12345678",
            submittedAt = "3 Aug 2026, 14:22",
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
    private var statusResult: NetworkResult<PaymentReceipt, NetworkError> =
        NetworkResult.Success(PaymentStatusFixtures.receipt()),
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
        error("send-money submits payments; payment-status never does")

    override suspend fun paymentStatus(
        domesticPaymentId: String,
    ): NetworkResult<PaymentReceipt, NetworkError> {
        statusReads += domesticPaymentId
        return statusResult
    }

    fun statusReturns(result: NetworkResult<PaymentReceipt, NetworkError>) {
        statusResult = result
    }
}
