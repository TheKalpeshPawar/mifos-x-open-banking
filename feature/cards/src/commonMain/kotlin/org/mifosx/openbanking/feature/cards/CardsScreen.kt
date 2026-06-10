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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CreditCardOff
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.mifosx.openbanking.core.model.obp.Card
import org.mifosx.openbanking.feature.cards.ui.CardsContent
import org.mifosx.openbanking.feature.cards.ui.CardsViewModel
import template.core.base.store.screen.ScreenState

/**
 * My Cards screen — Consumer cards hub. Stateful container binding [CardsViewModel]'s
 * offline-first [ScreenState] to the stateless content. Hosted inside the authenticated
 * navbar scaffold (bottom nav supplied by the host); owns its body, the card carousel, and a
 * details card describing the currently-selected card.
 *
 * Ordering a new card is deferred (see [CardsViewModel] for the OBP capability rationale): the
 * empty-state CTA surfaces a "coming soon" snackbar.
 */
@Composable
fun CardsScreen(
    onCardClick: (Card) -> Unit,
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
                onCardSelected = viewModel::selectCard,
            )
        }
    }
}

@Composable
private fun CardsLoaded(
    content: CardsContent,
    onCardClick: (Card) -> Unit,
    onCardSelected: (Card) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item { Title() }
        item { CardCarousel(cards = content.cards, onCardClick = onCardClick, onCardSelected = onCardSelected) }
        item { Spacer(Modifier.height(16.dp)) }
        item {
            val card = content.cards.firstOrNull { it.bankCardNumber == content.selectedCardId }
                ?: content.cards.first()
            CardDetailsCard(card = card)
        }
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
private fun CardCarousel(
    cards: List<Card>,
    onCardClick: (Card) -> Unit,
    onCardSelected: (Card) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { cards.size })
    // Swiping to a different card scopes the details card to that card.
    LaunchedEffect(pagerState, cards) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            cards.getOrNull(page)?.let(onCardSelected)
        }
    }
    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        pageSpacing = 16.dp,
        modifier = Modifier.fillMaxWidth(),
    ) { page ->
        cards[page].let { card -> PaymentCard(card = card, onClick = { onCardClick(card) }) }
    }
}

private val CardShape = RoundedCornerShape(20.dp)

/**
 * Card face gradient + content color derived from the theme so the carousel adapts to
 * dark/light: Visa keys off `primary`, Mastercard off `secondary`. Three stops are built
 * by darkening the base toward black, and text uses the matching `on*` role for contrast.
 */
private data class CardPalette(val gradient: List<Color>, val onColor: Color)

@Composable
private fun cardPalette(network: String): CardPalette {
    val scheme = MaterialTheme.colorScheme
    val base = if (network.equals("MASTERCARD", ignoreCase = true)) scheme.secondary else scheme.primary
    val onColor = if (network.equals("MASTERCARD", ignoreCase = true)) scheme.onSecondary else scheme.onPrimary
    return CardPalette(
        gradient = listOf(
            lerp(base, Color.Black, 0.12f),
            base,
            lerp(base, Color.Black, 0.38f),
        ),
        onColor = onColor,
    )
}

@Composable
private fun PaymentCard(card: Card, onClick: () -> Unit) {
    val isActive = card.enabled && !card.cancelled
    val palette = cardPalette(card.network)
    Surface(
        onClick = onClick,
        shape = CardShape,
        color = Color.Transparent,
        shadowElevation = 8.dp,
        modifier = Modifier
            .shadow(elevation = 10.dp, shape = CardShape, clip = false)
            .fillMaxWidth()
            .height(200.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(palette.gradient), CardShape)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatusChip(active = isActive)
                Text(
                    text = card.network.ifBlank { "CARD" }.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.onColor,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column {
                Text(
                    text = maskedNumber(card.bankCardNumber),
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.onColor,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = card.nameOnCard.uppercase(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.onColor.copy(alpha = 0.85f),
                )
            }
        }
    }
}

private val ChipActiveBg = Color(0xFF4C662B).copy(alpha = 0.85f)
private val ChipFrozenBg = Color(0xFFC5C8BA).copy(alpha = 0.3f)
private val ChipFrozenText = Color(0xFFC5C8BA)

@Composable
private fun StatusChip(active: Boolean) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (active) ChipActiveBg else ChipFrozenBg,
    ) {
        Text(
            text = if (active) "Active" else "Frozen",
            style = MaterialTheme.typography.labelSmall,
            color = if (active) Color.White else ChipFrozenText,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/**
 * Details for the currently-selected card — a label/value list with no dividers between rows
 * (per design). Sourced entirely from the OBP [Card] model; blank fields render as an em dash.
 */
@Composable
private fun CardDetailsCard(card: Card) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Card Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            CardDetailRow("Card Holder", card.nameOnCard.ifBlank { "—" })
            CardDetailRow("Card Number", maskedNumber(card.bankCardNumber))
            CardDetailRow("Expires", card.expiresDate.ifBlank { "—" })
            CardDetailRow("Card Type", card.cardType.ifBlank { "—" })
            CardDetailRow("Network", card.network.ifBlank { "—" })
            CardDetailRow("Status", cardStatusLabel(card))
            CardDetailRow("Linked Account", card.account.label.ifBlank { "—" })
        }
    }
}

@Composable
private fun CardDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Human-readable card status from the OBP flags, richer than the carousel's Active/Frozen chip. */
private fun cardStatusLabel(card: Card): String = when {
    card.cancelled -> "Cancelled"
    card.onHotList -> "Reported lost"
    card.enabled -> "Active"
    else -> "Frozen"
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
