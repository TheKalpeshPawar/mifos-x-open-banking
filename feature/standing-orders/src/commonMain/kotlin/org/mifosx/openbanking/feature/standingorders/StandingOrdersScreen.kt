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
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.ui.components.EmptyDataComponent
import org.mifosx.openbanking.core.ui.components.MifosErrorComponent
import org.mifosx.openbanking.core.ui.components.MifosProgressIndicator
import org.mifosx.openbanking.core.ui.scaffold.KptScaffold
import org.mifosx.openbanking.feature.standingorders.generated.resources.Res
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_empty_title
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_error_consent_revoked
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_error_network
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_error_rate_limited
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_error_server
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_error_token_expired
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_screen_title
import org.mifosx.openbanking.feature.standingorders.generated.resources.feature_standing_orders_unsupported_title
import org.mifosx.openbanking.feature.standingorders.ui.StandingOrdersAction
import org.mifosx.openbanking.feature.standingorders.ui.StandingOrdersErrorKind
import org.mifosx.openbanking.feature.standingorders.ui.StandingOrdersState
import org.mifosx.openbanking.feature.standingorders.ui.StandingOrdersUiState
import org.mifosx.openbanking.feature.standingorders.ui.StandingOrdersViewModel

/**
 * Standing-orders list for one account, pushed from the account-detail Explore row.
 *
 * Back navigation is delegated upward through [onBack]; the feature never sees the host route
 * table.
 *
 * There is no pull-to-refresh. `KptScaffold` draws its refresh indicator from the state it is given
 * and this screen's only refresh signal is its `Loading` state, which already renders a full-screen
 * loading animation — so the gesture put a second spinner on top of the first. Refreshing is offered
 * by the Retry button on the error state instead.
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
        StandingOrdersUiState.Loading -> MifosProgressIndicator()

        is StandingOrdersUiState.Content -> StandingOrdersContent(
            orders = current.orders,
            modifier = modifier,
        )

        StandingOrdersUiState.Empty -> EmptyDataComponent(
            isEmptyData = true,
            message = stringResource(Res.string.feature_standing_orders_empty_title),
        )

        is StandingOrdersUiState.Unsupported -> MifosErrorComponent(
            message = current.message.ifBlank {
                stringResource(Res.string.feature_standing_orders_unsupported_title)
            },
        )

        is StandingOrdersUiState.Error -> MifosErrorComponent(
            message = stringResource(current.kind.bodyResource()),
            isRetryEnabled = true,
            onRetry = { onAction(StandingOrdersAction.RetryLoad) },
        )
    }
}

private fun StandingOrdersErrorKind.bodyResource(): StringResource = when (this) {
    StandingOrdersErrorKind.TokenExpired -> Res.string.feature_standing_orders_error_token_expired
    StandingOrdersErrorKind.ConsentRevoked -> Res.string.feature_standing_orders_error_consent_revoked
    StandingOrdersErrorKind.RateLimited -> Res.string.feature_standing_orders_error_rate_limited
    StandingOrdersErrorKind.ServerError -> Res.string.feature_standing_orders_error_server
    StandingOrdersErrorKind.NetworkError -> Res.string.feature_standing_orders_error_network
}
