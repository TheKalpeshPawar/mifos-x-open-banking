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

package org.mifosx.openbanking.feature.sendmoney

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.core.model.obp.ObpException
import org.mifosx.openbanking.feature.sendmoney.ui.HubRecipient
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyHubContent
import org.mifosx.openbanking.feature.sendmoney.ui.SendMoneyHubViewModel
import template.core.base.store.screen.ScreenState

/**
 * Send Money hub — the Pay tab landing. Lets the user pick a recent recipient or any
 * beneficiary (→ amount entry preselected), or start a new transfer via the "New" avatar
 * (→ amount entry with the beneficiary picker).
 */
@Composable
fun SendMoneyHubScreen(
    onRecipientSelected: (accountId: String, counterpartyId: String) -> Unit,
    onNewTransfer: (accountId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SendMoneyHubViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Send money", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val selectedAccountId = ((state as? ScreenState.Content)?.data?.selectedAccountId).orEmpty()
                    IconButton(onClick = { onNewTransfer(selectedAccountId) }) {
                        Icon(Icons.Filled.PersonSearch, contentDescription = "New recipient")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is ScreenState.Loading -> Centered {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                is ScreenState.Empty -> EmptyHub(onNewTransfer = { onNewTransfer("") })
                is ScreenState.Error -> ErrorHub(
                    title = errorTitle(s.error),
                    body = errorBody(s.error),
                    onRetry = viewModel::onRetry,
                )
                is ScreenState.NoNetwork -> ErrorHub(
                    title = "You're offline",
                    body = "We can't reach your bank right now. Check your connection and try again.",
                    onRetry = viewModel::onRetry,
                )
                is ScreenState.Unauthenticated -> ErrorHub(
                    title = "Session expired",
                    body = "Your sign-in has expired. Sign in again to load your recipients.",
                    onRetry = viewModel::onRetry,
                )
                is ScreenState.Content -> HubContent(
                    content = s.data,
                    onAccountSelected = viewModel::onAccountSelected,
                    onRecipientSelected = onRecipientSelected,
                    onNewTransfer = onNewTransfer,
                )
            }
        }
    }
}

@Composable
private fun HubContent(
    content: SendMoneyHubContent,
    onAccountSelected: (String) -> Unit,
    onRecipientSelected: (accountId: String, counterpartyId: String) -> Unit,
    onNewTransfer: (accountId: String) -> Unit,
) {
    val accountId = content.selectedAccountId
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { SectionLabel("FROM ACCOUNT") }
        item {
            AccountSelector(
                accounts = content.accounts,
                selectedAccountId = accountId,
                onAccountSelected = onAccountSelected,
            )
        }

        item { Spacer(Modifier.height(8.dp)) }
        item { SectionLabel("RECENT RECIPIENTS") }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                NewRecipientAvatar(onClick = { onNewTransfer(accountId) })
                content.recentRecipients.forEach { r ->
                    RecipientAvatar(recipient = r, onClick = { onRecipientSelected(accountId, r.counterpartyId) })
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
        item { SectionLabel("ALL BENEFICIARIES") }
        if (content.allBeneficiaries.isEmpty()) {
            item { NoBeneficiariesNote() }
        } else {
            items(content.allBeneficiaries, key = { it.counterpartyId }) { r ->
                BeneficiaryListRow(recipient = r, onClick = { onRecipientSelected(accountId, r.counterpartyId) })
            }
        }
    }
}

@Composable
private fun AccountSelector(
    accounts: List<Account>,
    selectedAccountId: String,
    onAccountSelected: (String) -> Unit,
) {
    val selected = accounts.firstOrNull { it.accountIdOrId == selectedAccountId }
        ?: accounts.firstOrNull()
        ?: return
    if (accounts.size <= 1) {
        AccountField(label = accountLabel(selected))
        return
    }
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
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
                        "From account",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        accountLabel(selected),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = "Choose account",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = { Text(accountLabel(account)) },
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
private fun AccountField(label: String) {
    OutlinedTextField(
        value = label,
        onValueChange = {},
        readOnly = true,
        enabled = false,
        label = { Text("From account") },
        leadingIcon = { Icon(Icons.Filled.AccountBalance, contentDescription = null) },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun accountLabel(account: Account): String {
    val label = account.label.ifBlank { account.accountIdOrId }
    val bal = account.balance
    return if (bal.currency.isBlank()) label else "$label — ${currencySymbol(bal.currency)}${bal.amount}"
}

@Composable
private fun NoBeneficiariesNote() {
    Text(
        "No payees on this account yet. Switch accounts above.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun NewRecipientAvatar(onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "New recipient",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "New",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

@Composable
private fun RecipientAvatar(recipient: HubRecipient, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(MaterialTheme.colorScheme.secondary, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = recipient.initials,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondary,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            recipient.shortName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BeneficiaryListRow(recipient: HubRecipient, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.secondary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = recipient.initials,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondary,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    recipient.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    recipient.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun EmptyHub(onNewTransfer: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "No recipients yet",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "People you pay will appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onNewTransfer) { Text("New transfer") }
    }
}

@Composable
private fun ErrorHub(title: String, body: String, onRetry: () -> Unit) {
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
            title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onRetry) { Text("Try again") }
    }
}

/** Short headline for a load failure, derived from the OBP error reason. */
private fun errorTitle(error: Throwable): String = when ((error as? ObpException)?.reason) {
    "UNAUTHORIZED" -> "Session expired"
    "REQUEST_TIMEOUT" -> "Request timed out"
    "TOO_MANY_REQUESTS" -> "Too many requests"
    "SERVER" -> "Bank service unavailable"
    else -> "Couldn't load recipients"
}

/** Actionable detail for a load failure, derived from the OBP error reason. */
private fun errorBody(error: Throwable): String = when ((error as? ObpException)?.reason) {
    "UNAUTHORIZED" -> "Your sign-in has expired. Sign in again to continue."
    "REQUEST_TIMEOUT" -> "The bank took too long to respond. Check your connection and try again."
    "TOO_MANY_REQUESTS" -> "You've made too many requests. Wait a moment and try again."
    "SERVER" -> "Your bank's service is temporarily unavailable. Try again shortly."
    "SERIALIZATION" -> "We received an unexpected response from your bank. Try again."
    else -> "We couldn't load this account's recipients. Try again, or switch to another account."
}
