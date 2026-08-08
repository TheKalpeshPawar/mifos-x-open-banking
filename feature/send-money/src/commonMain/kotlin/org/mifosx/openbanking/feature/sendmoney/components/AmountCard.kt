/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.sendmoney.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.common.currencySymbol
import org.mifosx.openbanking.feature.sendmoney.CardBorder
import org.mifosx.openbanking.feature.sendmoney.CardCorner
import org.mifosx.openbanking.feature.sendmoney.CardPadding
import org.mifosx.openbanking.feature.sendmoney.HeadingGap
import org.mifosx.openbanking.feature.sendmoney.SectionGap
import org.mifosx.openbanking.feature.sendmoney.SendMoneyTestTags
import org.mifosx.openbanking.feature.sendmoney.generated.resources.Res
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_amount_available
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_amount_label
import org.mifosx.openbanking.feature.sendmoney.generated.resources.feature_send_money_amount_placeholder
import template.core.base.designsystem.theme.KptTheme

/** Keeps an empty field a comfortable tap target rather than a caret with nothing around it. */
private val FieldMinWidth = 96.dp

/**
 * The amount, as the card the reference leads with: a superscript currency mark, the figure, a
 * hairline, and the available balance beneath.
 *
 * The figure is a [BasicTextField] rather than a label over an `OutlinedTextField`. What is being
 * asked for is one number, and a bordered box with a floating label around a 36sp figure reads as a
 * form field among form fields — the point of the card is that this is the payment, not a detail
 * of it.
 *
 * **Major units.** `250` and `250.00` both mean £250. The field was labelled "Amount in pence" and
 * parsed minor units, so someone typing 250 for £250 sent £2.50. Nothing on screen mentions pence
 * now; the conversion happens once, in the ViewModel, and the draft still carries minor units.
 *
 * [balanceLabel] is blank until a payer is chosen, which is what hides the balance line: with no
 * account there is no balance to state, and a printed £0.00 would be a claim about one. An
 * [errorMessage] takes that line rather than being added below it, so the card cannot grow taller
 * as the customer types.
 *
 * [trailing] sits inside the figure's own row, not at the card's edge. The international rail passes
 * the instructed-currency control through it, and that control qualifies the amount: separating them
 * across the width of the card would put the unit an inch from the number it applies to, which is
 * the same defect the intrinsic width in [AmountField] exists to prevent. It defaults to empty, so
 * the domestic card is exactly what it was.
 */
@Composable
internal fun SendMoneyAmountCard(
    amount: String,
    instructedCurrency: String,
    balanceLabel: String,
    errorMessage: String?,
    onAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    val amountLabel = stringResource(Res.string.feature_send_money_amount_label)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(SendMoneyTestTags.AMOUNT_CARD)
            .clip(RoundedCornerShape(CardCorner))
            .border(
                width = CardBorder,
                color = if (errorMessage != null) {
                    KptTheme.colorScheme.error
                } else {
                    KptTheme.colorScheme.outlineVariant
                },
                shape = RoundedCornerShape(CardCorner),
            )
            .background(KptTheme.colorScheme.surfaceContainerLowest)
            .padding(CardPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SectionGap),
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(HeadingGap / 2),
        ) {
            Text(
                text = currencySymbol(instructedCurrency),
                style = KptTheme.typography.titleMedium,
                color = KptTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = HeadingGap),
            )
            AmountField(
                amount = amount,
                contentDescription = amountLabel,
                onAmountChange = onAmountChange,
            )
            trailing()
        }

        // Only when there is something under it. With no payer there is no balance to state and no
        // problem to report, and a rule with nothing beneath it reads as a card that failed to load.
        if (errorMessage != null || balanceLabel.isNotBlank()) {
            HorizontalDivider(color = KptTheme.colorScheme.outlineVariant)
            AmountFooter(balanceLabel = balanceLabel, errorMessage = errorMessage)
        }
    }
}

@Composable
private fun AmountFooter(balanceLabel: String, errorMessage: String?) {
    when {
        errorMessage != null -> Text(
            text = errorMessage,
            style = KptTheme.typography.bodySmall,
            color = KptTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(SendMoneyTestTags.AMOUNT_ERROR),
        )

        balanceLabel.isNotBlank() -> Text(
            text = stringResource(Res.string.feature_send_money_amount_available, balanceLabel),
            style = KptTheme.typography.bodySmall,
            color = KptTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(SendMoneyTestTags.AMOUNT_BALANCE),
        )
    }
}

/**
 * The figure itself.
 *
 * The placeholder is drawn behind the field rather than substituted into it, so an empty card still
 * shows the shape of what is wanted without anyone having to delete a zero to type over it.
 */
@Composable
private fun AmountField(
    amount: String,
    contentDescription: String,
    onAmountChange: (String) -> Unit,
) {
    val figureStyle = KptTheme.typography.displaySmall.copy(
        color = KptTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    // Intrinsic width, not a fixed one. BasicTextField otherwise claims a default minimum far wider
    // than the digits in it, and because the figure is centred inside that width the currency mark
    // ended up marooned an inch to the left of its own amount.
    Box(
        modifier = Modifier.width(IntrinsicSize.Min).widthIn(min = FieldMinWidth),
        contentAlignment = Alignment.Center,
    ) {
        if (amount.isEmpty()) {
            Text(
                text = stringResource(Res.string.feature_send_money_amount_placeholder),
                style = figureStyle,
                color = KptTheme.colorScheme.outlineVariant,
            )
        }
        BasicTextField(
            value = amount,
            onValueChange = onAmountChange,
            textStyle = figureStyle,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            cursorBrush = SolidColor(KptTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(SendMoneyTestTags.AMOUNT_FIELD)
                .semantics { this.contentDescription = contentDescription },
        )
    }
}
