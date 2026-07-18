/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.ui.scaffold.KptScaffold
import org.mifosx.openbanking.feature.home.ui.HomeAction
import org.mifosx.openbanking.feature.home.ui.HomeViewModel
import template.core.base.ui.screen.ScreenContent

/**
 * Consumer home dashboard. Renders the account overview across its loading / content / empty / error
 * states inside the app scaffold; navigation is delegated to the caller via the `onNavigate*`
 * callbacks so the feature stays decoupled from the app's route table.
 */
@Composable
internal fun HomeScreen(
    onNavigateToTransactions: () -> Unit,
    onNavigateToAccountDetail: () -> Unit,
    onNavigateToStatements: () -> Unit,
    onNavigateToConsents: () -> Unit,
    onNavigateToTransactionDetail: () -> Unit,
    onNavigateToSpending: () -> Unit,
    onConnectBank: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    KptScaffold(modifier = modifier) {
        ScreenContent(
            state = state.uiState,
            onRetry = { viewModel.trySendAction(HomeAction.RetryLoad) },
            loading = { HomeSkeleton() },
            empty = { HomeEmpty(onConnectBank = onConnectBank) },
            error = { HomeError(onRetry = { viewModel.trySendAction(HomeAction.RetryLoad) }) },
        ) { data, _ ->
            HomeContent(
                data = data,
                onSelectAccount = { viewModel.trySendAction(HomeAction.SelectAccount(it)) },
                onNavigateToTransactions = onNavigateToTransactions,
                onNavigateToAccountDetail = onNavigateToAccountDetail,
                onNavigateToStatements = onNavigateToStatements,
                onNavigateToConsents = onNavigateToConsents,
                onNavigateToTransactionDetail = onNavigateToTransactionDetail,
                onNavigateToSpending = onNavigateToSpending,
            )
        }
    }
}
