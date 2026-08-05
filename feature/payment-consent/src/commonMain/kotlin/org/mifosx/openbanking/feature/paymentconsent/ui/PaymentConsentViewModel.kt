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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.mifosx.openbanking.core.data.callback.PaymentAuthRepository
import org.mifosx.openbanking.core.data.callback.PaymentAuthValidation
import org.mifosx.openbanking.core.data.util.toThrowable
import template.core.base.network.NetworkResult
import template.core.base.ui.viewmodel.BaseViewModel

/** OBIE reports an authorised consent as `AUTH`; some responses spell it out. */
private val AUTHORISED_STATUSES = setOf("AUTH", "AUTHORISED", "AUTHORIZED")

/**
 * The return leg of a payment authorisation — the PISP counterpart to `consent-callback`.
 *
 * Validates the redirect, exchanges the code for a payments-scoped token, then confirms the consent
 * actually reached `Authorised` before handing control back. That last step is not a formality:
 * submitting a payment against a consent still awaiting authorisation is refused with `400 U009`, so
 * the hand-back is gated on it.
 *
 * Nothing is navigated from here. The outcome leaves as an event because send-money holds the
 * idempotency key and the staged `Initiation`, and only it can finish the payment.
 */
class PaymentConsentViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: PaymentAuthRepository,
) : BaseViewModel<PaymentConsentState, PaymentConsentEvent, PaymentConsentAction>(
    initialState = PaymentConsentState(),
) {

    private val redirectUrl: String = savedStateHandle.get<String>(REDIRECT_URL_ARG).orEmpty()

    private var authorizationCode: String = ""

    init {
        validate()
    }

    override fun handleAction(action: PaymentConsentAction) {
        when (action) {
            PaymentConsentAction.CheckAgain -> checkConsentStatus()
            PaymentConsentAction.RetryAuthorisation -> sendEvent(PaymentConsentEvent.RestartAuthorisation)
            PaymentConsentAction.AbandonPayment -> sendEvent(PaymentConsentEvent.Abandoned)
        }
    }

    /**
     * `state`/`nonce` are checked before anything else and a mismatch is terminal — there is no
     * retry offered for it, because a callback that does not match the authorisation this app
     * launched is not a transient fault.
     */
    private fun validate() {
        when (val result = repository.validateCallback(redirectUrl)) {
            is PaymentAuthValidation.Valid -> {
                authorizationCode = result.code
                updateState { copy(consentId = result.consentId) }
                exchange()
            }

            PaymentAuthValidation.SecurityError -> fail(PaymentConsentErrorKind.StateMismatch)
            PaymentAuthValidation.AccessDenied -> fail(PaymentConsentErrorKind.ConsentRejected)
            PaymentAuthValidation.MissingCode -> fail(PaymentConsentErrorKind.CodeExpired)
            is PaymentAuthValidation.Error -> fail(PaymentConsentErrorKind.NetworkError)
        }
    }

    private fun exchange() {
        updateState { copy(uiState = PaymentConsentUiState.Exchanging) }
        viewModelScope.launch {
            when (val result = repository.exchangeCode(authorizationCode)) {
                is NetworkResult.Success -> checkConsentStatus()
                is NetworkResult.Error ->
                    fail(classifyPaymentConsentError(result.error.toThrowable()))
            }
        }
    }

    /**
     * A consent that has not reached `AUTH` yet is not a failure — banks take a moment. So this
     * stays in [PaymentConsentUiState.Checking] and offers Check again rather than timing out into
     * an error the customer cannot act on.
     */
    private fun checkConsentStatus() {
        updateState { copy(uiState = PaymentConsentUiState.Checking()) }
        viewModelScope.launch {
            when (val result = repository.consentStatus(state.consentId)) {
                is NetworkResult.Success ->
                    if (result.data.trim().uppercase() in AUTHORISED_STATUSES) {
                        updateState { copy(uiState = PaymentConsentUiState.Authorised) }
                        sendEvent(PaymentConsentEvent.Authorised(state.consentId))
                    } else {
                        updateState {
                            copy(uiState = PaymentConsentUiState.Checking(canCheckAgain = true))
                        }
                    }

                is NetworkResult.Error ->
                    fail(classifyPaymentConsentError(result.error.toThrowable()))
            }
        }
    }

    private fun fail(kind: PaymentConsentErrorKind) {
        updateState { copy(uiState = PaymentConsentUiState.Error(kind)) }
    }

    companion object {
        /** Must match the `PaymentConsentRoute` property name — type-safe nav uses it as the key. */
        const val REDIRECT_URL_ARG: String = "redirectUrl"
    }
}
