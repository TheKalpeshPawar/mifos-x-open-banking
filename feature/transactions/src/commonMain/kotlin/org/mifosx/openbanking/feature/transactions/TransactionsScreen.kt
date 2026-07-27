/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.transactions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.ui.scaffold.KptScaffold
import org.mifosx.openbanking.feature.transactions.components.LoadMoreButton
import org.mifosx.openbanking.feature.transactions.components.PaginationLoader
import org.mifosx.openbanking.feature.transactions.components.PeriodSummary
import org.mifosx.openbanking.feature.transactions.components.TransactionFilterChips
import org.mifosx.openbanking.feature.transactions.components.TransactionRow
import org.mifosx.openbanking.feature.transactions.components.TransactionSearchField
import org.mifosx.openbanking.feature.transactions.generated.resources.Res
import org.mifosx.openbanking.feature.transactions.generated.resources.feature_transactions_date_apply
import org.mifosx.openbanking.feature.transactions.generated.resources.feature_transactions_date_cancel
import org.mifosx.openbanking.feature.transactions.generated.resources.feature_transactions_screen_title
import org.mifosx.openbanking.feature.transactions.ui.TransactionsAction
import org.mifosx.openbanking.feature.transactions.ui.TransactionsData
import org.mifosx.openbanking.feature.transactions.ui.TransactionsState
import org.mifosx.openbanking.feature.transactions.ui.TransactionsUiState
import org.mifosx.openbanking.feature.transactions.ui.TransactionsViewModel
import template.core.base.designsystem.theme.KptTheme
import kotlin.time.Instant

/**
 * Account-scoped transaction history: a date-grouped, filterable, searchable list paged transparently
 * over the OBIE `Links.Next` cursor. Navigation is delegated to the caller.
 */
@Composable
internal fun TransactionsScreen(
    onNavigateToTransactionDetail: (transactionId: String, accountId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionsViewModel = koinViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    KptScaffold(
        showNavigationIcon = true,
        onNavigationIconClick = onBack,
        title = stringResource(Res.string.feature_transactions_screen_title),
        modifier = modifier,
    ) {
        TransactionsScreenContent(
            state = state,
            onAction = viewModel::trySendAction,
            onRowClick = { transactionId -> onNavigateToTransactionDetail(transactionId, state.accountId) },
        )
    }
}

@Composable
internal fun TransactionsScreenContent(
    state: TransactionsState,
    onAction: (TransactionsAction) -> Unit,
    onRowClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (val ui = state.uiState) {
            TransactionsUiState.Loading -> TransactionsSkeleton()
            is TransactionsUiState.Error -> TransactionsError(
                kind = ui.kind,
                onRetry = { onAction(TransactionsAction.RetryLoad) },
            )

            is TransactionsUiState.Content -> TransactionsBrowse(state, ui.data, onAction, onRowClick)
            TransactionsUiState.Empty -> TransactionsBrowse(state, data = null, onAction, onRowClick)
        }
    }

    if (state.showDateRangePicker) {
        TransactionsDateRangeDialog(
            onConfirm = { from, to -> onAction(TransactionsAction.SetDateRange(from, to)) },
            onDismiss = { onAction(TransactionsAction.DismissDateRangePicker) },
        )
    }
}

/**
 * The chips + search surround (always shown), with either the date-grouped list ([data] non-null) or
 * the empty state below it.
 */
@Composable
private fun TransactionsBrowse(
    state: TransactionsState,
    data: TransactionsData?,
    onAction: (TransactionsAction) -> Unit,
    onRowClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().testTag(TransactionsTestTags.CONTENT),
    ) {
        if (data != null) {
            PeriodSummary(moneyInLabel = data.moneyInLabel, moneyOutLabel = data.moneyOutLabel)
        }
        TransactionFilterChips(
            active = state.activeFilter,
            dateRangeActive = state.dateFrom != null,
            onFilter = { onAction(TransactionsAction.FilterTransactions(it)) },
            onDateRange = { onAction(TransactionsAction.OpenDateRangePicker) },
        )
        TransactionSearchField(
            query = state.query,
            onQueryChange = { onAction(TransactionsAction.SearchTransactions(it)) },
        )
        if (data != null) {
            TransactionsList(
                data = data,
                hasNextPage = state.hasNextPage,
                isPaginating = state.isPaginating,
                onLoadMore = { onAction(TransactionsAction.LoadMore) },
                onRowClick = onRowClick,
                modifier = Modifier.weight(1f),
            )
        } else {
            TransactionsEmpty(
                onClearFilters = { onAction(TransactionsAction.ClearFilters) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TransactionsList(
    data: TransactionsData,
    hasNextPage: Boolean,
    isPaginating: Boolean,
    onLoadMore: () -> Unit,
    onRowClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize().testTag(TransactionsTestTags.LIST)) {
        data.groups.forEach { group ->
            item(key = "header-${group.dateLabel}") {
                Text(
                    text = group.dateLabel,
                    style = KptTheme.typography.labelMedium,
                    color = KptTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        horizontal = KptTheme.spacing.md,
                        vertical = KptTheme.spacing.sm,
                    ),
                )
            }
            items(group.rows, key = { it.key }) { row ->
                TransactionRow(row = row, onClick = { onRowClick(row.transactionId) })
                HorizontalDivider(color = KptTheme.colorScheme.outlineVariant)
            }
        }
        if (isPaginating) {
            item(key = "pagination-loader") { PaginationLoader() }
        } else if (hasNextPage) {
            item(key = "load-more") { LoadMoreButton(onClick = onLoadMore) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionsDateRangeDialog(
    onConfirm: (from: LocalDate, to: LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberDateRangePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val from = pickerState.selectedStartDateMillis?.toLocalDate()
                    val to = pickerState.selectedEndDateMillis?.toLocalDate()
                    if (from != null && to != null) onConfirm(from, to) else onDismiss()
                },
            ) {
                Text(stringResource(Res.string.feature_transactions_date_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.feature_transactions_date_cancel))
            }
        },
    ) {
        DateRangePicker(state = pickerState)
    }
}

private fun Long.toLocalDate(): LocalDate =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault()).date
