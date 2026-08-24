/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.directdebits

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
import org.mifosx.openbanking.feature.directdebits.generated.resources.Res
import org.mifosx.openbanking.feature.directdebits.generated.resources.feature_direct_debits_empty_title
import org.mifosx.openbanking.feature.directdebits.generated.resources.feature_direct_debits_error_consent_revoked
import org.mifosx.openbanking.feature.directdebits.generated.resources.feature_direct_debits_error_network
import org.mifosx.openbanking.feature.directdebits.generated.resources.feature_direct_debits_error_rate_limited
import org.mifosx.openbanking.feature.directdebits.generated.resources.feature_direct_debits_error_server
import org.mifosx.openbanking.feature.directdebits.generated.resources.feature_direct_debits_error_token_expired
import org.mifosx.openbanking.feature.directdebits.generated.resources.feature_direct_debits_screen_title
import org.mifosx.openbanking.feature.directdebits.generated.resources.feature_direct_debits_unsupported_title
import org.mifosx.openbanking.feature.directdebits.ui.DirectDebitsAction
import org.mifosx.openbanking.feature.directdebits.ui.DirectDebitsErrorKind
import org.mifosx.openbanking.feature.directdebits.ui.DirectDebitsState
import org.mifosx.openbanking.feature.directdebits.ui.DirectDebitsUiState
import org.mifosx.openbanking.feature.directdebits.ui.DirectDebitsViewModel

/**
 * Direct-debits list for one account, pushed from the account-detail Explore row.
 *
 * Back navigation is delegated upward through [onBack]; the feature never sees the host route table.
 */
@Composable
internal fun DirectDebitsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DirectDebitsViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    KptScaffold(
        showNavigationIcon = true,
        onNavigationIconClick = onBack,
        title = stringResource(Res.string.feature_direct_debits_screen_title),
        modifier = modifier,
    ) {
        DirectDebitsScreenContent(
            state = state,
            onAction = viewModel::trySendAction,
        )
    }
}

@Composable
internal fun DirectDebitsScreenContent(
    state: DirectDebitsState,
    onAction: (DirectDebitsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val current = state.uiState) {
        DirectDebitsUiState.Loading -> MifosProgressIndicator()

        is DirectDebitsUiState.Content -> DirectDebitsContent(
            mandates = current.mandates,
            activeCount = current.activeCount,
            inactiveCount = current.inactiveCount,
            modifier = modifier,
        )

        DirectDebitsUiState.Empty -> EmptyDataComponent(
            isEmptyData = true,
            message = stringResource(Res.string.feature_direct_debits_empty_title),
        )

        is DirectDebitsUiState.Unsupported -> MifosErrorComponent(
            message = current.message.ifBlank {
                stringResource(Res.string.feature_direct_debits_unsupported_title)
            },
        )

        is DirectDebitsUiState.Error -> MifosErrorComponent(
            message = stringResource(current.kind.bodyResource()),
            isRetryEnabled = current.kind.isRetriable,
            onRetry = { onAction(DirectDebitsAction.RetryLoad) },
        )
    }
}

private fun DirectDebitsErrorKind.bodyResource(): StringResource = when (this) {
    DirectDebitsErrorKind.TokenExpired -> Res.string.feature_direct_debits_error_token_expired
    DirectDebitsErrorKind.ConsentRevoked -> Res.string.feature_direct_debits_error_consent_revoked
    DirectDebitsErrorKind.RateLimited -> Res.string.feature_direct_debits_error_rate_limited
    DirectDebitsErrorKind.ServerError -> Res.string.feature_direct_debits_error_server
    DirectDebitsErrorKind.NetworkError -> Res.string.feature_direct_debits_error_network
}
