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

import org.mifosx.openbanking.core.model.banking.payment.PaymentHistoryItem

sealed interface PaymentsHubUiState {
    data object Loading : PaymentsHubUiState

    data class Content(
        val activityItems: List<PaymentHistoryItem> = emptyList(),
        val isRefreshing: Boolean = false,
    ) : PaymentsHubUiState

    data class Error(val message: String) : PaymentsHubUiState
}

data class PaymentsHubState(
    val uiState: PaymentsHubUiState = PaymentsHubUiState.Loading,
)

sealed interface PaymentsHubAction {
    data object RefreshActivity : PaymentsHubAction
    data object RetryLoad : PaymentsHubAction
}
