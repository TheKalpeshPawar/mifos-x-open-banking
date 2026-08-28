/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.common.AccountScheme
import org.mifosx.openbanking.core.common.formatMoney
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import org.mifosx.openbanking.core.model.banking.AccountBalance
import org.mifosx.openbanking.core.model.banking.AccountWithBalance
import org.mifosx.openbanking.core.model.banking.BankAccount
import org.mifosx.openbanking.core.ui.account.accountTypeLabel
import org.mifosx.openbanking.core.ui.components.MifosSectionHeading
import org.mifosx.openbanking.feature.home.HomeTestTags
import org.mifosx.openbanking.feature.home.generated.resources.Res
import org.mifosx.openbanking.feature.home.generated.resources.feature_home_cards
import template.core.base.designsystem.theme.KptTheme

/** Fraction of the row width one card face occupies. */
private const val CARD_FACE_WIDTH_FRACTION = 0.85f

/**
 * The customer's card accounts, as a horizontally scrollable row of card faces. Renders nothing at all
 * — heading included — when there is no card account.
 *
 * @param cards The card accounts.
 * @param onCardClick Invoked with the account id when a card face is tapped.
 */
@Composable
internal fun HomeCardsSection(
    cards: List<AccountWithBalance>,
    onCardClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (cards.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth().testTag(HomeTestTags.CARDS_SECTION),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
    ) {
        MifosSectionHeading(
            text = stringResource(Res.string.feature_home_cards),
            style = KptTheme.typography.titleLarge,
            color = KptTheme.colorScheme.onSurface,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.md)) {
            items(cards, key = { it.account.accountId }) { card ->
                CardFace(
                    card = card,
                    onClick = { onCardClick(card.account.accountId) },
                    modifier = Modifier.fillParentMaxWidth(CARD_FACE_WIDTH_FRACTION),
                )
            }
        }
    }
}

@Composable
private fun CardFace(
    card: AccountWithBalance,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(DesignToken.sizes.cardFace)
            .clip(KptTheme.shapes.large)
            .background(
                Brush.linearGradient(
                    listOf(KptTheme.colorScheme.primary, KptTheme.colorScheme.primaryContainer),
                ),
            )
            .clickable(onClick = onClick)
            .padding(KptTheme.spacing.md)
            .testTag(HomeTestTags.cardFace(card.account.accountId)),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = accountTypeLabel(card.account.accountTypeCode, card.account.description),
            style = KptTheme.typography.titleMedium,
            color = KptTheme.colorScheme.onPrimary,
        )
        Text(
            text = card.balance?.let { formatMoney(it.availableAmount, it.currency) }.orEmpty(),
            style = KptTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = KptTheme.colorScheme.onPrimary,
        )
        Column {
            if (card.account.accountHolderName.isNotBlank()) {
                Text(
                    text = card.account.accountHolderName,
                    style = KptTheme.typography.titleSmall,
                    color = KptTheme.colorScheme.onPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(KptTheme.spacing.xs))
            }
            Text(
                text = card.account.identification,
                style = KptTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                color = KptTheme.colorScheme.onPrimary,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Preview
@Composable
private fun HomeCardsSectionPreview() {
    MifosXOpenBankingTheme {
        HomeCardsSection(
            cards = listOf(
                AccountWithBalance(
                    account = BankAccount(
                        accountId = "acc-card",
                        accountTypeCode = "CARD",
                        currency = "GBP",
                        identification = "xxxx-xxxx-xxxx-3456",
                        scheme = AccountScheme.Pan,
                        accountHolderName = "Mr Bantu",
                    ),
                    balance = AccountBalance("acc-card", "GBP", "342.18", "342.18"),
                ),
            ),
            onCardClick = {},
        )
    }
}
