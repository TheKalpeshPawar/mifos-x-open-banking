/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)

package org.mifosx.openbanking.feature.pfm

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.ui.datepicker.DateRangePickerDialog
import org.mifosx.openbanking.core.ui.picker.AccountPickerBottomSheet
import org.mifosx.openbanking.feature.pfm.ui.PfmContent
import org.mifosx.openbanking.feature.pfm.ui.PfmDashboardViewModel
import org.mifosx.openbanking.feature.pfm.ui.PfmPeriod
import template.core.base.store.screen.ScreenState
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Spending Insights — period summary, category donut, budgets and top merchants for one
 * account. All analytics run client-side over the sandbox's 50-newest-transactions
 * window; budgets persist as OBP personal-data fields.
 */
@Composable
fun PfmDashboardScreen(
    onViewTransactions: (bankId: String, accountId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PfmDashboardViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(notice) {
        notice?.let { message ->
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
            viewModel.onNoticeConsumed()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Spending Insights", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is ScreenState.Loading -> PfmCentered {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                is ScreenState.Error,
                is ScreenState.NoNetwork,
                is ScreenState.Unauthenticated,
                is ScreenState.Empty,
                -> PfmErrorState(onRetry = viewModel::onRetry)
                is ScreenState.Content -> PfmDashboardContent(
                    content = s.data,
                    viewModel = viewModel,
                    onViewTransactions = onViewTransactions,
                )
            }
        }
    }
}

@Composable
private fun PfmDashboardContent(
    content: PfmContent,
    viewModel: PfmDashboardViewModel,
    onViewTransactions: (String, String) -> Unit,
) {
    var showAccountPicker by remember { mutableStateOf(false) }
    var showRangePicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PfmAccountRow(content, onClick = { showAccountPicker = true })
        PfmPeriodChips(
            selected = content.period,
            onSelect = viewModel::onPeriodSelected,
            onCustom = { showRangePicker = true },
        )
        Text(
            content.periodLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(16.dp))
        if (content.hasActivity) {
            PfmSummaryCard(content)
            Spacer(Modifier.height(16.dp))
            PfmBudgetOverview(content, onSaveBudget = viewModel::onSaveBudget)
            PfmCategorySection(content)
            PfmBudgetRowsSection(content, onSaveBudget = viewModel::onSaveBudget)
            PfmMerchantsSection(content, onMerchantClick = {
                onViewTransactions(content.bankId, content.selectedAccountId)
            })
            TextButton(
                onClick = { onViewTransactions(content.bankId, content.selectedAccountId) },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTag(PfmDashboardTestTags.VIEW_ALL_TRANSACTIONS),
            ) { Text("View All Transactions") }
            Spacer(Modifier.height(24.dp))
        } else {
            PfmNoActivity()
        }
    }

    if (showAccountPicker) {
        AccountPickerBottomSheet(
            accounts = content.accounts,
            selectedAccountId = content.selectedAccountId,
            onAccountSelected = { account ->
                showAccountPicker = false
                viewModel.onAccountSelected(account.accountIdOrId)
            },
            onDismiss = { showAccountPicker = false },
            title = "Insights for account",
        )
    }
    if (showRangePicker) {
        val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
        DateRangePickerDialog(
            onDismiss = { showRangePicker = false },
            onApply = { start, end ->
                showRangePicker = false
                viewModel.onCustomRangeSelected(start, end)
            },
            maxDate = today,
        )
    }
}

@Composable
private fun PfmAccountRow(content: PfmContent, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
    ) {
        AssistChip(
            onClick = onClick,
            label = { Text(content.accountLabel) },
            trailingIcon = {
                Icon(Icons.Filled.ExpandMore, contentDescription = "Switch account")
            },
            modifier = Modifier.testTag(PfmDashboardTestTags.ACCOUNT_SELECTOR),
        )
    }
}

@Composable
private fun PfmPeriodChips(
    selected: PfmPeriod,
    onSelect: (PfmPeriod) -> Unit,
    onCustom: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        PfmPeriod.entries.forEach { period ->
            FilterChip(
                selected = period == selected,
                onClick = { if (period == PfmPeriod.CUSTOM) onCustom() else onSelect(period) },
                label = { Text(period.label) },
                modifier = Modifier
                    .padding(end = 8.dp)
                    .testTag(PfmDashboardTestTags.periodChip(period.name)),
            )
        }
    }
}

@Composable
private fun PfmNoActivity() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
    ) {
        Icon(
            Icons.Outlined.ReceiptLong,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "No transaction history yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Your spending insights will appear once you have transactions in the selected period.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PfmErrorState(onRetry: () -> Unit) {
    PfmCentered {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Icon(
                Icons.Outlined.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Could not load spending insights",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Check your connection and try again.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
private fun PfmCentered(content: @Composable () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { content() }
}
