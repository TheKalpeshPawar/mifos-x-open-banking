/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.designsystem.icon.AppIcons
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import template.core.base.designsystem.theme.KptTheme

/** Filled card with a content slot, full-width and optionally clickable. */
@Composable
fun MifosCard(
    modifier: Modifier = Modifier,
    shape: Shape = KptTheme.shapes.small,
    elevation: Dp = KptTheme.elevation.level1,
    onClick: (() -> Unit)? = null,
    colors: CardColors = CardDefaults.cardColors(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        shape = shape,
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = colors,
        content = content,
    )
}

/**
 * Theme-aware card that dispatches to Material3 [Card], [ElevatedCard] or [OutlinedCard] by
 * [variant].
 */
@Composable
fun MifosCustomCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    enabled: Boolean = true,
    variant: CardVariant = CardVariant.FILLED,
    shape: Shape? = null,
    colors: CardColors? = null,
    elevation: CardElevation? = null,
    borderStroke: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    when (variant) {
        CardVariant.FILLED -> Card(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape ?: CardDefaults.shape,
            colors = colors ?: CardDefaults.cardColors(),
            elevation = elevation ?: CardDefaults.cardElevation(),
            border = borderStroke,
            interactionSource = interactionSource,
        ) { content() }

        CardVariant.ELEVATED -> ElevatedCard(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape ?: CardDefaults.elevatedShape,
            colors = colors ?: CardDefaults.elevatedCardColors(),
            elevation = elevation ?: CardDefaults.elevatedCardElevation(),
            interactionSource = interactionSource,
        ) { content() }

        CardVariant.OUTLINED -> OutlinedCard(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape ?: CardDefaults.outlinedShape,
            colors = colors ?: CardDefaults.outlinedCardColors(),
            elevation = elevation ?: CardDefaults.outlinedCardElevation(),
            border = borderStroke ?: CardDefaults.outlinedCardBorder(enabled),
            interactionSource = interactionSource,
        ) { content() }
    }
}

/** Outlined option card with an icon and label. */
@Composable
fun MifosExploreCard(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    multiline: Boolean = true,
    maxLines: Int = 2,
    onClick: () -> Unit,
) {
    MifosCustomCard(
        modifier = modifier
            .height(DesignToken.sizes.inputHeight)
            .border(
                DesignToken.strokes.thin,
                KptTheme.colorScheme.secondaryContainer,
                KptTheme.shapes.medium,
            ),
        variant = CardVariant.OUTLINED,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
            contentColor = KptTheme.colorScheme.onSurface,
        ),
    ) {
        Row(
            modifier = Modifier.padding(KptTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(DesignToken.sizes.iconMedium),
            )

            Text(
                text = if (multiline) text.replaceFirst(" ", "\n") else text,
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                style = KptTheme.typography.bodyMedium,
                maxLines = maxLines,
                overflow = TextOverflow.MiddleEllipsis,
                textAlign = TextAlign.Start,
            )
        }
    }
}

@Preview
@Composable
private fun MifosExploreCardPreview() {
    MifosXOpenBankingTheme {
        Column(
            modifier = Modifier.padding(KptTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
        ) {
            MifosExploreCard(icon = AppIcons.AttachMoney, text = "Personal Loan", onClick = {})
            MifosExploreCard(icon = AppIcons.AttachMoney, text = "Savings Account", onClick = {})
        }
    }
}

/** Card visual variant for [MifosCustomCard]. */
enum class CardVariant {
    FILLED,
    ELEVATED,
    OUTLINED,
}
