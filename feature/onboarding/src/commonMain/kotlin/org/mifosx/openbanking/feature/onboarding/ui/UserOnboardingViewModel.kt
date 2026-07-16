/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.onboarding.ui

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.mifosx.openbanking.core.data.user.UserDataRepository
import template.core.base.ui.viewmodel.BackgroundEvent
import template.core.base.ui.viewmodel.BaseViewModel

// ── UI State ────────────────────────────────────────────────────────────────

sealed interface UserOnboardingUiState {
    data object Loading : UserOnboardingUiState

    data class Error(val message: String) : UserOnboardingUiState

    data object Empty : UserOnboardingUiState

    /** Step 1 of 3 — introduction to Open Banking. */
    data class Intro(val step: Int = 1) : UserOnboardingUiState

    /** Step 2 of 3 — list of 6 OBIE AISP permissions. */
    data class PermissionsOverview(val step: Int = 2) : UserOnboardingUiState

    /** Step 3 of 3 — reassurance items + legal footer. */
    data class ConsentExplainer(
        val step: Int = 3,
        val showObExplainerSheet: Boolean = false,
    ) : UserOnboardingUiState

    /** Step 3 with the "How Open Banking works" bottom sheet overlaid. */
    data class ObExplainerOpen(
        val step: Int = 3,
    ) : UserOnboardingUiState
}

// ── One-shot Event ──────────────────────────────────────────────────────────

sealed interface UserOnboardingEvent {
    /**
     * Emitted after the ViewModel writes [UserDataRepository.setFirstTimeState](
     * false). Navigation consumes this and transitions to the Auth (login) graph.
     */
    data object NavigateToLogin : UserOnboardingEvent, BackgroundEvent
}

// ── User Actions ────────────────────────────────────────────────────────────

sealed interface UserOnboardingAction {
    /** Check DataStore for the [org.mifosx.openbanking.core.model.user.UserData.firstTimeUser] flag. */
    data object LoadOnboarding : UserOnboardingAction

    /** Advance to the next step (capped at 3). */
    data object StepNext : UserOnboardingAction

    /** Return to the previous step (capped at 1). */
    data object StepBack : UserOnboardingAction

    /** Open the "How Open Banking works" bottom sheet (only valid on step 3). */
    data object OpenObExplainer : UserOnboardingAction

    /** Dismiss the bottom sheet (return to step 3). */
    data object CloseObExplainer : UserOnboardingAction

    /** Write onboarding_completed and navigate to the login screen. */
    data object NavigateToLogin : UserOnboardingAction

    /** Re-run the DataStore check after a transient failure. */
    data object RetryLoad : UserOnboardingAction
}

// ── ViewModel ───────────────────────────────────────────────────────────────

/**
 * Pure state-machine ViewModel with zero suspend functions in [handleAction].
 * No network calls — the only external dependency is [UserDataRepository] for
 * the [firstTimeUser][org.mifosx.openbanking.core.model.user.UserData.firstTimeUser]
 * flag.
 */
class UserOnboardingViewModel(
    private val userDataRepository: UserDataRepository,
) : BaseViewModel<UserOnboardingUiState, UserOnboardingEvent, UserOnboardingAction>(
    initialState = UserOnboardingUiState.Loading,
) {
    init {
        trySendAction(UserOnboardingAction.LoadOnboarding)
    }

    override fun handleAction(action: UserOnboardingAction) {
        when (action) {
            UserOnboardingAction.LoadOnboarding -> handleLoadOnboarding()
            UserOnboardingAction.StepNext -> handleStepNext()
            UserOnboardingAction.StepBack -> handleStepBack()
            UserOnboardingAction.OpenObExplainer -> handleOpenObExplainer()
            UserOnboardingAction.CloseObExplainer -> handleCloseObExplainer()
            UserOnboardingAction.NavigateToLogin -> handleNavigateToLogin()
            UserOnboardingAction.RetryLoad -> handleRetryLoad()
        }
    }

    // ── Load ────────────────────────────────────────────────────────────────

    private fun handleLoadOnboarding() {
        updateState { UserOnboardingUiState.Loading }
        viewModelScope.launch {
            try {
                val userData = userDataRepository.userData.first()
                if (userData.firstTimeUser) {
                    updateState { UserOnboardingUiState.Intro(step = 1) }
                } else {
                    updateState { UserOnboardingUiState.Empty }
                }
            } catch (e: Exception) {
                updateState { UserOnboardingUiState.Error(message = e.message ?: "Unknown error") }
            }
        }
    }

    // ── Step navigation ─────────────────────────────────────────────────────

    private fun handleStepNext() {
        val currentStep = extractStep(state) ?: return
        if (currentStep >= 3) return
        val next = currentStep + 1
        updateState { stateForStep(next) }
    }

    private fun handleStepBack() {
        val currentStep = extractStep(state) ?: return
        if (currentStep <= 1) return
        val prev = currentStep - 1
        updateState { stateForStep(prev) }
    }

    // ── Bottom sheet ────────────────────────────────────────────────────────

    private fun handleOpenObExplainer() {
        val current = state
        if (current is UserOnboardingUiState.ConsentExplainer) {
            updateState { UserOnboardingUiState.ObExplainerOpen(step = current.step) }
        }
    }

    private fun handleCloseObExplainer() {
        val current = state
        if (current is UserOnboardingUiState.ObExplainerOpen) {
            updateState { UserOnboardingUiState.ConsentExplainer(step = current.step) }
        }
    }

    // ── Navigate to login ───────────────────────────────────────────────────

    private fun handleNavigateToLogin() {
        viewModelScope.launch {
            userDataRepository.setFirstTimeState(false)
        }
        sendEvent(UserOnboardingEvent.NavigateToLogin)
    }

    // ── Retry ───────────────────────────────────────────────────────────────

    private fun handleRetryLoad() {
        trySendAction(UserOnboardingAction.LoadOnboarding)
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /** Extract the step number from any step-carrying state, or null. */
    private fun extractStep(state: UserOnboardingUiState): Int? = when (state) {
        is UserOnboardingUiState.Intro -> state.step
        is UserOnboardingUiState.PermissionsOverview -> state.step
        is UserOnboardingUiState.ConsentExplainer -> state.step
        is UserOnboardingUiState.ObExplainerOpen -> state.step
        else -> null
    }

    /** Map a step number (1–3) to its canonical UI state. */
    private fun stateForStep(step: Int): UserOnboardingUiState = when (step) {
        1 -> UserOnboardingUiState.Intro(step = 1)
        2 -> UserOnboardingUiState.PermissionsOverview(step = 2)
        3 -> UserOnboardingUiState.ConsentExplainer(step = 3)
        else -> UserOnboardingUiState.Loading
    }
}
