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
import org.mifosx.openbanking.core.model.obp.TransactionRequest

/** Confirm-payment state. */
sealed interface ConfirmUiState {
    data object Review : ConfirmUiState
    data object Submitting : ConfirmUiState
    data class Failed(val message: String) : ConfirmUiState
}

/**
 * Confirm-payment ViewModel. Routes the reviewed draft to the rail the form derived:
 * SEPA payments go through the SEPA transaction-request (by IBAN); Domestic and
 * International use the COUNTERPARTY transaction-request (by counterparty id).
 */
class SendMoneyConfirmViewModel(
    private val paymentsRepository: PaymentsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ConfirmUiState>(ConfirmUiState.Review)
    val state: StateFlow<ConfirmUiState> = _state.asStateFlow()

    fun submit(draft: PaymentDraft, onSuccess: () -> Unit) {
        if (_state.value == ConfirmUiState.Submitting) return
        _state.value = ConfirmUiState.Submitting
        viewModelScope.launch {
            send(draft).fold(
                onSuccess = { onSuccess() },
                onFailure = { _state.value = ConfirmUiState.Failed(errorMessage(it)) },
            )
        }
    }

    private suspend fun send(draft: PaymentDraft): Result<TransactionRequest> {
        if (draft.paymentType != PaymentType.SEPA || draft.iban.isBlank()) return sendCounterparty(draft)
        val sepa = paymentsRepository.sendSepaPayment(
            bankId = draft.fromBankId,
            accountId = draft.fromAccountId,
            iban = draft.iban,
            amount = draft.amount,
            currency = draft.currency,
            reference = draft.reference,
        )
        // The sandbox can only settle SEPA-by-IBAN when the payee's counterparty carries the
        // IBAN in its secondary routing AND resolves to a hosted account (OBP-30012/OBP-30074
        // otherwise). Those payees are still payable by counterparty id, so fall back.
        val message = sepa.exceptionOrNull()?.message.orEmpty()
        return if ("OBP-30012" in message || "OBP-30074" in message) sendCounterparty(draft) else sepa
    }

    private suspend fun sendCounterparty(draft: PaymentDraft): Result<TransactionRequest> =
        paymentsRepository.sendToCounterparty(
            bankId = draft.fromBankId,
            accountId = draft.fromAccountId,
            counterpartyId = draft.counterpartyId,
            amount = draft.amount,
            currency = draft.currency,
            reference = draft.reference,
        )

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
