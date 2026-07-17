/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.consentcallback.ui

import androidx.lifecycle.viewModelScope
import com.russhwolf.settings.Settings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.mifosx.openbanking.core.data.callback.CallbackParams
import org.mifosx.openbanking.core.data.callback.ConsentCallbackRepository
import org.mifosx.openbanking.core.data.callback.ValidationResult
import org.mifosx.openbanking.core.model.callback.ConsentStatus
import org.mifosx.openbanking.core.model.oauth.PsuTokenResponse
import template.core.base.common.screen.ScreenState
import template.core.base.ui.viewmodel.BackgroundEvent
import template.core.base.ui.viewmodel.BaseViewModel

sealed interface ConsentCallbackUiState {
    data object Loading : ConsentCallbackUiState
    data object Content : ConsentCallbackUiState
    data object Awaiting : ConsentCallbackUiState
    data class Error(val message: String) : ConsentCallbackUiState
    data object AccessDenied : ConsentCallbackUiState
    data object SecurityError : ConsentCallbackUiState
}

sealed interface ConsentCallbackEvent {
    data object NavigateToHome : ConsentCallbackEvent, BackgroundEvent
    data object NavigateToLogin : ConsentCallbackEvent, BackgroundEvent
}

sealed interface ConsentCallbackAction {
    data class ProcessCallback(
        val code: String?,
        val idToken: String?,
        val state: String?,
        val error: String?,
        val errorDescription: String?,
        val expectedState: String,
        val expectedNonce: String,
        val consentId: String,
    ) : ConsentCallbackAction
    data object PollConsentStatus : ConsentCallbackAction
    data object NavigateRetry : ConsentCallbackAction
    data object NavigateLogin : ConsentCallbackAction
}

class ConsentCallbackViewModel(
    private val repository: ConsentCallbackRepository,
    private val secureSettings: Settings,
    private val redirectUri: String,
) : BaseViewModel<ConsentCallbackUiState, ConsentCallbackEvent, ConsentCallbackAction>(
    initialState = ConsentCallbackUiState.Loading,
) {
    private var pendingConsentId: String? = null

    override fun handleAction(action: ConsentCallbackAction) {
        when (action) {
            is ConsentCallbackAction.ProcessCallback -> process(action)
            ConsentCallbackAction.PollConsentStatus -> repoll()
            ConsentCallbackAction.NavigateRetry -> {
                sendEvent(ConsentCallbackEvent.NavigateToLogin)
            }
            ConsentCallbackAction.NavigateLogin -> {
                secureSettings.clear()
                sendEvent(ConsentCallbackEvent.NavigateToLogin)
            }
        }
    }

    private fun process(action: ConsentCallbackAction.ProcessCallback) {
        updateState { ConsentCallbackUiState.Loading }

        val params = CallbackParams(
            code = action.code,
            idToken = action.idToken,
            state = action.state,
            error = action.error,
            errorDescription = action.errorDescription,
            expectedState = action.expectedState,
            expectedNonce = action.expectedNonce,
        )

        when (val v = repository.validateCallback(params)) {
            ValidationResult.SecurityError ->
                updateState { ConsentCallbackUiState.SecurityError }

            ValidationResult.AccessDenied ->
                updateState { ConsentCallbackUiState.AccessDenied }

            is ValidationResult.Error ->
                updateState { ConsentCallbackUiState.Error(v.message) }

            ValidationResult.MissingCode ->
                updateState { ConsentCallbackUiState.Error("No authorisation code received.") }

            ValidationResult.Valid -> {
                pendingConsentId = action.consentId
                viewModelScope.launch {
                    executeExchange(action.code!!)
                }
            }
        }
    }

    private suspend fun executeExchange(code: String) {
        when (val result = repository.exchangeCode(code, redirectUri)) {
            is ScreenState.Content -> {
                persistTokens(result.data)
                repoll()
            }
            is ScreenState.Error ->
                updateState { ConsentCallbackUiState.Error(result.error.message ?: "Token exchange failed.") }

            is ScreenState.Unauthenticated ->
                updateState { ConsentCallbackUiState.Error("Authorisation failed.") }

            is ScreenState.NoNetwork ->
                updateState { ConsentCallbackUiState.Error("Unable to reach HSBC.") }

            else -> {}
        }
    }

    private fun repoll() {
        val consentId = pendingConsentId ?: return
        viewModelScope.launch {
            when (val result = repository.pollConsentStatus(consentId)) {
                is ScreenState.Content ->
                    when (result.data) {
                        ConsentStatus.Authorised -> {
                            updateState { ConsentCallbackUiState.Content }
                            delay(1500)
                            sendEvent(ConsentCallbackEvent.NavigateToHome)
                        }
                        ConsentStatus.AwaitingAuthorisation ->
                            updateState { ConsentCallbackUiState.Awaiting }

                        else ->
                            updateState { ConsentCallbackUiState.Error("Consent was ${result.data}.") }
                    }

                is ScreenState.Error ->
                    updateState { ConsentCallbackUiState.Error(result.error.message ?: "Consent check failed.") }

                is ScreenState.Unauthenticated ->
                    updateState { ConsentCallbackUiState.Error("Authorisation failed.") }

                is ScreenState.NoNetwork ->
                    updateState { ConsentCallbackUiState.Error("Unable to reach HSBC.") }

                else -> {}
            }
        }
    }

    private fun persistTokens(token: PsuTokenResponse) {
        val json = Json { ignoreUnknownKeys = true }
        secureSettings.putString(
            "hsbc_tokens",
            json.encodeToString(PsuTokenResponse.serializer(), token),
        )
    }
}
