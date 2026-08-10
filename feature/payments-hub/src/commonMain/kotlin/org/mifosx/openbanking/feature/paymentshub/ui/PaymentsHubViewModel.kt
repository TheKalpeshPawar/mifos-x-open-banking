/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.paymentshub.ui

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.mifosx.openbanking.core.data.banking.PaymentHistoryRepository
import org.mifosx.openbanking.core.model.banking.payment.PaymentHistoryItem
import template.core.base.ui.viewmodel.BaseViewModel

class PaymentsHubViewModel(
    private val paymentHistoryRepository: PaymentHistoryRepository,
) : BaseViewModel<PaymentsHubState, Nothing, PaymentsHubAction>(
    initialState = PaymentsHubState(),
) {

    private val activityItems = MutableStateFlow<List<PaymentHistoryItem>>(emptyList())

    init {
        paymentHistoryRepository.observeRecent()
            .onEach { items ->
                activityItems.value = items
                updateState {
                    copy(uiState = PaymentsHubUiState.Content(activityItems = items))
                }
            }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: PaymentsHubAction) {
        when (action) {
            is PaymentsHubAction.RefreshActivity -> refreshActivity()
            is PaymentsHubAction.RetryLoad -> refreshActivity()
        }
    }

    private fun refreshActivity() {
        val current = state.uiState
        if (current is PaymentsHubUiState.Content) {
            updateState { copy(uiState = current.copy(isRefreshing = true)) }
        }
        viewModelScope.launch {
            try {
                paymentHistoryRepository.refreshStatuses()
            } finally {
                if (state.uiState is PaymentsHubUiState.Content) {
                    val c = state.uiState as PaymentsHubUiState.Content
                    updateState { copy(uiState = c.copy(isRefreshing = false)) }
                }
            }
        }
    }
}
