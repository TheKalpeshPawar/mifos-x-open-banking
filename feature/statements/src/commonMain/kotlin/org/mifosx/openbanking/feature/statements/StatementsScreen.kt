/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.statements

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.ui.components.EmptyDataComponent
import org.mifosx.openbanking.core.ui.components.MifosErrorComponent
import org.mifosx.openbanking.core.ui.components.MifosProgressIndicator
import org.mifosx.openbanking.core.ui.scaffold.KptScaffold
import org.mifosx.openbanking.feature.statements.generated.resources.Res
import org.mifosx.openbanking.feature.statements.generated.resources.feature_statements_download_error
import org.mifosx.openbanking.feature.statements.generated.resources.feature_statements_download_unavailable
import org.mifosx.openbanking.feature.statements.generated.resources.feature_statements_empty_title
import org.mifosx.openbanking.feature.statements.generated.resources.feature_statements_error_consent_scope
import org.mifosx.openbanking.feature.statements.generated.resources.feature_statements_error_network
import org.mifosx.openbanking.feature.statements.generated.resources.feature_statements_error_rate_limited
import org.mifosx.openbanking.feature.statements.generated.resources.feature_statements_error_server
import org.mifosx.openbanking.feature.statements.generated.resources.feature_statements_error_token_expired
import org.mifosx.openbanking.feature.statements.generated.resources.feature_statements_screen_title
import org.mifosx.openbanking.feature.statements.ui.StatementsAction
import org.mifosx.openbanking.feature.statements.ui.StatementsErrorKind
import org.mifosx.openbanking.feature.statements.ui.StatementsEvent
import org.mifosx.openbanking.feature.statements.ui.StatementsState
import org.mifosx.openbanking.feature.statements.ui.StatementsUiState
import org.mifosx.openbanking.feature.statements.ui.StatementsViewModel

/**
 * Statements list for one account, pushed from the account-detail Explore row.
 *
 * Back navigation is delegated upward through [onBack] and a row tap through
 * [onNavigateToStatementDetail]; the feature never sees the host route table. Download failures are
 * surfaced as a snackbar, driven by the view model's one-shot event flow.
 */
@Composable
internal fun StatementsScreen(
    onBack: () -> Unit,
    onNavigateToStatementDetail: (statementId: String, accountId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatementsViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val unavailableMessage = stringResource(Res.string.feature_statements_download_unavailable)
    val errorMessage = stringResource(Res.string.feature_statements_download_error)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            val message = when (event) {
                StatementsEvent.ShowDownloadUnavailable -> unavailableMessage
                StatementsEvent.ShowDownloadError -> errorMessage
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    KptScaffold(
        showNavigationIcon = true,
        onNavigationIconClick = onBack,
        title = stringResource(Res.string.feature_statements_screen_title),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) {
        StatementsScreenContent(
            state = state,
            onAction = viewModel::trySendAction,
            onRowClick = { statementId -> onNavigateToStatementDetail(statementId, state.accountId) },
        )
    }
}

@Composable
internal fun StatementsScreenContent(
    state: StatementsState,
    onAction: (StatementsAction) -> Unit,
    onRowClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val current = state.uiState) {
        StatementsUiState.Loading -> MifosProgressIndicator()

        is StatementsUiState.Content -> StatementsContent(
            statements = current.statements,
            downloadState = state.downloadState,
            onRowClick = onRowClick,
            onDownload = { statementId -> onAction(StatementsAction.DownloadStatement(statementId)) },
            modifier = modifier,
        )

        StatementsUiState.Empty -> EmptyDataComponent(
            isEmptyData = true,
            message = stringResource(Res.string.feature_statements_empty_title),
        )

        is StatementsUiState.Error -> MifosErrorComponent(
            message = stringResource(current.kind.bodyResource()),
            isRetryEnabled = true,
            onRetry = { onAction(StatementsAction.RetryLoad) },
        )
    }
}

private fun StatementsErrorKind.bodyResource(): StringResource = when (this) {
    StatementsErrorKind.TokenExpired -> Res.string.feature_statements_error_token_expired
    StatementsErrorKind.ConsentScope -> Res.string.feature_statements_error_consent_scope
    StatementsErrorKind.RateLimited -> Res.string.feature_statements_error_rate_limited
    StatementsErrorKind.ServerError -> Res.string.feature_statements_error_server
    StatementsErrorKind.NetworkError -> Res.string.feature_statements_error_network
}
