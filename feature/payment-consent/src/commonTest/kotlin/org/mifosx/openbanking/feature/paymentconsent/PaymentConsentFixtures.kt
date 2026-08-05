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

import org.mifosx.openbanking.core.data.callback.PaymentAuthRepository
import org.mifosx.openbanking.core.data.callback.PaymentAuthValidation
import org.mifosx.openbanking.feature.paymentconsent.ui.PaymentConsentErrorKind
import org.mifosx.openbanking.feature.paymentconsent.ui.PaymentConsentState
import org.mifosx.openbanking.feature.paymentconsent.ui.PaymentConsentUiState
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

object PaymentConsentFixtures {

    const val CONSENT_ID = "812774903"
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

    fun authorisedState(): PaymentConsentState =
        PaymentConsentState(uiState = PaymentConsentUiState.Authorised, consentId = CONSENT_ID)

    fun errorState(
        kind: PaymentConsentErrorKind = PaymentConsentErrorKind.StateMismatch,
    ): PaymentConsentState =
        PaymentConsentState(uiState = PaymentConsentUiState.Error(kind), consentId = CONSENT_ID)
}

class FakePaymentAuthRepository(
    private var validation: PaymentAuthValidation =
        PaymentAuthValidation.Valid(PaymentConsentFixtures.CODE, PaymentConsentFixtures.CONSENT_ID),
    private var exchange: NetworkResult<Unit, NetworkError> = NetworkResult.Success(Unit),
    private var status: NetworkResult<String, NetworkError> = NetworkResult.Success("AUTH"),
) : PaymentAuthRepository {

    val exchangedCodes = mutableListOf<String>()
    val statusChecks = mutableListOf<String>()

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
