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

sealed interface UserOnboardingUiState {
    data object Loading : UserOnboardingUiState

    data class Error(val message: String) : UserOnboardingUiState

    data object Empty : UserOnboardingUiState

    /** Step 1 of 2 — introduction to Open Banking. */
    data class Intro(val step: Int = 1) : UserOnboardingUiState

    /** Step 2 of 2 — reassurance items + legal footer. */
    data class ConsentExplainer(val step: Int = 2) : UserOnboardingUiState
}

sealed interface UserOnboardingEvent {
    /**
     * Emitted after the ViewModel writes [UserDataRepository.setFirstTimeState](
     * false). Navigation consumes this and transitions to the Auth (login) graph.
     */
    data object NavigateToLogin : UserOnboardingEvent, BackgroundEvent
}

sealed interface UserOnboardingAction {
    /** Check DataStore for the [org.mifosx.openbanking.core.model.user.UserData.firstTimeUser] flag. */
    data object LoadOnboarding : UserOnboardingAction

    /** Advance to the next step (capped at 2). */
    data object StepNext : UserOnboardingAction

    /** Return to the previous step (capped at 1). */
    data object StepBack : UserOnboardingAction

    /** Write onboarding_completed and navigate to the login screen. */
    data object NavigateToLogin : UserOnboardingAction

    /** Re-run the DataStore check after a transient failure. */
    data object RetryLoad : UserOnboardingAction
}

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
            UserOnboardingAction.NavigateToLogin -> handleNavigateToLogin()
            UserOnboardingAction.RetryLoad -> handleRetryLoad()
        }
    }

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

    private fun handleStepNext() {
        val currentStep = extractStep(state) ?: return
        if (currentStep >= 2) return
        updateState { stateForStep(currentStep + 1) }
    }

    private fun handleStepBack() {
        val currentStep = extractStep(state) ?: return
        if (currentStep <= 1) return
        updateState { stateForStep(currentStep - 1) }
    }

    private fun handleNavigateToLogin() {
        viewModelScope.launch {
            userDataRepository.setFirstTimeState(false)
        }
        sendEvent(UserOnboardingEvent.NavigateToLogin)
    }

    private fun handleRetryLoad() {
        trySendAction(UserOnboardingAction.LoadOnboarding)
    }

    private fun extractStep(state: UserOnboardingUiState): Int? = when (state) {
        is UserOnboardingUiState.Intro -> state.step
        is UserOnboardingUiState.ConsentExplainer -> state.step
        else -> null
    }

    private fun stateForStep(step: Int): UserOnboardingUiState = when (step) {
        1 -> UserOnboardingUiState.Intro(step = 1)
        2 -> UserOnboardingUiState.ConsentExplainer(step = 2)
        else -> UserOnboardingUiState.Loading
    }
}
