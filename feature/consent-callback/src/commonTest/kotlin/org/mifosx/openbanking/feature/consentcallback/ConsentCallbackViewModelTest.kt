/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.consentcallback

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.core.data.callback.SettingsConsentSession
import org.mifosx.openbanking.core.data.callback.ValidationResult
import org.mifosx.openbanking.core.model.callback.ConsentStatus
import org.mifosx.openbanking.feature.consentcallback.ui.ConsentCallbackAction
import org.mifosx.openbanking.feature.consentcallback.ui.ConsentCallbackEvent
import org.mifosx.openbanking.feature.consentcallback.ui.ConsentCallbackUiState
import org.mifosx.openbanking.feature.consentcallback.ui.ConsentCallbackViewModel
import template.core.base.common.screen.DataFreshness
import template.core.base.common.screen.ScreenState
import template.core.base.ui.viewmodel.BackgroundEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ConsentCallbackViewModelTest {

    private val repository = FakeConsentCallbackRepository()
    private val secureSettings = MapSettings()

    /** The real session, so the tests pin the storage contract the navigator reads. */
    private val consentSession = SettingsConsentSession(secureSettings)

    private val userDataRepository = FakeUserDataRepository()

    private fun createViewModel(): ConsentCallbackViewModel {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        return ConsentCallbackViewModel(
            repository = repository,
            consentSession = consentSession,
            userDataRepository = userDataRepository,
            redirectUri = REDIRECT_URI,
        )
    }

    private fun ConsentCallbackViewModel.processCallback(url: String = CALLBACK_URL) =
        trySendAction(ConsentCallbackAction.ProcessCallback(url))

    @Test
    fun `initial state is Loading`() = runTest {
        val vm = createViewModel()

        assertIs<ConsentCallbackUiState.Loading>(vm.stateFlow.value)
    }

    @Test
    fun `ProcessCallback forwards the raw redirect url to the repository`() = runTest {
        val vm = createViewModel()

        vm.processCallback("org.mifosx.openbanking://callback?code=xyz&state=st-1")

        assertEquals(
            listOf("org.mifosx.openbanking://callback?code=xyz&state=st-1"),
            repository.validatedUrls,
        )
    }

    @Test
    fun `a valid callback exchanges the code and reaches Content`() = runTest {
        repository.validateResult = ValidationResult.Valid(code = "auth-code", consentId = "cn-1")
        val vm = createViewModel()

        vm.processCallback()

        assertIs<ConsentCallbackUiState.Content>(vm.stateFlow.first { it is ConsentCallbackUiState.Content })
        assertEquals(listOf("auth-code"), repository.exchangedCodes)
        assertEquals(listOf("cn-1"), repository.polledConsentIds)
    }

    @Test
    fun `a valid callback persists the psu tokens`() = runTest {
        val vm = createViewModel()

        vm.processCallback()
        vm.stateFlow.first { it is ConsentCallbackUiState.Content }

        val stored = secureSettings.getStringOrNull("hsbc_tokens")
        assertTrue(stored != null && stored.contains("psu-access-token"))
    }

    @Test
    fun `an authorised consent navigates home after the success dwell`() = runTest {
        val vm = createViewModel()

        vm.processCallback()
        vm.stateFlow.first { it is ConsentCallbackUiState.Content }
        advanceTimeBy(2_000)

        val event = vm.eventFlow.first()
        assertIs<ConsentCallbackEvent.NavigateToHome>(event)
        assertIs<BackgroundEvent>(event)
    }

    @Test
    fun `an authorised consent marks onboarding complete`() = runTest {
        val vm = createViewModel()

        vm.processCallback()
        vm.stateFlow.first { it is ConsentCallbackUiState.Content }

        assertEquals(1, userDataRepository.firstTimeStateSetCount)
        assertEquals(false, userDataRepository.lastFirstTimeStateValue)
    }

    @Test
    fun `SecurityError validation surfaces the SecurityError state`() = runTest {
        repository.validateResult = ValidationResult.SecurityError
        val vm = createViewModel()

        vm.processCallback()

        assertIs<ConsentCallbackUiState.SecurityError>(
            vm.stateFlow.first { it is ConsentCallbackUiState.SecurityError },
        )
        assertTrue(repository.exchangedCodes.isEmpty(), "a failed check must never exchange the code")
    }

    @Test
    fun `AccessDenied validation surfaces the AccessDenied state`() = runTest {
        repository.validateResult = ValidationResult.AccessDenied
        val vm = createViewModel()

        vm.processCallback()

        assertIs<ConsentCallbackUiState.AccessDenied>(
            vm.stateFlow.first { it is ConsentCallbackUiState.AccessDenied },
        )
        assertTrue(repository.exchangedCodes.isEmpty())
    }

    @Test
    fun `MissingCode validation surfaces an explanatory Error`() = runTest {
        repository.validateResult = ValidationResult.MissingCode
        val vm = createViewModel()

        vm.processCallback()

        val state = vm.stateFlow.first { it is ConsentCallbackUiState.Error }
        assertIs<ConsentCallbackUiState.Error>(state)
        assertTrue(state.message.contains("No authorisation code"))
    }

    @Test
    fun `an Error validation surfaces the repository message`() = runTest {
        repository.validateResult = ValidationResult.Error("HSBC is down")
        val vm = createViewModel()

        vm.processCallback()

        val state = vm.stateFlow.first { it is ConsentCallbackUiState.Error }
        assertIs<ConsentCallbackUiState.Error>(state)
        assertEquals("HSBC is down", state.message)
    }

    @Test
    fun `a failed token exchange surfaces an Error and does not poll`() = runTest {
        repository.exchangeResult = ScreenState.Error(FakeConsentCallbackRepository.NETWORK_ERROR)
        val vm = createViewModel()

        vm.processCallback()

        assertIs<ConsentCallbackUiState.Error>(vm.stateFlow.first { it is ConsentCallbackUiState.Error })
        assertTrue(repository.polledConsentIds.isEmpty())
    }

    @Test
    fun `an unauthenticated token exchange surfaces an Error`() = runTest {
        repository.exchangeResult = ScreenState.Unauthenticated
        val vm = createViewModel()

        vm.processCallback()

        val state = vm.stateFlow.first { it is ConsentCallbackUiState.Error }
        assertIs<ConsentCallbackUiState.Error>(state)
        assertTrue(state.message.contains("Authorisation failed"))
    }

    @Test
    fun `an offline token exchange surfaces a reachability Error`() = runTest {
        repository.exchangeResult = ScreenState.NoNetwork(isCaptivePortal = false)
        val vm = createViewModel()

        vm.processCallback()

        val state = vm.stateFlow.first { it is ConsentCallbackUiState.Error }
        assertIs<ConsentCallbackUiState.Error>(state)
        assertTrue(state.message.contains("Unable to reach HSBC"))
    }

    @Test
    fun `an awaiting consent surfaces the Awaiting state`() = runTest {
        repository.pollResults = mutableListOf(
            ScreenState.Content(ConsentStatus.AwaitingAuthorisation, DataFreshness.FRESH),
        )
        val vm = createViewModel()

        vm.processCallback()

        assertIs<ConsentCallbackUiState.Awaiting>(vm.stateFlow.first { it is ConsentCallbackUiState.Awaiting })
    }

    @Test
    fun `a rejected consent surfaces an Error naming the status`() = runTest {
        repository.pollResults = mutableListOf(
            ScreenState.Content(ConsentStatus.Rejected, DataFreshness.FRESH),
        )
        val vm = createViewModel()

        vm.processCallback()

        val state = vm.stateFlow.first { it is ConsentCallbackUiState.Error }
        assertIs<ConsentCallbackUiState.Error>(state)
        assertTrue(state.message.contains("Rejected"))
    }

    @Test
    fun `a failed poll surfaces an Error`() = runTest {
        repository.pollResults = mutableListOf(ScreenState.Error(FakeConsentCallbackRepository.NETWORK_ERROR))
        val vm = createViewModel()

        vm.processCallback()

        assertIs<ConsentCallbackUiState.Error>(vm.stateFlow.first { it is ConsentCallbackUiState.Error })
    }

    @Test
    fun `an unauthenticated poll surfaces an Error`() = runTest {
        repository.pollResults = mutableListOf(ScreenState.Unauthenticated)
        val vm = createViewModel()

        vm.processCallback()

        val state = vm.stateFlow.first { it is ConsentCallbackUiState.Error }
        assertIs<ConsentCallbackUiState.Error>(state)
        assertTrue(state.message.contains("Authorisation failed"))
    }

    @Test
    fun `an offline poll surfaces a reachability Error`() = runTest {
        repository.pollResults = mutableListOf(ScreenState.NoNetwork(isCaptivePortal = false))
        val vm = createViewModel()

        vm.processCallback()

        val state = vm.stateFlow.first { it is ConsentCallbackUiState.Error }
        assertIs<ConsentCallbackUiState.Error>(state)
        assertTrue(state.message.contains("Unable to reach HSBC"))
    }

    @Test
    fun `PollConsentStatus re-polls the pending consent and can reach Content`() = runTest {
        repository.pollResults = mutableListOf(
            ScreenState.Content(ConsentStatus.AwaitingAuthorisation, DataFreshness.FRESH),
            ScreenState.Content(ConsentStatus.Authorised, DataFreshness.FRESH),
        )
        val vm = createViewModel()
        vm.processCallback()
        vm.stateFlow.first { it is ConsentCallbackUiState.Awaiting }

        vm.trySendAction(ConsentCallbackAction.PollConsentStatus)

        assertIs<ConsentCallbackUiState.Content>(vm.stateFlow.first { it is ConsentCallbackUiState.Content })
        assertEquals(listOf("cn-1", "cn-1"), repository.polledConsentIds)
    }

    @Test
    fun `PollConsentStatus before any callback is a no-op`() = runTest {
        val vm = createViewModel()

        vm.trySendAction(ConsentCallbackAction.PollConsentStatus)

        assertTrue(repository.polledConsentIds.isEmpty())
        assertIs<ConsentCallbackUiState.Loading>(vm.stateFlow.value)
    }

    @Test
    fun `NavigateRetry emits NavigateToLogin and keeps the session`() = runTest {
        secureSettings.putString("hsbc_tokens", "existing")
        val vm = createViewModel()

        vm.trySendAction(ConsentCallbackAction.NavigateRetry)

        assertIs<ConsentCallbackEvent.NavigateToLogin>(vm.eventFlow.first())
        assertEquals("existing", secureSettings.getStringOrNull("hsbc_tokens"))
    }

    @Test
    fun `NavigateLogin clears the secure session before emitting NavigateToLogin`() = runTest {
        secureSettings.putString("hsbc_tokens", "existing")
        val vm = createViewModel()

        vm.trySendAction(ConsentCallbackAction.NavigateLogin)

        assertIs<ConsentCallbackEvent.NavigateToLogin>(vm.eventFlow.first())
        assertTrue(
            secureSettings.keys.isEmpty(),
            "a security failure must not leave credentials behind",
        )
    }

    private companion object {
        const val REDIRECT_URI = "https://thekalpeshpawar.github.io/obp-callback/callback/"
        const val CALLBACK_URL = "org.mifosx.openbanking://callback?code=auth-code&state=st-1"
    }
}
