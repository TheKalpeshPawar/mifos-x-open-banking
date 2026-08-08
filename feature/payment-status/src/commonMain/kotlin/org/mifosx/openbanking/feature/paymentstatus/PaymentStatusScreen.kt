/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.paymentstatus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.ui.scaffold.KptScaffold
import org.mifosx.openbanking.core.ui.scaffold.rememberKptPullToRefreshState
import org.mifosx.openbanking.feature.paymentstatus.generated.resources.Res
import org.mifosx.openbanking.feature.paymentstatus.generated.resources.feature_payment_status_screen_title
import org.mifosx.openbanking.feature.paymentstatus.ui.PaymentStatusAction
import org.mifosx.openbanking.feature.paymentstatus.ui.PaymentStatusState
import org.mifosx.openbanking.feature.paymentstatus.ui.PaymentStatusUiState
import org.mifosx.openbanking.feature.paymentstatus.ui.PaymentStatusViewModel
import org.mifosx.openbanking.feature.paymentstatus.ui.isReading

/**
 * One submitted payment as the bank currently reports it.
 *
 * Pull-to-refresh is wired at the scaffold so the gesture also works from the error page, which is
 * exactly where someone reaches for it. It dispatches the same `RefreshStatus` the Refresh button
 * does, so both share one path through the view model — and the button stays, because the gesture
 * is undiscoverable on desktop and web where this screen also runs.
 */
@Composable
internal fun PaymentStatusScreen(
    onBack: () -> Unit,
    onStartNewPayment: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaymentStatusViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    KptScaffold(
        showNavigationIcon = true,
        onNavigationIconClick = onBack,
        title = stringResource(Res.string.feature_payment_status_screen_title),
        pullToRefreshState = rememberKptPullToRefreshState(
            isEnabled = true,
            isRefreshing = state.uiState.isReading,
            onRefresh = { viewModel.trySendAction(PaymentStatusAction.RefreshStatus) },
        ),
        modifier = modifier,
    ) {
        PaymentStatusScreenContent(
            state = state,
            onAction = viewModel::trySendAction,
            onStartNewPayment = onStartNewPayment,
        )
    }
}

/** The stateless half every UI suite drives directly. */
@Composable
internal fun PaymentStatusScreenContent(
    state: PaymentStatusState,
    onAction: (PaymentStatusAction) -> Unit,
    onStartNewPayment: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val current = state.uiState) {
        PaymentStatusUiState.Loading -> PaymentStatusSkeleton(modifier = modifier)

        is PaymentStatusUiState.Content -> PaymentStatusContent(
            state = current,
            onAction = onAction,
            onStartNewPayment = onStartNewPayment,
            modifier = modifier,
        )

        is PaymentStatusUiState.Error -> PaymentStatusError(
            kind = current.kind,
            onRetry = { onAction(PaymentStatusAction.RefreshStatus) },
            modifier = modifier,
        )
    }
}
