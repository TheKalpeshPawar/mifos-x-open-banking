/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.transactiondetail

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.ui.components.MifosErrorComponent
import org.mifosx.openbanking.core.ui.components.MifosProgressIndicator
import org.mifosx.openbanking.core.ui.scaffold.KptScaffold
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.Res
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.feature_transaction_detail_copy_success
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.feature_transaction_detail_error_consent_withdrawn
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.feature_transaction_detail_error_network
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.feature_transaction_detail_error_token_expired
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.feature_transaction_detail_error_unexpected
import org.mifosx.openbanking.feature.transactiondetail.generated.resources.feature_transaction_detail_screen_title
import org.mifosx.openbanking.feature.transactiondetail.ui.TransactionDetailAction
import org.mifosx.openbanking.feature.transactiondetail.ui.TransactionDetailErrorKind
import org.mifosx.openbanking.feature.transactiondetail.ui.TransactionDetailEvent
import org.mifosx.openbanking.feature.transactiondetail.ui.TransactionDetailState
import org.mifosx.openbanking.feature.transactiondetail.ui.TransactionDetailUiState
import org.mifosx.openbanking.feature.transactiondetail.ui.TransactionDetailViewModel

/**
 * Transaction detail for one transaction, pushed from a transactions-list row.
 *
 * Back navigation is delegated upward through [onBack]; the feature never sees the host route table.
 * A copy-reference tap is emitted by the view model as a one-shot event and performed here against the
 * composition's clipboard manager, then confirmed with a snackbar.
 */
@Composable
internal fun TransactionDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionDetailViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current

    val copySuccessMessage = stringResource(Res.string.feature_transaction_detail_copy_success)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is TransactionDetailEvent.CopyToClipboard -> {
                    clipboardManager.setText(AnnotatedString(event.text))
                    snackbarHostState.showSnackbar(copySuccessMessage)
                }
            }
        }
    }

    KptScaffold(
        showNavigationIcon = true,
        onNavigationIconClick = onBack,
        title = stringResource(Res.string.feature_transaction_detail_screen_title),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) {
        TransactionDetailScreenContent(
            state = state,
            onAction = viewModel::trySendAction,
            onBack = onBack,
        )
    }
}

@Composable
internal fun TransactionDetailScreenContent(
    state: TransactionDetailState,
    onAction: (TransactionDetailAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val current = state.uiState) {
        TransactionDetailUiState.Loading -> MifosProgressIndicator()

        is TransactionDetailUiState.Content -> TransactionDetailContent(
            transaction = current.transaction,
            onCopyReference = { reference ->
                onAction(TransactionDetailAction.CopyReference(reference))
            },
            modifier = modifier,
        )

        is TransactionDetailUiState.Error -> MifosErrorComponent(
            message = stringResource(current.kind.bodyResource()),
            isRetryEnabled = current.kind.recoverable,
            onRetry = { onAction(TransactionDetailAction.RetryLoad) },
        )

        TransactionDetailUiState.Empty -> TransactionDetailEmpty(
            onBack = onBack,
            modifier = modifier,
        )
    }
}

private fun TransactionDetailErrorKind.bodyResource(): StringResource = when (this) {
    TransactionDetailErrorKind.TokenExpired ->
        Res.string.feature_transaction_detail_error_token_expired
    TransactionDetailErrorKind.ConsentWithdrawn ->
        Res.string.feature_transaction_detail_error_consent_withdrawn
    TransactionDetailErrorKind.Network ->
        Res.string.feature_transaction_detail_error_network
    TransactionDetailErrorKind.Unexpected ->
        Res.string.feature_transaction_detail_error_unexpected
}
