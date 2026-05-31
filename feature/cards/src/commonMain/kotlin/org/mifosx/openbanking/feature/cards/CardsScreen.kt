/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.cards

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CreditCardOff
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.model.obp.Card
import org.mifosx.openbanking.core.model.obp.Transaction
import org.mifosx.openbanking.feature.cards.ui.CardsContent
import org.mifosx.openbanking.feature.cards.ui.CardsViewModel
import template.core.base.store.screen.ScreenState

/**
 * My Cards screen — Consumer cards hub. Stateful container binding [CardsViewModel]'s
 * offline-first [ScreenState] to the stateless content. Hosted inside the authenticated
 * navbar scaffold (bottom nav supplied by the host); owns its body, the card carousel,
 * quick-action controls, recent card transactions, and the Order New Card affordance.
 *
 * Deferred controls (Freeze / Set Limit / View PIN / Report Lost / Order New Card) surface
 * a "coming soon" snackbar — see [CardsViewModel] for the OBP capability rationale.
 */
@Composable
fun CardsScreen(
    onCardClick: (Card) -> Unit,
    onTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CardsViewModel = koinViewModel(),
    onDeferred: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val deferred: (String) -> Unit = remember(viewModel) {
        { control ->
            viewModel.onDeferredAction(control)
            onDeferred("$control is coming soon")
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val s = state) {
            is ScreenState.Loading -> LoadingState()
            is ScreenState.Empty -> EmptyState(onOrderCard = { deferred("Order New Card") })
            is ScreenState.Error -> ErrorState(onRetry = viewModel::onRetry)
            is ScreenState.NoNetwork -> ErrorState(onRetry = viewModel::onRetry)
            is ScreenState.Unauthenticated -> ErrorState(onRetry = viewModel::onRetry)
            is ScreenState.Content -> CardsLoaded(
                content = s.data,
                onCardClick = onCardClick,
                onTransactionClick = onTransactionClick,
                onDeferred = deferred,
            )
        }
    }
}

@Composable
private fun CardsLoaded(
    content: CardsContent,
    onCardClick: (Card) -> Unit,
    onTransactionClick: (Transaction) -> Unit,
    onDeferred: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item { Title() }
        item { CardCarousel(cards = content.cards, onCardClick = onCardClick) }
        item { QuickActions(onDeferred = onDeferred) }
        if (content.recentTransactions.isNotEmpty()) {
            item { SectionHeader("Card Transactions") }
            items(content.recentTransactions, key = { it.id }) { tx ->
                TransactionRow(tx = tx, onClick = { onTransactionClick(tx) })
            }
        }
        item { OrderNewCardButton(onClick = { onDeferred("Order New Card") }) }
    }
}

@Composable
private fun Title() {
    Text(
        text = "My Cards",
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 20.dp).padding(top = 24.dp, bottom = 4.dp),
    )
}

@Composable
private fun CardCarousel(cards: List<Card>, onCardClick: (Card) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(cards, key = { it.bankCardNumber }) { card -> PaymentCard(card = card, onClick = { onCardClick(card) }) }
    }
}

@Composable
private fun PaymentCard(card: Card, onClick: () -> Unit) {
    val isActive = card.enabled && !card.cancelled
    val gradient = if (card.network.equals("MASTERCARD", ignoreCase = true)) {
        Brush.linearGradient(listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.secondaryContainer))
    } else {
        Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer))
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        modifier = Modifier.width(320.dp).height(200.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().background(gradient, RoundedCornerShape(20.dp)).padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatusChip(active = isActive)
                Text(
                    text = card.network.ifBlank { "CARD" }.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column {
                Text(
                    text = maskedNumber(card.bankCardNumber),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = card.nameOnCard.uppercase(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun StatusChip(active: Boolean) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
    ) {
        Text(
            text = if (active) "Active" else "Frozen",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun QuickActions(onDeferred: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        QuickAction(Icons.Filled.AcUnit, "Freeze", MaterialTheme.colorScheme.primary) { onDeferred("Freeze") }
        QuickAction(Icons.Filled.Tune, "Set Limit", MaterialTheme.colorScheme.primary) { onDeferred("Set Limit") }
        QuickAction(Icons.Filled.Password, "View PIN", MaterialTheme.colorScheme.primary) { onDeferred("View PIN") }
        QuickAction(Icons.Filled.ReportProblem, "Report Lost", MaterialTheme.colorScheme.error) {
            onDeferred("Report Lost")
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, tint = tint)
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 20.dp).padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun TransactionRow(tx: Transaction, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transactionLabel(tx),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = tx.details.posted.take(10),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = formatAmount(tx.details.value.amount, tx.details.value.currency),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun OrderNewCardButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Icon(Icons.Filled.AddCard, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Order New Card")
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(
            Icons.Filled.CreditCard,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
        )
    }
}

@Composable
private fun EmptyState(onOrderCard: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.CreditCardOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No cards yet",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Order your first Mifos card to start making payments.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onOrderCard) { Text("Order New Card") }
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
            "Unable to load cards",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Check your connection and try again. Your cards are safe.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onRetry) { Text("Retry") }
    }
}

private fun transactionLabel(tx: Transaction): String =
    tx.otherAccount.holder.name.ifBlank { tx.details.description.ifBlank { "Transaction" } }

/** Masks all but the last four digits of an OBP card number for display. */
private fun maskedNumber(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    val last4 = digits.takeLast(4)
    return if (last4.isBlank()) raw else "•••• •••• •••• $last4"
}

private fun formatAmount(amount: String, currency: String): String {
    val value = amount.toDoubleOrNull()
    val prefix = if (value != null && value < 0) "−" else ""
    val abs = value?.let { if (it < 0) -it else it }
    val number = abs?.let { it.toString() } ?: amount.removePrefix("-")
    return "$prefix${currency.ifBlank { "" }} $number".trim()
}
