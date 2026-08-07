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
import org.mifosx.openbanking.core.data.banking.PaymentHistoryRepository
import org.mifosx.openbanking.core.data.banking.PaymentInitiationRepository
import org.mifosx.openbanking.core.data.callback.PaymentAuthRepository
import org.mifosx.openbanking.core.data.callback.PaymentAuthValidation
import org.mifosx.openbanking.core.data.util.toThrowable
import org.mifosx.openbanking.core.model.banking.payment.PaymentDraft
import template.core.base.network.NetworkResult
import template.core.base.ui.viewmodel.BaseViewModel

/** OBIE reports an authorised consent as `AUTH`; some responses spell it out. */
private val AUTHORISED_STATUSES = setOf("AUTH", "AUTHORISED", "AUTHORIZED")

/**
 * The return leg of a payment authorisation, and the screen that completes the payment.
 *
 * It validates the redirect, exchanges the code for a payments-scoped token, confirms the consent
 * reached `Authorised`, checks funds and submits — the whole tail of the journey, in one place.
 *
 * Submitting here rather than handing back to send-money is not a stylistic choice. Returning to the
 * authenticated graph pops it without saving state, so the send-money ViewModel that built the draft
 * is rebuilt empty and its `submitPayment` finds nothing to send. This screen, by contrast, is alive
 * at the moment the tokens arrive — it just exchanged them — and reads the staged draft back from
 * storage, which is what keeps the submitted `Initiation` byte-identical to the staged one.
 *
 * Every terminal outcome clears the authorisation, so a finished or abandoned payment leaves no
 * consent id, PSU token or draft behind for the next one to find.
 */
class PaymentConsentViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: PaymentAuthRepository,
    private val paymentInitiationRepository: PaymentInitiationRepository,
    private val paymentHistoryRepository: PaymentHistoryRepository,
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
            PaymentConsentAction.RetryAuthorisation -> restartAuthorisation()
            PaymentConsentAction.AbandonPayment -> abandon()
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
     * an error the customer cannot act on. Reaching `AUTH` is what releases the submission.
     */
    private fun checkConsentStatus() {
        updateState { copy(uiState = PaymentConsentUiState.Checking()) }
        viewModelScope.launch {
            when (val result = repository.consentStatus(state.consentId)) {
                is NetworkResult.Success ->
                    if (result.data.trim().uppercase() in AUTHORISED_STATUSES) {
                        completePayment()
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

    /**
     * Confirms funds, then submits the staged instruction.
     *
     * A missing draft is terminal rather than a prompt to rebuild one: the consent was granted
     * against a specific `Initiation`, and anything reconstructed here would be a different
     * instruction wearing the same consent.
     */
    private suspend fun completePayment() {
        val draft = paymentInitiationRepository.stagedDraft()
            ?: return fail(PaymentConsentErrorKind.NoStagedPayment)

        updateState { copy(uiState = PaymentConsentUiState.ConfirmingFunds) }

        when (val funds = paymentInitiationRepository.confirmFunds(state.consentId)) {
            is NetworkResult.Success ->
                if (funds.data) {
                    submit(draft)
                } else {
                    fail(PaymentConsentErrorKind.InsufficientFunds)
                }

            is NetworkResult.Error -> fail(classifyPaymentConsentError(funds.error.toThrowable()))
        }
    }

    private suspend fun submit(draft: PaymentDraft) {
        updateState { copy(uiState = PaymentConsentUiState.Submitting) }

        when (val result = paymentInitiationRepository.submitPayment(draft, state.consentId)) {
            is NetworkResult.Success -> {
                repository.discardAuthorisation()
                sendEvent(PaymentConsentEvent.PaymentSubmitted(result.data.domesticPaymentId))
            }

            is NetworkResult.Error -> fail(PaymentConsentErrorKind.SubmissionFailed)
        }
    }

    /**
     * Starting again means starting from the form: the consent this screen was handed is spent, and
     * a fresh payment needs a fresh consent staged under fresh keys.
     */
    private fun restartAuthorisation() {
        repository.discardAuthorisation()
        sendEvent(PaymentConsentEvent.RestartAuthorisation)
    }

    private fun abandon() {
        repository.discardAuthorisation()
        sendEvent(PaymentConsentEvent.Abandoned)
    }

    /**
     * Every failure is terminal for this authorisation, so the session goes with it. Leaving the
     * PSU token behind would let a later payment submit on a credential its own authorisation never
     * issued.
     */
    private fun fail(kind: PaymentConsentErrorKind) {
        // Read the draft before discarding the session — discard clears the stored draft.
        val draft = paymentInitiationRepository.stagedDraft()
        repository.discardAuthorisation()

        if (draft != null) {
            viewModelScope.launch {
                paymentHistoryRepository.saveFailed(
                    draft = draft,
                    errorKind = kind.name,
                    errorDescription = kind.description(),
                )
            }
        }

        updateState { copy(uiState = PaymentConsentUiState.Error(kind)) }
    }

    companion object {
        /** Must match the `PaymentConsentRoute` property name — type-safe nav uses it as the key. */
        const val REDIRECT_URL_ARG: String = "redirectUrl"
    }
}
