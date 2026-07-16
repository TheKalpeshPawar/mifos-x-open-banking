/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.onboarding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.feature.onboarding.ui.UserOnboardingAction
import org.mifosx.openbanking.feature.onboarding.ui.UserOnboardingEvent
import org.mifosx.openbanking.feature.onboarding.ui.UserOnboardingUiState
import org.mifosx.openbanking.feature.onboarding.ui.UserOnboardingViewModel
import template.core.base.ui.viewmodel.BackgroundEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class UserOnboardingViewModelTest {

    private fun createViewModel(
        firstTimeUser: Boolean = true,
        errorOnRead: Throwable? = null,
    ): Pair<UserOnboardingViewModel, FakeUserDataRepository> {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val repo = FakeUserDataRepository(firstTimeUser = firstTimeUser, errorOnRead = errorOnRead)
        val vm = UserOnboardingViewModel(repo)
        return vm to repo
    }

    private suspend fun navigateToStep(vm: UserOnboardingViewModel, step: Int) {
        vm.stateFlow.first { it is UserOnboardingUiState.Intro }
        if (step >= 2) {
            vm.trySendAction(UserOnboardingAction.StepNext)
            vm.stateFlow.first { it is UserOnboardingUiState.PermissionsOverview }
        }
        if (step >= 3) {
            vm.trySendAction(UserOnboardingAction.StepNext)
            vm.stateFlow.first { it is UserOnboardingUiState.ConsentExplainer }
        }
    }

    // ── Init & LoadOnboarding ───────────────────────────────────────────────

    @Test
    fun `init transitions to Intro when firstTimeUser is true`() = runTest {
        val (vm, _) = createViewModel(firstTimeUser = true)
        val state = vm.stateFlow.first { it is UserOnboardingUiState.Intro }
        assertIs<UserOnboardingUiState.Intro>(state)
        assertEquals(1, (state as UserOnboardingUiState.Intro).step)
    }

    @Test
    fun `init transitions to Empty when firstTimeUser is false`() = runTest {
        val (vm, _) = createViewModel(firstTimeUser = false)
        val state = vm.stateFlow.first { it is UserOnboardingUiState.Empty }
        assertIs<UserOnboardingUiState.Empty>(state)
    }

    @Test
    fun `init transitions to Error when repository throws`() = runTest {
        val (vm, _) = createViewModel(errorOnRead = RuntimeException("boom"))
        val state = vm.stateFlow.first { it is UserOnboardingUiState.Error }
        assertIs<UserOnboardingUiState.Error>(state)
        assertTrue((state as UserOnboardingUiState.Error).message.contains("boom"))
    }

    // ── Step navigation ─────────────────────────────────────────────────────

    @Test
    fun `StepNext from Intro transitions to PermissionsOverview`() = runTest {
        val (vm, _) = createViewModel(firstTimeUser = true)
        navigateToStep(vm, 1)
        vm.trySendAction(UserOnboardingAction.StepNext)
        val state = vm.stateFlow.first { it is UserOnboardingUiState.PermissionsOverview }
        assertIs<UserOnboardingUiState.PermissionsOverview>(state)
        assertEquals(2, (state as UserOnboardingUiState.PermissionsOverview).step)
    }

    @Test
    fun `StepNext from PermissionsOverview transitions to ConsentExplainer`() = runTest {
        val (vm, _) = createViewModel(firstTimeUser = true)
        navigateToStep(vm, 2)
        vm.trySendAction(UserOnboardingAction.StepNext)
        val state = vm.stateFlow.first { it is UserOnboardingUiState.ConsentExplainer }
        assertIs<UserOnboardingUiState.ConsentExplainer>(state)
        assertEquals(3, (state as UserOnboardingUiState.ConsentExplainer).step)
    }

    @Test
    fun `StepNext from ConsentExplainer stays at step 3`() = runTest {
        val (vm, _) = createViewModel(firstTimeUser = true)
        navigateToStep(vm, 3)
        vm.trySendAction(UserOnboardingAction.StepNext)
        val state = vm.stateFlow.value
        assertIs<UserOnboardingUiState.ConsentExplainer>(state)
    }

    @Test
    fun `StepBack from PermissionsOverview transitions to Intro`() = runTest {
        val (vm, _) = createViewModel(firstTimeUser = true)
        navigateToStep(vm, 2)
        vm.trySendAction(UserOnboardingAction.StepBack)
        val state = vm.stateFlow.first { it is UserOnboardingUiState.Intro }
        assertIs<UserOnboardingUiState.Intro>(state)
    }

    @Test
    fun `StepBack from Intro stays at step 1`() = runTest {
        val (vm, _) = createViewModel(firstTimeUser = true)
        navigateToStep(vm, 1)
        vm.trySendAction(UserOnboardingAction.StepBack)
        val state = vm.stateFlow.value
        assertIs<UserOnboardingUiState.Intro>(state)
    }

    @Test
    fun `StepBack from ConsentExplainer transitions to PermissionsOverview`() = runTest {
        val (vm, _) = createViewModel(firstTimeUser = true)
        navigateToStep(vm, 3)
        vm.trySendAction(UserOnboardingAction.StepBack)
        val state = vm.stateFlow.first { it is UserOnboardingUiState.PermissionsOverview }
        assertIs<UserOnboardingUiState.PermissionsOverview>(state)
    }

    // ── Bottom sheet ────────────────────────────────────────────────────────

    @Test
    fun `OpenObExplainer from ConsentExplainer transitions to ObExplainerOpen`() = runTest {
        val (vm, _) = createViewModel(firstTimeUser = true)
        navigateToStep(vm, 3)
        vm.trySendAction(UserOnboardingAction.OpenObExplainer)
        val state = vm.stateFlow.first { it is UserOnboardingUiState.ObExplainerOpen }
        assertIs<UserOnboardingUiState.ObExplainerOpen>(state)
    }

    @Test
    fun `CloseObExplainer from ObExplainerOpen transitions back to ConsentExplainer`() = runTest {
        val (vm, _) = createViewModel(firstTimeUser = true)
        navigateToStep(vm, 3)
        vm.trySendAction(UserOnboardingAction.OpenObExplainer)
        vm.stateFlow.first { it is UserOnboardingUiState.ObExplainerOpen }
        vm.trySendAction(UserOnboardingAction.CloseObExplainer)
        val state = vm.stateFlow.first { it is UserOnboardingUiState.ConsentExplainer }
        assertIs<UserOnboardingUiState.ConsentExplainer>(state)
    }

    @Test
    fun `OpenObExplainer from Intro is a no-op`() = runTest {
        val (vm, _) = createViewModel(firstTimeUser = true)
        navigateToStep(vm, 1)
        vm.trySendAction(UserOnboardingAction.OpenObExplainer)
        val state = vm.stateFlow.value
        assertIs<UserOnboardingUiState.Intro>(state)
    }

    // ── NavigateToLogin ─────────────────────────────────────────────────────

    @Test
    fun `NavigateToLogin sets firstTimeState to false`() = runTest {
        val (vm, repo) = createViewModel(firstTimeUser = true)
        navigateToStep(vm, 1)
        vm.trySendAction(UserOnboardingAction.NavigateToLogin)
        // Let the launched coroutine run
        vm.stateFlow.first { repo.firstTimeStateSetCount > 0 }
        assertEquals(1, repo.firstTimeStateSetCount)
        assertEquals(false, repo.lastFirstTimeStateValue)
    }

    @Test
    fun `NavigateToLogin emits NavigateToLogin event`() = runTest {
        val (vm, _) = createViewModel(firstTimeUser = true)
        navigateToStep(vm, 1)
        vm.trySendAction(UserOnboardingAction.NavigateToLogin)
        val event = vm.eventFlow.first { it is UserOnboardingEvent.NavigateToLogin }
        assertIs<UserOnboardingEvent.NavigateToLogin>(event)
        assertIs<BackgroundEvent>(event)
    }

    // ── RetryLoad ───────────────────────────────────────────────────────────

    @Test
    fun `RetryLoad from Error re-runs LoadOnboarding and transitions to Intro`() = runTest {
        val (vm, repo) = createViewModel(errorOnRead = RuntimeException("transient"))
        vm.stateFlow.first { it is UserOnboardingUiState.Error }
        repo.errorOnRead = null
        vm.trySendAction(UserOnboardingAction.RetryLoad)
        val state = vm.stateFlow.first { it is UserOnboardingUiState.Intro }
        assertIs<UserOnboardingUiState.Intro>(state)
    }

    @Test
    fun `RetryLoad from Error stays Error if repository still throws`() = runTest {
        val (vm, _) = createViewModel(errorOnRead = RuntimeException("persistent"))
        vm.stateFlow.first { it is UserOnboardingUiState.Error }
        vm.trySendAction(UserOnboardingAction.RetryLoad)
        val state = vm.stateFlow.first { it is UserOnboardingUiState.Error }
        assertIs<UserOnboardingUiState.Error>(state)
    }

    // ── Edge cases ──────────────────────────────────────────────────────────

    @Test
    fun `LoadOnboarding captures exception message in Error state`() = runTest {
        val (vm, _) = createViewModel(errorOnRead = RuntimeException("DataStore I/O failed"))
        val state = vm.stateFlow.first { it is UserOnboardingUiState.Error }
        assertIs<UserOnboardingUiState.Error>(state)
        assertTrue((state as UserOnboardingUiState.Error).message.contains("DataStore I/O failed"))
    }
}
