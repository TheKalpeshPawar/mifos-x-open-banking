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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.mifosx.openbanking.core.data.callback.ConsentCallbackRepository
import org.mifosx.openbanking.core.data.callback.ConsentSession
import org.mifosx.openbanking.core.data.callback.ValidationResult
import org.mifosx.openbanking.core.network.model.oauth.PsuTokenResponse
import template.core.base.common.screen.ScreenState
import template.core.base.ui.viewmodel.BackgroundEvent
import template.core.base.ui.viewmodel.BaseViewModel

/** How long the success screen lingers before the navigator swaps to Home. */
private const val NAVIGATION_DELAY_MS = 1500L

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
    /**
     * The raw redirect URL as the OS handed it to us. Parsing and authentication belong to
     * [ConsentCallbackRepository], which alone holds the expected `state`/`nonce`.
     */
    data class ProcessCallback(val redirectUrl: String) : ConsentCallbackAction
    data object PollConsentStatus : ConsentCallbackAction
    data object NavigateRetry : ConsentCallbackAction
    data object NavigateLogin : ConsentCallbackAction
}

class ConsentCallbackViewModel(
    private val repository: ConsentCallbackRepository,
    private val consentSession: ConsentSession,
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
                consentSession.clear()
                sendEvent(ConsentCallbackEvent.NavigateToLogin)
            }
        }
    }

    private fun process(action: ConsentCallbackAction.ProcessCallback) {
        updateState { ConsentCallbackUiState.Loading }

        when (val v = repository.validateCallback(action.redirectUrl)) {
            ValidationResult.SecurityError ->
                updateState { ConsentCallbackUiState.SecurityError }

            ValidationResult.AccessDenied ->
                updateState { ConsentCallbackUiState.AccessDenied }

            is ValidationResult.Error ->
                updateState { ConsentCallbackUiState.Error(v.message) }

            ValidationResult.MissingCode ->
                updateState { ConsentCallbackUiState.Error("No authorisation code received.") }

            is ValidationResult.Valid -> {
                pendingConsentId = v.consentId
                viewModelScope.launch {
                    executeExchange(v.code)
                }
            }
        }
    }

    private suspend fun executeExchange(code: String) {
        when (val result = repository.exchangeCode(code, redirectUri)) {
            is ScreenState.Content -> completeSignIn(result.data)

            is ScreenState.Error ->
                updateState { ConsentCallbackUiState.Error(result.error.message ?: "Token exchange failed.") }

            is ScreenState.Unauthenticated ->
                updateState { ConsentCallbackUiState.Error("Authorisation failed.") }

            is ScreenState.NoNetwork ->
                updateState { ConsentCallbackUiState.Error("Unable to reach HSBC.") }

            else -> {}
        }
    }

    /**
     * Completes the sign-in after a successful token exchange.
     *
     * A PSU access token is only ever issued against an *authorised* consent, so a successful
     * exchange already proves authorisation — the consent status the bank reports a beat later can
     * still read `AwaitingAuthorisation` while it settles, but that is a propagation lag, not a
     * pending decision. So the consent id is recorded unconditionally (this is the only writer of
     * `ConsentSession.consentId()`, which the consent screens read to show the current connection),
     * and the poll is used only to read the expiry, best-effort. Gating this on the poll returning
     * `Authorised` is what first left a freshly authorised consent invisible until a second attempt.
     *
     * Order is load-bearing: the consent is recorded BEFORE the tokens are persisted. Persisting the
     * tokens flips [ConsentSession] active, which the root navigator reacts to by routing to the
     * authenticated graph and tearing this screen (and this coroutine) down — so recording the
     * consent afterwards could be cancelled mid-write, which is what left it invisible even after the
     * poll-gating was removed.
     */
    private suspend fun completeSignIn(tokens: PsuTokenResponse) {
        recordConsent()
        persistTokens(tokens)
        updateState { ConsentCallbackUiState.Content }
        delay(NAVIGATION_DELAY_MS)
        sendEvent(ConsentCallbackEvent.NavigateToHome)
    }

    /** Records the current consent id (best-effort expiry from a single status read). */
    private suspend fun recordConsent() {
        val consentId = pendingConsentId ?: return
        val expiry = (repository.pollConsentStatus(consentId) as? ScreenState.Content)
            ?.data?.expirationDateTime.orEmpty()
        consentSession.saveConsentMeta(consentId, expiry)
    }

    /**
     * A manual re-check from the (now vestigial) awaiting screen: re-records the consent and moves on.
     * A no-op before any callback has produced a pending consent id.
     */
    private fun repoll() {
        if (pendingConsentId == null) return
        viewModelScope.launch {
            recordConsent()
            updateState { ConsentCallbackUiState.Content }
            delay(NAVIGATION_DELAY_MS)
            sendEvent(ConsentCallbackEvent.NavigateToHome)
        }
    }

    /**
     * Persisting the tokens is what makes the PSU "signed in" — the root navigator derives that from
     * [ConsentSession], not from a stored flag, so this write is the whole of the transition.
     */
    private fun persistTokens(token: PsuTokenResponse) {
        consentSession.save(token)
    }
}
