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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mifosx.openbanking.core.data.payments.PaymentsRepository

/** Confirm-payment state. */
sealed interface ConfirmUiState {
    data object Review : ConfirmUiState
    data object Submitting : ConfirmUiState
    data class Failed(val message: String) : ConfirmUiState
}

/**
 * Confirm-payment ViewModel. Executes the SEPA transaction-request for the reviewed
 * draft and reports success (→ navigate home) or a typed error message.
 */
class SendMoneyConfirmViewModel(
    private val paymentsRepository: PaymentsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ConfirmUiState>(ConfirmUiState.Review)
    val state: StateFlow<ConfirmUiState> = _state.asStateFlow()

    fun submit(
        bankId: String,
        accountId: String,
        counterpartyId: String,
        amount: String,
        currency: String,
        reference: String,
        onSuccess: () -> Unit,
    ) {
        if (_state.value == ConfirmUiState.Submitting) return
        _state.value = ConfirmUiState.Submitting
        viewModelScope.launch {
            paymentsRepository.sendToCounterparty(bankId, accountId, counterpartyId, amount, currency, reference).fold(
                onSuccess = { onSuccess() },
                onFailure = { _state.value = ConfirmUiState.Failed(errorMessage(it)) },
            )
        }
    }

    fun reset() {
        _state.value = ConfirmUiState.Review
    }

    private fun errorMessage(error: Throwable): String {
        val reason = error.message.orEmpty()
        return when {
            reason.contains("INSUFFICIENT", true) -> "Insufficient funds in your account."
            reason.contains("LIMIT", true) -> "This payment exceeds your daily transfer limit."
            reason.contains("40003", true) -> "Payment currency must match the source account currency."
            else -> "Payment could not be processed. Please try again."
        }
    }
}
