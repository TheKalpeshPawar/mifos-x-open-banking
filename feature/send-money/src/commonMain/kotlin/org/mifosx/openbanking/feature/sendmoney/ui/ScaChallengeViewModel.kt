/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.sendmoney.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mifosx.openbanking.core.data.payments.PaymentsRepository

/** Everything needed to answer one payment's SCA challenge, carried across the navigation hop. */
@Immutable
data class ScaChallengeArgs(
    val bankId: String,
    val accountId: String,
    val type: String,
    val requestId: String,
    val challengeId: String,
)

/** Confirm-challenge UI state — the entered code plus submit/error flags. */
@Immutable
data class ScaChallengeUiState(
    val answer: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
) {
    val isAnswerValid: Boolean get() = answer.isNotBlank()
}

/**
 * Answers the Strong Customer Authentication challenge on an INITIATED payment. The user enters the
 * one-time code (any positive integer in the sandbox); a COMPLETED response books the payment and
 * triggers `onCompleted`, otherwise a mapped error is surfaced.
 */
class ScaChallengeViewModel(
    private val paymentsRepository: PaymentsRepository,
    private val bankId: String,
    private val accountId: String,
    private val type: String,
    private val requestId: String,
    private val challengeId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(ScaChallengeUiState())
    val state: StateFlow<ScaChallengeUiState> = _state.asStateFlow()

    fun onAnswerChanged(value: String) {
        _state.update { it.copy(answer = value, errorMessage = null) }
    }

    fun onSubmit(onCompleted: () -> Unit) {
        val current = _state.value
        if (!current.isAnswerValid || current.isSubmitting) return
        _state.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            paymentsRepository.answerChallenge(bankId, accountId, type, requestId, challengeId, current.answer.trim())
                .fold(
                    onSuccess = { request ->
                        if (request.status.equals("COMPLETED", ignoreCase = true)) {
                            onCompleted()
                        } else {
                            _state.update {
                                it.copy(
                                    isSubmitting = false,
                                    errorMessage = "Couldn't confirm the payment. Please try again.",
                                )
                            }
                        }
                    },
                    onFailure = { error ->
                        _state.update { it.copy(isSubmitting = false, errorMessage = challengeErrorMessage(error)) }
                    },
                )
        }
    }
}

private fun challengeErrorMessage(error: Throwable): String {
    val reason = error.message.orEmpty()
    return when {
        "40014" in reason -> "Too many incorrect attempts. Please start the payment again."
        "40011" in reason -> "This payment can no longer be confirmed. Please start it again."
        "40009" in reason -> "Something changed with this payment. Please start it again."
        "30279" in reason -> "This payment needs approval from a different authoriser on the account."
        else -> "That code wasn't accepted. Please check it and try again."
    }
}
