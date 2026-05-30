/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.login.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.mifosx.openbanking.core.data.auth.AuthRecoveryRepository
import org.mifosx.openbanking.core.model.obp.ObpException
import template.core.base.ui.viewmodel.BaseViewModel

/**
 * Forgot-password ViewModel. Submits a username/email to [AuthRecoveryRepository.initiateReset]
 * (OBP password-reset). On success the UI swaps to an anti-enumeration confirmation that never
 * reveals whether the account exists; on failure a mapped error banner is shown.
 */
class ForgotPasswordViewModel(
    private val authRecoveryRepository: AuthRecoveryRepository,
) : BaseViewModel<ForgotPasswordState, ForgotPasswordEvent, ForgotPasswordAction>(
    initialState = ForgotPasswordState(),
) {

    override fun handleAction(action: ForgotPasswordAction) {
        when (action) {
            is ForgotPasswordAction.IdentifierChanged ->
                updateState { copy(identifier = action.value, errorMessage = null) }

            ForgotPasswordAction.SubmitClicked -> onSubmit()

            ForgotPasswordAction.BackToLoginClicked ->
                sendEvent(ForgotPasswordEvent.NavigateBackToLogin)

            is ForgotPasswordAction.Internal.ResetResultReceive -> onResetResult(action.result)
        }
    }

    private fun onSubmit() {
        val current = state
        if (!current.isFormValid || current.isSubmitting || current.isSuccess) return
        updateState { copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            val result = authRecoveryRepository.initiateReset(current.identifier.trim())
            sendAction(ForgotPasswordAction.Internal.ResetResultReceive(result))
        }
    }

    private fun onResetResult(result: Result<String>) {
        result.fold(
            onSuccess = {
                updateState { copy(isSubmitting = false, isSuccess = true, errorMessage = null) }
            },
            onFailure = { error ->
                updateState {
                    copy(isSubmitting = false, errorMessage = error.toResetErrorMessage())
                }
            },
        )
    }
}

private fun Throwable.toResetErrorMessage(): String =
    when ((this as? ObpException)?.reason) {
        "NOT_FOUND", "BAD_REQUEST" ->
            "We couldn't find an account with that username or email. Please try again."

        "TOO_MANY_REQUESTS" ->
            "Too many requests. Please wait a moment and try again."

        else ->
            "Could not connect to banking services. Please try again."
    }

/** Immutable UI state for the Forgot Password screen (mirrors the feature SPEC State Model). */
@Immutable
data class ForgotPasswordState(
    val identifier: String = "",
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
) {
    val isFormValid: Boolean get() = identifier.isNotBlank()
}

/** One-shot navigation events for the Forgot Password screen. */
sealed interface ForgotPasswordEvent {
    data object NavigateBackToLogin : ForgotPasswordEvent
}

/** Actions accepted by [ForgotPasswordViewModel]. */
sealed interface ForgotPasswordAction {
    data class IdentifierChanged(val value: String) : ForgotPasswordAction
    data object SubmitClicked : ForgotPasswordAction
    data object BackToLoginClicked : ForgotPasswordAction

    sealed interface Internal : ForgotPasswordAction {
        data class ResetResultReceive(val result: Result<String>) : Internal
    }
}
