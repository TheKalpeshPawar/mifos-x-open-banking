/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.scheduledpayments

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
import org.mifosx.openbanking.feature.scheduledpayments.generated.resources.Res
import org.mifosx.openbanking.feature.scheduledpayments.generated.resources.feature_scheduled_payments_empty_title
import org.mifosx.openbanking.feature.scheduledpayments.generated.resources.feature_scheduled_payments_error_consent_revoked
import org.mifosx.openbanking.feature.scheduledpayments.generated.resources.feature_scheduled_payments_error_network
import org.mifosx.openbanking.feature.scheduledpayments.generated.resources.feature_scheduled_payments_error_rate_limited
import org.mifosx.openbanking.feature.scheduledpayments.generated.resources.feature_scheduled_payments_error_token_expired
import org.mifosx.openbanking.feature.scheduledpayments.generated.resources.feature_scheduled_payments_screen_title
import org.mifosx.openbanking.feature.scheduledpayments.ui.ScheduledPaymentsAction
import org.mifosx.openbanking.feature.scheduledpayments.ui.ScheduledPaymentsError
import org.mifosx.openbanking.feature.scheduledpayments.ui.ScheduledPaymentsState
import org.mifosx.openbanking.feature.scheduledpayments.ui.ScheduledPaymentsUiState
import org.mifosx.openbanking.feature.scheduledpayments.ui.ScheduledPaymentsViewModel

/**
 * Scheduled-payments list for one account, pushed from the account-detail Scheduled chip.
 *
 * Back navigation is delegated upward through [onBack]; the feature never sees the host route table.
 */
@Composable
internal fun ScheduledPaymentsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScheduledPaymentsViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    KptScaffold(
        showNavigationIcon = true,
        onNavigationIconClick = onBack,
        title = stringResource(Res.string.feature_scheduled_payments_screen_title),
        modifier = modifier,
    ) {
        ScheduledPaymentsScreenContent(
            state = state,
            onAction = viewModel::trySendAction,
        )
    }
}

@Composable
internal fun ScheduledPaymentsScreenContent(
    state: ScheduledPaymentsState,
    onAction: (ScheduledPaymentsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val current = state.uiState) {
        ScheduledPaymentsUiState.Loading -> MifosProgressIndicator()

        is ScheduledPaymentsUiState.Content -> ScheduledPaymentsContent(
            payments = current.payments,
            modifier = modifier,
        )

        ScheduledPaymentsUiState.Empty -> EmptyDataComponent(
            isEmptyData = true,
            message = stringResource(Res.string.feature_scheduled_payments_empty_title),
        )

        is ScheduledPaymentsUiState.Error -> MifosErrorComponent(
            message = stringResource(current.kind.bodyResource()),
            isRetryEnabled = true,
            onRetry = { onAction(ScheduledPaymentsAction.RetryLoad) },
        )
    }
}

private fun ScheduledPaymentsError.bodyResource(): StringResource = when (this) {
    ScheduledPaymentsError.TokenExpired -> Res.string.feature_scheduled_payments_error_token_expired
    ScheduledPaymentsError.ConsentRevoked -> Res.string.feature_scheduled_payments_error_consent_revoked
    ScheduledPaymentsError.RateLimited -> Res.string.feature_scheduled_payments_error_rate_limited
    ScheduledPaymentsError.NetworkError -> Res.string.feature_scheduled_payments_error_network
}
