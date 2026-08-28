/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.statementdetail

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
import org.mifosx.openbanking.core.ui.components.MifosErrorComponent
import org.mifosx.openbanking.core.ui.components.MifosProgressIndicator
import org.mifosx.openbanking.core.ui.scaffold.KptScaffold
import org.mifosx.openbanking.feature.statementdetail.generated.resources.Res
import org.mifosx.openbanking.feature.statementdetail.generated.resources.feature_statement_detail_download_error
import org.mifosx.openbanking.feature.statementdetail.generated.resources.feature_statement_detail_download_success
import org.mifosx.openbanking.feature.statementdetail.generated.resources.feature_statement_detail_error_consent_missing
import org.mifosx.openbanking.feature.statementdetail.generated.resources.feature_statement_detail_error_file_not_found
import org.mifosx.openbanking.feature.statementdetail.generated.resources.feature_statement_detail_error_network
import org.mifosx.openbanking.feature.statementdetail.generated.resources.feature_statement_detail_error_session_expired
import org.mifosx.openbanking.feature.statementdetail.generated.resources.feature_statement_detail_error_statement_not_found
import org.mifosx.openbanking.feature.statementdetail.generated.resources.feature_statement_detail_screen_title
import org.mifosx.openbanking.feature.statementdetail.ui.StatementDetailAction
import org.mifosx.openbanking.feature.statementdetail.ui.StatementDetailErrorCode
import org.mifosx.openbanking.feature.statementdetail.ui.StatementDetailEvent
import org.mifosx.openbanking.feature.statementdetail.ui.StatementDetailState
import org.mifosx.openbanking.feature.statementdetail.ui.StatementDetailUiState
import org.mifosx.openbanking.feature.statementdetail.ui.StatementDetailViewModel

/**
 * Statement detail for one statement, pushed from a statements-list row.
 *
 * Back navigation is delegated upward through [onBack] and a transaction-row tap through
 * [onNavigateToTransactionDetail]; the feature never sees the host route table. The top-bar title is
 * the statement reference once loaded, a generic fallback otherwise. Download outcomes are surfaced as
 * a snackbar, driven by the view model's one-shot event flow.
 */
@Composable
internal fun StatementDetailScreen(
    onBack: () -> Unit,
    onNavigateToTransactionDetail: (transactionId: String, accountId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatementDetailViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val successMessage = stringResource(Res.string.feature_statement_detail_download_success)
    val errorMessage = stringResource(Res.string.feature_statement_detail_download_error)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            val message = when (event) {
                StatementDetailEvent.DownloadSucceeded -> successMessage
                is StatementDetailEvent.DownloadFailed -> errorMessage
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    val fallbackTitle = stringResource(Res.string.feature_statement_detail_screen_title)
    KptScaffold(
        showNavigationIcon = true,
        onNavigationIconClick = onBack,
        title = state.uiState.screenTitle() ?: fallbackTitle,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) {
        StatementDetailScreenContent(
            state = state,
            onAction = viewModel::trySendAction,
            onRowClick = onNavigateToTransactionDetail,
        )
    }
}

@Composable
internal fun StatementDetailScreenContent(
    state: StatementDetailState,
    onAction: (StatementDetailAction) -> Unit,
    onRowClick: (transactionId: String, accountId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val current = state.uiState) {
        StatementDetailUiState.Loading -> MifosProgressIndicator()

        is StatementDetailUiState.Content -> StatementDetailContent(
            statement = current.statement,
            transactions = current.transactions,
            downloadState = state.downloadState,
            onRowClick = onRowClick,
            onDownload = { onAction(StatementDetailAction.DownloadPdf) },
            modifier = modifier,
        )

        is StatementDetailUiState.Empty -> StatementDetailEmpty(
            statement = current.statement,
            downloadState = state.downloadState,
            onDownload = { onAction(StatementDetailAction.DownloadPdf) },
            modifier = modifier,
        )

        is StatementDetailUiState.Error -> MifosErrorComponent(
            message = stringResource(current.code.bodyResource()),
            isRetryEnabled = true,
            onRetry = { onAction(StatementDetailAction.RetryLoad) },
        )
    }
}

private fun StatementDetailErrorCode.bodyResource(): StringResource = when (this) {
    StatementDetailErrorCode.StatementNotFound ->
        Res.string.feature_statement_detail_error_statement_not_found
    StatementDetailErrorCode.ConsentMissingReadStatements ->
        Res.string.feature_statement_detail_error_consent_missing
    StatementDetailErrorCode.SessionExpired ->
        Res.string.feature_statement_detail_error_session_expired
    StatementDetailErrorCode.StatementFileNotFound ->
        Res.string.feature_statement_detail_error_file_not_found
    StatementDetailErrorCode.NetworkError ->
        Res.string.feature_statement_detail_error_network
}

/** The top-bar title: the statement reference once loaded, or null to fall back to a generic title. */
private fun StatementDetailUiState.screenTitle(): String? = when (this) {
    is StatementDetailUiState.Content -> statement.reference.ifBlank { null }
    is StatementDetailUiState.Empty -> statement.reference.ifBlank { null }
    else -> null
}
