/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.paymentshub

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.ui.scaffold.KptScaffold
import org.mifosx.openbanking.feature.paymentshub.ui.PaymentsHubViewModel

@Composable
internal fun PaymentsHubScreen(
    onNavigateToSendMoney: () -> Unit,
    onNavigateToPaymentStatus: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaymentsHubViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    KptScaffold(
        showNavigationIcon = false,
        title = "Payments",
        modifier = modifier,
    ) {
        PaymentsHubContent(
            state = state,
            onAction = viewModel::trySendAction,
            onNavigateToSendMoney = onNavigateToSendMoney,
            onNavigateToPaymentStatus = onNavigateToPaymentStatus,
        )
    }
}
