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

package org.mifosx.openbanking.feature.beneficiaries

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.feature.beneficiaries.ui.BeneficiariesContent
import org.mifosx.openbanking.feature.beneficiaries.ui.BeneficiariesViewModel
import org.mifosx.openbanking.feature.beneficiaries.ui.BeneficiaryRow
import template.core.base.store.screen.ScreenState

/**
 * Beneficiaries screen — counterparty (payee) hub. Pushed (non-tab) destination inside the
 * authenticated navbar, so it owns its own top app bar (transparent — sits on the theme
 * background). Binds [BeneficiariesViewModel]'s offline-derived [ScreenState] to stateless
 * content; tapping a beneficiary switches to the Pay tab.
 */
@Composable
fun BeneficiariesScreen(
    onBeneficiaryClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    accountId: String = "",
    viewModel: BeneficiariesViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()

    LaunchedEffect(accountId) {
        if (accountId.isNotBlank()) viewModel.onAccountSelected(accountId)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Beneficiaries", fontWeight = FontWeight.SemiBold) },
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
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is ScreenState.Loading -> CenteredProgress()
                is ScreenState.Empty -> EmptyState(
                    query = query,
                    onQueryChange = viewModel::onSearchQueryChanged,
                )
                is ScreenState.Error -> ErrorState(onRetry = viewModel::onRetry)
                is ScreenState.NoNetwork -> ErrorState(onRetry = viewModel::onRetry)
                is ScreenState.Unauthenticated -> ErrorState(onRetry = viewModel::onRetry)
                is ScreenState.Content -> Loaded(
                    content = s.data,
                    query = query,
                    onQueryChange = viewModel::onSearchQueryChanged,
                    onSort = viewModel::toggleSort,
                    onAccountSelected = viewModel::onAccountSelected,
                    onBeneficiaryClick = onBeneficiaryClick,
                )
            }
        }
    }
}

@Composable
private fun Loaded(
    content: BeneficiariesContent,
    query: String,
    onQueryChange: (String) -> Unit,
    onSort: () -> Unit,
    onAccountSelected: (String) -> Unit,
    onBeneficiaryClick: (String) -> Unit,
) {
    val showRecent = content.recentlyUsed.isNotEmpty() && query.isBlank()
    Column(modifier = Modifier.fillMaxSize()) {
        AccountSelector(
            accounts = content.accounts,
            selectedAccountId = content.selectedAccountId,
            onAccountSelected = onAccountSelected,
        )
        SearchBar(query = query, onQueryChange = onQueryChange)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        ) {
            if (showRecent) {
                item { SectionHeader("Recently Used", BeneficiariesTestTags.RECENTLY_USED_HEADER) }
                itemsIndexed(content.recentlyUsed, key = { _, r -> "recent-${r.id}" }) { index, row ->
                    RecentCard(row = row, index = index, onClick = { onBeneficiaryClick(row.id) })
                }
                item {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            }
            item { AllBeneficiariesHeader(onSort = onSort) }
            if (content.all.isEmpty()) {
                // Distinguish "this account has no payees" from "search matched nothing".
                item { if (query.isBlank()) NoPayeesRow() else NoMatchRow() }
            } else {
                items(content.all, key = { it.id }) { row ->
                    AllCard(row = row, onClick = { onBeneficiaryClick(row.id) })
                }
            }
        }
    }
}

/**
 * From-account selector. Beneficiaries are per-account, so this scopes the whole screen: the list
 * below + the Add target are the selected account's. Single account → a read-only field; multiple
 * → a tappable field with a dropdown.
 */
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
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Card(
            onClick = { if (accounts.size > 1) expanded = true },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .testTag(BeneficiariesTestTags.ACCOUNT_SELECTOR),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.AccountBalance,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Beneficiaries of",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = selected.label.ifBlank { selected.accountIdOrId },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (accounts.size > 1) {
                    Icon(
                        Icons.Filled.ArrowDropDown,
                        contentDescription = "Choose account",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = { Text(account.label.ifBlank { account.accountIdOrId }) },
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
private fun NoPayeesRow() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No beneficiaries on this account yet. Switch accounts above.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(BeneficiariesTestTags.SEARCH_BAR),
        placeholder = {
            Text(
                text = "Search beneficiaries by name or IBAN...",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
    )
}

@Composable
private fun SectionHeader(text: String, tag: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .padding(top = 16.dp, bottom = 8.dp)
            .testTag(tag)
            .semantics { heading() },
    )
}

@Composable
private fun AllBeneficiariesHeader(onSort: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "All Beneficiaries",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .testTag(BeneficiariesTestTags.ALL_HEADER)
                .semantics { heading() },
        )
        IconButton(
            onClick = onSort,
            modifier = Modifier.testTag(BeneficiariesTestTags.SORT_BUTTON),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = "Sort beneficiaries",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** Bordered surface-container card at level1 elevation (matches the accounts cards). */
@Composable
private fun BeneficiaryCard(tag: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .testTag(tag),
    ) {
        content()
    }
}

@Composable
private fun RecentCard(row: BeneficiaryRow, index: Int, onClick: () -> Unit) {
    val avatarBg =
        if (index % 2 == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    BeneficiaryCard(tag = BeneficiariesTestTags.beneficiaryRow(row.id), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleAvatar(initials = row.initials, background = avatarBg)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val bank = row.bankName.ifBlank { row.accountIdentifier }
                if (bank.isNotBlank()) {
                    Text(
                        text = bank,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                row.lastPayment?.let {
                    Text(
                        text = "${it.amount} · ${it.relativeDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            Chevron()
        }
    }
}

@Composable
private fun AllCard(row: BeneficiaryRow, onClick: () -> Unit) {
    BeneficiaryCard(tag = BeneficiariesTestTags.beneficiaryRow(row.id), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SquareAvatar(initials = row.initials)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (row.accountIdentifier.isNotBlank()) {
                    Text(
                        text = row.accountIdentifier,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                row.lastPayment?.let {
                    Text(
                        text = "Last: ${it.date}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Chevron()
        }
    }
}

@Composable
private fun CircleAvatar(initials: String, background: Color) {
    Box(
        modifier = Modifier.size(44.dp).background(background, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun SquareAvatar(initials: String) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun Chevron() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun CenteredProgress() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun NoMatchRow() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No beneficiaries match your search.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EmptyState(query: String, onQueryChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(query = query, onQueryChange = onQueryChange)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .testTag(BeneficiariesTestTags.EMPTY_STATE),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PersonOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "No beneficiaries yet",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "People you pay will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ErrorState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag(BeneficiariesTestTags.ERROR_STATE),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Unable to load beneficiaries",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Check your connection and try again",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onRetry) { Text("Retry") }
    }
}
