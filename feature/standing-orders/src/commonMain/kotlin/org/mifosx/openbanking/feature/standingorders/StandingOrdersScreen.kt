/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.standingorders

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.ui.scaffold.KptScaffold
import org.mifosx.openbanking.core.ui.scaffold.rememberKptPullToRefreshState
import org.mifosx.openbanking.feature.standingorders.generated.resources.Res
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_screen_title
import org.mifosx.openbanking.feature.standingorders.ui.StandingOrdersAction
import org.mifosx.openbanking.feature.standingorders.ui.StandingOrdersState
import org.mifosx.openbanking.feature.standingorders.ui.StandingOrdersUiState
import org.mifosx.openbanking.feature.standingorders.ui.StandingOrdersViewModel

/**
 * Standing-orders list for one account, pushed from the account-detail Explore row.
 *
 * Back navigation is delegated upward through [onBack]; the feature never sees the host route
 * table.
 *
 * Pull-to-refresh is wired at the scaffold rather than inside the list so the gesture works from
 * the empty and error states too — an account that just failed to load is exactly where a user
 * will reach for it, and a refresh available only once content exists would be missing whenever it
 * mattered most. It dispatches the same `RetryLoad` action the Retry button does, so both paths
 * share one code path in the view model.
 */
@Composable
internal fun StandingOrdersScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StandingOrdersViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    KptScaffold(
        showNavigationIcon = true,
        onNavigationIconClick = onBack,
        title = stringResource(Res.string.feature_standing_orders_screen_title),
        pullToRefreshState = rememberKptPullToRefreshState(
            isEnabled = true,
            isRefreshing = state.uiState is StandingOrdersUiState.Loading,
            onRefresh = { viewModel.trySendAction(StandingOrdersAction.RetryLoad) },
        ),
        modifier = modifier,
    ) {
        StandingOrdersScreenContent(
            state = state,
            onAction = viewModel::trySendAction,
        )
    }
}

@Composable
internal fun StandingOrdersScreenContent(
    state: StandingOrdersState,
    onAction: (StandingOrdersAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val current = state.uiState) {
        StandingOrdersUiState.Loading -> StandingOrdersSkeleton(modifier = modifier)

        is StandingOrdersUiState.Content -> StandingOrdersContent(
            orders = current.orders,
            activeCount = current.activeCount,
            inactiveCount = current.inactiveCount,
            modifier = modifier,
        )

        StandingOrdersUiState.Empty -> StandingOrdersEmpty(modifier = modifier)

        is StandingOrdersUiState.Unsupported -> StandingOrdersUnsupported(
            message = current.message,
            modifier = modifier,
        )

        is StandingOrdersUiState.Error -> StandingOrdersError(
            kind = current.kind,
            onRetry = { onAction(StandingOrdersAction.RetryLoad) },
            modifier = modifier,
        )
    }
}
