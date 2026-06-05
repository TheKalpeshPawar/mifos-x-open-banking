/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.feature.home.ui.HomeContent
import org.mifosx.openbanking.feature.home.ui.HomeViewModel
import org.mifosx.openbanking.feature.home.ui.formatDashboardDate
import org.mifosx.openbanking.feature.home.ui.formatMoney
import org.mifosx.openbanking.feature.home.ui.initials
import org.mifosx.openbanking.feature.home.ui.timeOfDayGreeting
import template.core.base.store.screen.ScreenState
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Home Dashboard — Consumer landing tab. Stateful container binding [HomeViewModel]'s
 * offline-first [ScreenState] to the dashboard body, styled to the rendered design preview
 * (`idea-layer/screens/home/preview/content.html`). All colors come from `MaterialTheme.colorScheme`
 * roles (light/dark green scheme), never hardcoded hex — so light and dark both match by construction.
 * Hosted inside the authenticated navbar scaffold; the host owns the bottom nav.
 */
@Composable
fun HomeScreen(
    onTransfer: () -> Unit,
    onAccounts: () -> Unit,
    onStandingOrders: () -> Unit,
    onViewCards: () -> Unit,
    onFindAtm: () -> Unit,
    onBeneficiaries: () -> Unit,
    onDeferred: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val greeting = rememberGreeting()

    Box(modifier = modifier.fillMaxSize()) {
        when (val s = state) {
            is ScreenState.Loading -> HomeLoading()

            is ScreenState.Empty -> HomeEmpty(onSetUpAccount = onAccounts)

            is ScreenState.Error -> HomeError(onRetry = viewModel::onRetry)
            is ScreenState.NoNetwork -> HomeError(onRetry = viewModel::onRetry)
            is ScreenState.Unauthenticated -> HomeError(onRetry = viewModel::onRetry)

            is ScreenState.Content -> HomeLoaded(
                content = s.data,
                greeting = greeting,
                onTransfer = onTransfer,
                onStandingOrders = onStandingOrders,
                onViewCards = onViewCards,
                onFindAtm = onFindAtm,
                onBeneficiaries = onBeneficiaries,
                onDeferred = onDeferred,
            )
        }
    }
}

private data class Greeting(val line: String, val date: String)

@OptIn(ExperimentalTime::class)
@Composable
private fun rememberGreeting(): Greeting = remember {
    val ldt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    Greeting(
        line = timeOfDayGreeting(ldt.hour),
        date = formatDashboardDate(ldt.dayOfWeek.isoDayNumber, ldt.day, ldt.month.number, ldt.year),
    )
}

@Composable
private fun HomeLoaded(
    content: HomeContent,
    greeting: Greeting,
    onTransfer: () -> Unit,
    onStandingOrders: () -> Unit,
    onViewCards: () -> Unit,
    onFindAtm: () -> Unit,
    onBeneficiaries: () -> Unit,
    onDeferred: (String) -> Unit,
) {
    val name = content.greetingName.ifBlank { "there" }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text(
                    text = "${greeting.line}, $name",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = greeting.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            PrimaryAccountCard(
                account = content.primaryAccount,
                ownerName = name,
                onTransfer = onTransfer,
                onBeneficiaries = onBeneficiaries,
                onDeferred = onDeferred,
            )
        }
        item { SectionHeader("Banking Services") }
        item {
            BankingServices(
                onStandingOrders = onStandingOrders,
                onTransfers = onTransfer,
                onViewCards = onViewCards,
                onFindAtm = onFindAtm,
                onDeferred = onDeferred,
            )
        }
    }
}

@Composable
private fun PrimaryAccountCard(
    account: Account?,
    ownerName: String,
    onTransfer: () -> Unit,
    onBeneficiaries: () -> Unit,
    onDeferred: (String) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account?.label?.ifBlank { "Account" } ?: "Account",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = account?.displayIdentifier?.ifBlank { "—" } ?: "—",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                Avatar(initials = initials(ownerName))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Available Balance",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatMoney(account?.balance?.amount.orEmpty(), account?.balance?.currency.orEmpty()),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickChip("Transfer", Modifier.weight(1f), onClick = onTransfer)
                QuickChip("Pay Bill", Modifier.weight(1f)) { onDeferred("Pay Bill — coming soon") }
                QuickChip("Top-up", Modifier.weight(1f)) { onDeferred("Top-up — coming soon") }
                QuickChip("Payees", Modifier.weight(1f), onClick = onBeneficiaries)
            }
        }
    }
}

@Composable
private fun Avatar(initials: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun QuickChip(label: String, modifier: Modifier = Modifier, muted: Boolean = false, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.height(32.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                textAlign = TextAlign.Center,
                color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun BankingServices(
    onStandingOrders: () -> Unit,
    onTransfers: () -> Unit,
    onViewCards: () -> Unit,
    onFindAtm: () -> Unit,
    onDeferred: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            ServiceTile("Standing Orders", Icons.Filled.EventRepeat, Modifier.weight(1f), onStandingOrders)
            ServiceTile("Transfers", Icons.Filled.SyncAlt, Modifier.weight(1f), onTransfers)
            ServiceTile("Statements", Icons.Filled.Description, Modifier.weight(1f)) { onDeferred("Statements — coming soon") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            ServiceTile("Cards", Icons.Filled.CreditCard, Modifier.weight(1f), onViewCards)
            ServiceTile("Find ATM", Icons.Filled.LocationOn, Modifier.weight(1f), onFindAtm)
            ServiceTile("Support", Icons.Filled.HeadsetMic, Modifier.weight(1f)) { onDeferred("Support — coming soon") }
        }
    }
}

@Composable
private fun ServiceTile(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .height(88.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HomeLoading() {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun HomeEmpty(onSetUpAccount: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No accounts yet", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Text(
            "Your accounts will appear here once your profile is set up.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onSetUpAccount) {
            Text("Set Up Account", color = MaterialTheme.colorScheme.primary)
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun HomeError(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text("Could not load your account", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))
        Text(
            "We were unable to fetch your account data. Check your connection and try again.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onRetry) {
            Text("Retry", color = MaterialTheme.colorScheme.primary)
        }
    }
}
