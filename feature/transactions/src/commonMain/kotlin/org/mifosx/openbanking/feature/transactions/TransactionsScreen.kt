/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
@file:OptIn(ExperimentalMaterial3Api::class)

package org.mifosx.openbanking.feature.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.mifosx.openbanking.core.model.obp.Transaction
import org.mifosx.openbanking.feature.transactions.ui.DateRangePreset
import org.mifosx.openbanking.feature.transactions.ui.MonthlySummary
import org.mifosx.openbanking.feature.transactions.ui.PendingPayment
import org.mifosx.openbanking.feature.transactions.ui.TransactionTypeFilter
import org.mifosx.openbanking.feature.transactions.ui.TransactionsContent
import org.mifosx.openbanking.feature.transactions.ui.TransactionsViewModel
import org.mifosx.openbanking.feature.transactions.ui.completedDate
import template.core.base.store.screen.ScreenState

/**
 * Transaction History for one account. Pinned header (search + date range + type chips);
 * pending payments (initiated, awaiting SCA confirmation) sit muted above the booked,
 * date-grouped rows. Pagination is client-side, 10 rows per Load More.
 */
@Composable
fun TransactionsScreen(
    bankId: String,
    accountId: String,
    onTransactionClick: (String) -> Unit,
    onPendingClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionsViewModel = koinViewModel { parametersOf(bankId, accountId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Transactions", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is ScreenState.Loading -> {
                    Header(content = null, viewModel = viewModel)
                    Centered { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
                }
                is ScreenState.Empty -> {
                    Header(content = null, viewModel = viewModel)
                    EmptyState(onClear = {
                        viewModel.onQueryChanged("")
                        viewModel.onFilterChanged(TransactionTypeFilter.ALL)
                        viewModel.onRangePresetSelected(DateRangePreset.ALL)
                    })
                }
                is ScreenState.Error,
                is ScreenState.NoNetwork,
                is ScreenState.Unauthenticated,
                -> {
                    Header(content = null, viewModel = viewModel)
                    ErrorState(onRetry = viewModel::onRetry)
                }
                is ScreenState.Content -> {
                    Header(content = s.data, viewModel = viewModel)
                    TransactionsList(
                        content = s.data,
                        onTransactionClick = onTransactionClick,
                        onPendingClick = onPendingClick,
                        onLoadMore = viewModel::onLoadMore,
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(content: TransactionsContent?, viewModel: TransactionsViewModel) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = content?.query.orEmpty(),
            onValueChange = viewModel::onQueryChanged,
            placeholder = { Text("Search by merchant, amount, date...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag(TransactionsTestTags.SEARCH_BAR),
        )
        Spacer(Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            DateRangeChip(
                label = content?.range?.label ?: DateRangePreset.LAST_30_DAYS.label,
                onPreset = viewModel::onRangePresetSelected,
                onCustom = viewModel::onCustomRangeSelected,
            )
            TypeChips(selected = content?.filter ?: TransactionTypeFilter.ALL, viewModel = viewModel)
        }
        Spacer(Modifier.height(10.dp))
        content?.summary?.let { SummaryCard(it) }
    }
}

@Composable
private fun DateRangeChip(
    label: String,
    onPreset: (DateRangePreset) -> Unit,
    onCustom: (LocalDate, LocalDate) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var pickerOpen by remember { mutableStateOf(false) }

    Box {
        FilterChip(
            selected = false,
            onClick = { menuOpen = true },
            label = { Text(label) },
            leadingIcon = {
                Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                Icon(Icons.Filled.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            modifier = Modifier.testTag(TransactionsTestTags.DATE_RANGE_CHIP),
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DateRangePreset.entries.filter { it != DateRangePreset.CUSTOM }.forEach { preset ->
                DropdownMenuItem(
                    text = { Text(preset.label) },
                    onClick = {
                        menuOpen = false
                        onPreset(preset)
                    },
                )
            }
            DropdownMenuItem(
                text = { Text("Custom…") },
                onClick = {
                    menuOpen = false
                    pickerOpen = true
                },
            )
        }
    }

    if (pickerOpen) {
        CustomRangeDialog(
            onDismiss = { pickerOpen = false },
            onConfirm = { start, end ->
                pickerOpen = false
                onCustom(start, end)
            },
        )
    }
}

@Composable
private fun CustomRangeDialog(onDismiss: () -> Unit, onConfirm: (LocalDate, LocalDate) -> Unit) {
    val pickerState = rememberDateRangePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = pickerState.selectedStartDateMillis != null && pickerState.selectedEndDateMillis != null,
                onClick = {
                    val start = pickerState.selectedStartDateMillis ?: return@TextButton
                    val end = pickerState.selectedEndDateMillis ?: return@TextButton
                    onConfirm(start.toLocalDate(), end.toLocalDate())
                },
            ) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DateRangePicker(
            state = pickerState,
            title = {
                Text(
                    "Select date range",
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            showModeToggle = false,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TypeChips(selected: TransactionTypeFilter, viewModel: TransactionsViewModel) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.testTag(TransactionsTestTags.FILTER_CHIPS_ROW),
    ) {
        TransactionTypeFilter.entries.forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { viewModel.onFilterChanged(filter) },
                label = { Text(filter.label()) },
                modifier = Modifier.testTag(TransactionsTestTags.filterChip(filter.name)),
            )
        }
    }
}

@Composable
private fun SummaryCard(summary: MonthlySummary) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().testTag(TransactionsTestTags.SUMMARY_CARD),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SummaryColumn(
                label = "SPENT THIS MONTH",
                value = "-${symbol(summary.currency)}${summary.spent}",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .padding(vertical = 2.dp),
            ) { HorizontalDivider(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.outlineVariant) }
            Spacer(Modifier.width(16.dp))
            SummaryColumn(
                label = "RECEIVED",
                value = "+${symbol(summary.currency)}${summary.received}",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SummaryColumn(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun TransactionsList(
    content: TransactionsContent,
    onTransactionClick: (String) -> Unit,
    onPendingClick: (String) -> Unit,
    onLoadMore: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (content.pending.isNotEmpty()) {
            item(key = "pending_header") {
                SectionHeader("PENDING", Modifier.testTag(TransactionsTestTags.PENDING_HEADER))
            }
            items(content.pending, key = { "p_${it.id}" }) { pending ->
                PendingRow(pending, onClick = { onPendingClick(pending.id) })
            }
        }
        content.groups.forEach { group ->
            item(key = "h_${group.date}") { SectionHeader(formatDate(group.date)) }
            items(group.transactions, key = { it.txId }) { txn ->
                TransactionRow(txn, onClick = { onTransactionClick(txn.txId) })
            }
        }
        if (content.hasMore) {
            item(key = "load_more") {
                TextButton(
                    onClick = onLoadMore,
                    modifier = Modifier.fillMaxWidth().testTag(TransactionsTestTags.LOAD_MORE),
                ) { Text("Load More Transactions") }
            }
        }
        item(key = "bottom_spacer") { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 6.dp, bottom = 2.dp),
    )
}

@Composable
private fun PendingRow(pending: PendingPayment, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().testTag(TransactionsTestTags.pendingRow(pending.id)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Schedule,
                contentDescription = "Pending",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    pending.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Awaiting confirmation",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "-${symbol(pending.currency)}${pending.amount.trimStart('-')}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TransactionRow(txn: Transaction, onClick: () -> Unit) {
    val amount = txn.details.value.amount.toDoubleOrNull() ?: 0.0
    val debit = amount < 0
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().testTag(TransactionsTestTags.row(txn.txId)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    txn.details.description.ifBlank { txn.otherAccount.holder.name.ifBlank { "Transaction" } },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    txn.completedDate?.let {
                        Text(
                            formatDate(it),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            " · ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                    CategoryBadge(categoryLabel(txn))
                }
            }
            Text(
                formatSigned(amount, txn.details.value.currency),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (debit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun CategoryBadge(label: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun EmptyState(onClear: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "No transactions found",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "No transactions match your current filters. Try adjusting your search or date range.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onClear) { Text("Clear Filters") }
    }
}

@Composable
private fun ErrorState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Could not load transactions",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "We were unable to load your transactions. Check your connection and try again.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) { content() }
}
