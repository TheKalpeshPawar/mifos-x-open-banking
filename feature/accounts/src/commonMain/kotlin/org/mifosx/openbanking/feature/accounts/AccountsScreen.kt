/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.feature.accounts.ui.AccountTypeFilter
import org.mifosx.openbanking.feature.accounts.ui.AccountsContent
import org.mifosx.openbanking.feature.accounts.ui.AccountsViewModel
import template.core.base.store.screen.ScreenState

/** Decorative amber accent for Business accounts (SPEC: decorative left-border only). */
private val BusinessAccent = Color(0xFFE8A317)

/**
 * My Accounts screen — Consumer accounts tab. Stateful container binding [AccountsViewModel]'s
 * offline-first [ScreenState] to the stateless content. Hosted inside the authenticated navbar
 * scaffold (bottom nav supplied by the host), so this screen owns only its body + FAB.
 */
@Composable
fun AccountsScreen(
    onAccountClick: (accountId: String) -> Unit,
    onRequestNewAccount: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activeFilter by viewModel.activeFilter.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        when (val s = state) {
            is ScreenState.Loading -> LoadingState()

            is ScreenState.Empty -> EmptyState(onRequestNewAccount = onRequestNewAccount)

            is ScreenState.Error ->
                ErrorState(onRetry = viewModel::onRetry)

            is ScreenState.NoNetwork ->
                ErrorState(onRetry = viewModel::onRetry)

            is ScreenState.Unauthenticated ->
                ErrorState(onRetry = viewModel::onRetry)

            is ScreenState.Content -> AccountsLoaded(
                content = s.data,
                activeFilter = activeFilter,
                onFilterChanged = viewModel::onFilterChanged,
                onAccountClick = onAccountClick,
            )
        }

        FloatingActionButton(
            onClick = onRequestNewAccount,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 80.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Request new account")
        }
    }
}

@Composable
private fun AccountsLoaded(
    content: AccountsContent,
    activeFilter: AccountTypeFilter,
    onFilterChanged: (AccountTypeFilter) -> Unit,
    onAccountClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item { Header() }
        item {
            FilterTabs(activeFilter = activeFilter, onFilterChanged = onFilterChanged)
        }

        if (content.filteredAccounts.isEmpty()) {
            item { NoMatchRow() }
        } else {
            items(content.filteredAccounts, key = { it.id }) { account ->
                AccountCard(account = account, onClick = { onAccountClick(account.id) })
            }
            item {
                TotalFooter(
                    count = content.filteredAccounts.size,
                    total = content.totalBalance,
                    currency = content.currency,
                )
            }
        }
    }
}

@Composable
private fun Header() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "My Accounts",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        IconButton(onClick = { /* open_account_help — wired with help feature */ }) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                contentDescription = "Account help",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterTabs(
    activeFilter: AccountTypeFilter,
    onFilterChanged: (AccountTypeFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AccountTypeFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == activeFilter,
                onClick = { onFilterChanged(filter) },
                label = { Text(filter.label, style = MaterialTheme.typography.labelMedium) },
            )
        }
    }
}

@Composable
private fun AccountCard(account: Account, onClick: () -> Unit) {
    val accent = accentColor(account)
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accent),
            )
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = account.label.ifBlank { "Account" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                TypeBadge(account = account)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = formatMoney(account.balance.amount, account.balance.currency),
                    style = MaterialTheme.typography.displaySmall,
                    color = accent,
                )
                if (account.iban.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = account.iban,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun TypeBadge(account: Account) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            text = account.accountType.ifBlank { "ACCOUNT" }.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun TotalFooter(count: Int, total: Double, currency: String) {
    Column {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Total across $count accounts",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatMoney(total, currency),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun NoMatchRow() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No accounts match this filter.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun EmptyState(onRequestNewAccount: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.AccountBalanceWallet,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No accounts found",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "You don't have any accounts yet. Request a new account to get started.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onRequestNewAccount) {
            Text("Request Account")
        }
    }
}

@Composable
private fun ErrorState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Could not load accounts",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "We were unable to fetch your account list. Please check your connection and try again.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun accentColor(account: Account): Color = when {
    account.accountType.contains("savings", ignoreCase = true) ->
        MaterialTheme.colorScheme.secondary
    account.accountType.contains("business", ignoreCase = true) -> BusinessAccent
    else -> MaterialTheme.colorScheme.primary
}

/** Formats an OBP money string ("4250.00", "GBP") for display, falling back gracefully. */
private fun formatMoney(amount: String, currency: String): String {
    val parsed = amount.toDoubleOrNull() ?: return listOf(currency, amount).joinToString(" ").trim()
    return formatMoney(parsed, currency)
}

private fun formatMoney(amount: Double, currency: String): String {
    val cents = kotlin.math.round(amount * 100).toLong()
    val whole = cents / 100
    val frac = (if (cents < 0) -cents else cents) % 100
    val number = "$whole.${frac.toString().padStart(2, '0')}"
    return if (currency.isBlank()) number else "$currency $number"
}
