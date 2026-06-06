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

package org.mifosx.openbanking.feature.standingorders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.core.model.obp.StandingOrder
import org.mifosx.openbanking.feature.standingorders.ui.CreateGate
import org.mifosx.openbanking.feature.standingorders.ui.StandingOrderFilter
import org.mifosx.openbanking.feature.standingorders.ui.StandingOrdersHeader
import org.mifosx.openbanking.feature.standingorders.ui.StandingOrdersViewModel
import org.mifosx.openbanking.feature.standingorders.ui.accountDisplayName
import org.mifosx.openbanking.feature.standingorders.ui.formatDate
import org.mifosx.openbanking.feature.standingorders.ui.formatMoney
import template.core.base.store.screen.ScreenState

/**
 * Standing Orders screen — recurring outgoing payments for the user's primary account,
 * derived from transaction history (OBP has no read endpoint for standing orders).
 * The extended FAB navigates to the New Standing Order screen.
 */
@Composable
fun StandingOrdersScreen(
    onBack: () -> Unit,
    onCreate: (accountId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StandingOrdersViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val header by viewModel.header.collectAsStateWithLifecycle()
    val createGate by viewModel.createGate.collectAsStateWithLifecycle()

    // Re-fires when this destination re-enters composition (e.g. returning from the
    // create screen), so a freshly created order shows up immediately.
    LaunchedEffect(Unit) { viewModel.onRefresh() }

    LaunchedEffect(createGate) {
        (createGate as? CreateGate.Ready)?.let { gate ->
            viewModel.onCreateGateConsumed()
            onCreate(gate.accountId)
        }
    }

    if (createGate is CreateGate.NoPayees) {
        NoPayeesDialog(onDismiss = viewModel::onCreateGateConsumed)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Standing Orders", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::onCreateClicked,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New order") },
                modifier = Modifier.testTag(StandingOrdersTestTags.CREATE_FAB),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Pinned header — picker + stats + filter chips stay put (and stats derive
            // from real data, zeroing out when nothing is loaded) while ONLY the list
            // below scrolls or shows loading/empty/error.
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                if (header.accounts.isNotEmpty()) {
                    AccountSelector(
                        accounts = header.accounts,
                        selectedAccountId = header.selectedAccountId,
                        onAccountSelected = viewModel::onAccountSelected,
                    )
                }
                StatsRow(header)
                FilterChips(selected = header.filter, onFilterChanged = viewModel::onFilterChanged)
            }
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when (val s = state) {
                    is ScreenState.Loading -> CenteredProgress()
                    is ScreenState.Empty -> EmptyState()
                    is ScreenState.Error -> ErrorState(onRetry = viewModel::onRetry)
                    is ScreenState.NoNetwork -> ErrorState(onRetry = viewModel::onRetry)
                    is ScreenState.Unauthenticated -> ErrorState(onRetry = viewModel::onRetry)
                    is ScreenState.Content -> OrdersList(
                        orders = s.data,
                        filter = header.filter,
                        fromAccountName = accountDisplayName(header.selectedAccount),
                    )
                }
            }
        }
    }
}

@Composable
private fun OrdersList(
    orders: List<StandingOrder>,
    filter: StandingOrderFilter,
    fromAccountName: String,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
    ) {
        if (orders.isEmpty()) {
            item {
                Text(
                    text = "No ${filter.name.lowercase()} standing orders",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            items(orders, key = { it.id }) { order ->
                StandingOrderCard(order = order, fromAccountName = fromAccountName)
            }
        }
    }
}

/** Shown when the selected account has no beneficiaries — standing orders pay an existing payee. */
@Composable
private fun NoPayeesDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text("No payees", fontWeight = FontWeight.SemiBold) },
        text = {
            Text(
                "This account has no beneficiaries yet. Add a beneficiary first — " +
                    "standing orders pay an existing payee.",
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        },
        modifier = Modifier
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .testTag(StandingOrdersTestTags.NO_PAYEES_DIALOG),
    )
}

/** From-account selector — standing orders are per-account, so this scopes the screen. */
@Composable
private fun AccountSelector(
    accounts: List<Account>,
    selectedAccountId: String,
    onAccountSelected: (String) -> Unit,
) {
    val selected = accounts.firstOrNull { it.accountIdOrId == selectedAccountId }
        ?: accounts.firstOrNull()
        ?: return
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Card(
            onClick = { if (accounts.size > 1) expanded = true },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .testTag(StandingOrdersTestTags.ACCOUNT_SELECTOR),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountBalance,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "From account",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = accountDisplayName(selected),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (accounts.size > 1) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Change account",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = { Text(accountDisplayName(account)) },
                    onClick = {
                        onAccountSelected(account.accountIdOrId)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun StatsRow(header: StandingOrdersHeader) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 14.dp)
            .testTag(StandingOrdersTestTags.TITLE_COUNT_ROW),
    ) {
        StatCard("ACTIVE", header.activeCount.toString(), Modifier.weight(1f))
        StatCard("MONTHLY TOTAL", header.monthlyTotal, Modifier.weight(1.3f))
        StatCard("PAUSED", header.pausedCount.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun FilterChips(selected: StandingOrderFilter, onFilterChanged: (StandingOrderFilter) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 14.dp),
    ) {
        StandingOrderFilter.entries.forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onFilterChanged(filter) },
                label = { Text(filter.name) },
                shape = RoundedCornerShape(50),
            )
        }
    }
}

@Composable
private fun StandingOrderCard(order: StandingOrder, fromAccountName: String) {
    val muted = !order.isActive
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .testTag("${StandingOrdersTestTags.ORDER_CARD}_${order.id}"),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = order.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (muted) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f, fill = false),
                )
                StatusBadge(status = order.status)
            }
            if (order.counterpartyName.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                val accountHint = maskAccountHint(order.counterpartyAccount)
                    .takeIf { it.isNotBlank() }
                    ?.let { " · Acc ••$it" }
                    .orEmpty()
                Text(
                    text = "To ${order.counterpartyName}$accountHint",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (fromAccountName.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "From $fromAccountName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatMoney(order.amountValue, order.amountCurrency),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (muted) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
                Text(
                    text = frequencyLabel(order.frequency),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (order.isActive) Icons.Filled.CalendarMonth else Icons.Filled.PauseCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = scheduleLabel(order),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** "MONTHLY" -> "Monthly", "BI-WEEKLY" -> "Fortnightly". */
private fun frequencyLabel(frequency: String): String = when (frequency.uppercase()) {
    "BI-WEEKLY" -> "Fortnightly"
    else -> frequency.lowercase().replaceFirstChar { it.uppercase() }
}

/** "ac.savings.001" -> "s001": last 4 alphanumerics for the "Acc ••s001" hint. */
private fun maskAccountHint(id: String): String =
    id.filter { it.isLetterOrDigit() }.takeLast(4)

private fun scheduleLabel(order: StandingOrder): String = when {
    order.isActive && order.nextPaymentDate.isNotBlank() -> "Next: ${formatDate(order.nextPaymentDate)}"
    order.isPaused && order.lastPaymentDate.isNotBlank() -> "Paused since ${formatDate(order.lastPaymentDate)}"
    order.isCancelled && order.lastPaymentDate.isNotBlank() -> "Last paid ${formatDate(order.lastPaymentDate)}"
    else -> ""
}

@Composable
private fun StatusBadge(status: String) {
    val (bg, fg, label) = when (status) {
        StandingOrder.STATUS_ACTIVE -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            "Active",
        )
        StandingOrder.STATUS_PAUSED -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "Paused",
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Cancelled",
        )
    }
    Box(
        modifier = Modifier
            .background(color = bg, shape = RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = fg)
    }
}

@Composable
private fun CenteredProgress() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag(StandingOrdersTestTags.EMPTY_STATE),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.EventRepeat,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No standing orders",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Set up recurring payments to automate your regular bills",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ErrorState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag(StandingOrdersTestTags.ERROR_STATE),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Unable to load standing orders",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Check your connection and try again",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onRetry) { Text("Retry") }
    }
}
