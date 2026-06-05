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
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import org.mifosx.openbanking.feature.accounts.ui.AccountsContent
import org.mifosx.openbanking.feature.accounts.ui.AccountsViewModel
import template.core.base.store.screen.ScreenState

/** Decorative amber accent for Business accounts (SPEC: decorative left-border only). */
private val BusinessAccent = Color(0xFFE8A317)

/**
 * My Accounts screen — Consumer accounts tab. Stateful container binding [AccountsViewModel]'s
 * offline-first [ScreenState] to the stateless content. Hosted inside the authenticated navbar
 * scaffold (bottom nav supplied by the host), so this screen owns only its body.
 */
@Composable
fun AccountsScreen(
    onAccountClick: (bankId: String, accountId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        when (val s = state) {
            is ScreenState.Loading -> LoadingState()

            is ScreenState.Empty -> EmptyState()

            is ScreenState.Error ->
                ErrorState(onRetry = viewModel::onRetry)

            is ScreenState.NoNetwork ->
                ErrorState(onRetry = viewModel::onRetry)

            is ScreenState.Unauthenticated ->
                ErrorState(onRetry = viewModel::onRetry)

            is ScreenState.Content -> AccountsLoaded(
                content = s.data,
                query = searchQuery,
                onQueryChange = viewModel::onSearchQueryChanged,
                onAccountClick = onAccountClick,
            )
        }
    }
}

@Composable
private fun AccountsLoaded(
    content: AccountsContent,
    query: String,
    onQueryChange: (String) -> Unit,
    onAccountClick: (bankId: String, accountId: String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header + search bar are hoisted OUT of the LazyColumn: a TextField inside a
        // LazyColumn item loses IME focus every time the list recomposes on query change,
        // which drops keystrokes. Keeping them in a stable parent Column fixes that.
        Header()
        SearchBar(query = query, onQueryChange = onQueryChange)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            if (content.filteredAccounts.isEmpty()) {
                item { NoMatchRow() }
            } else {
                // Group accounts by their owning bank. No balance totals are shown anywhere —
                // only each account's own balance, on its card.
                val groups = content.filteredAccounts.groupBy { it.bankId }
                groups.forEach { (bankId, accounts) ->
                    item(key = "bank-$bankId") {
                        BankSectionHeader(bankId = bankId, count = accounts.size)
                    }
                    items(accounts, key = { it.id }) { account ->
                        AccountCard(
                            account = account,
                            onClick = { onAccountClick(account.bankId, account.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BankSectionHeader(bankId: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.AccountBalance,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = bankName(bankId),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (count == 1) "1 account" else "$count accounts",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Best-effort human label for an OBP bank id ("ac.bank.uk" -> "Ac Bank Uk"). */
private fun bankName(bankId: String): String =
    bankId.ifBlank { "Bank" }
        .split('.', '-', '_')
        .filter { it.isNotBlank() }
        .joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }

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
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        placeholder = {
            Text(
                text = "Search by name, number or label",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

@Composable
private fun AccountCard(
    account: Account,
    onClick: () -> Unit,
) {
    val accent = accentColor(account)
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp),
            ),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accent),
            )
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = account.label.ifBlank { "Account" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(8.dp))
                    TypeBadge(account = account)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = formatMoney(account.balance.amount, account.balance.currency),
                    style = MaterialTheme.typography.displaySmall,
                    color = balanceColor(account),
                )
                val identifier = account.displayIdentifier
                if (identifier.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AccountBox,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = identifier,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeBadge(account: Account) {
    val isSavings = account.typeOrProduct.contains("savings", ignoreCase = true)
    val isBusiness = account.typeOrProduct.contains("business", ignoreCase = true)
    val container = when {
        isSavings -> MaterialTheme.colorScheme.secondaryContainer
        isBusiness -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val onContainer = when {
        isSavings -> MaterialTheme.colorScheme.onSecondaryContainer
        isBusiness -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = container,
    ) {
        Text(
            text = account.typeOrProduct.ifBlank { "ACCOUNT" }.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = onContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
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
            text = "No accounts match your search.",
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
private fun EmptyState() {
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
            text = "Your accounts will appear here once your profile is set up.",
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

/** Decorative left-border accent: checking=primary, savings=secondary, business=amber. */
@Composable
private fun accentColor(account: Account): Color = when {
    account.typeOrProduct.contains("savings", ignoreCase = true) ->
        MaterialTheme.colorScheme.secondary
    account.typeOrProduct.contains("business", ignoreCase = true) -> BusinessAccent
    else -> MaterialTheme.colorScheme.primary
}

/** Balance text color: checking=primary, savings=secondary, business=onSurface (per preview). */
@Composable
private fun balanceColor(account: Account): Color = when {
    account.typeOrProduct.contains("savings", ignoreCase = true) ->
        MaterialTheme.colorScheme.secondary
    account.typeOrProduct.contains("business", ignoreCase = true) ->
        MaterialTheme.colorScheme.onSurface
    else -> MaterialTheme.colorScheme.primary
}

/** Formats an OBP money string ("4250.00", "GBP") for display, falling back gracefully. */
private fun formatMoney(amount: String, currency: String): String {
    val parsed = amount.toDoubleOrNull() ?: return listOf(currency, amount).joinToString(" ").trim()
    return formatMoney(parsed, currency)
}

private fun formatMoney(amount: Double, currency: String): String {
    val cents = kotlin.math.round(amount * 100).toLong()
    val negative = cents < 0
    val absCents = if (negative) -cents else cents
    val whole = absCents / 100
    val frac = absCents % 100
    val grouped = groupThousands(whole)
    val code = if (currency.isBlank()) "" else " $currency"
    val sign = if (negative) "-" else ""
    return "$sign$grouped.${frac.toString().padStart(2, '0')}$code"
}

/** Inserts thousands separators into a non-negative whole-number string. */
private fun groupThousands(value: Long): String {
    val digits = value.toString()
    if (digits.length <= 3) return digits
    val sb = StringBuilder()
    val firstGroup = digits.length % 3
    if (firstGroup > 0) {
        sb.append(digits, 0, firstGroup)
        if (digits.length > firstGroup) sb.append(',')
    }
    var i = firstGroup
    while (i < digits.length) {
        sb.append(digits, i, i + 3)
        if (i + 3 < digits.length) sb.append(',')
        i += 3
    }
    return sb.toString()
}
