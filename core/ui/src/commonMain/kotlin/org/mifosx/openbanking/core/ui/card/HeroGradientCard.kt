/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.ui.card

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Brand hero surface: a primary→tertiary vertical gradient with a soft light "orb"
 * bleeding off the top-right corner (the Hero.png design language). Colors come from the
 * theme pair, so light/dark both work; content color defaults to onPrimary.
 *
 * Pure background container — it imposes NO size or padding of its own. Sizing comes
 * entirely from [modifier] and the content, so it drops into top bars, columns, rows,
 * list headers, anywhere. [shape] controls the corners (e.g. [RoundedCornerShape] for a
 * floating card, bottom-only rounding for a full-bleed header, RectangleShape for none).
 */
@Composable
fun HeroGradientCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Column(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                drawRect(Brush.verticalGradient(colors = listOf(primary, tertiary)))
                val center = Offset(x = size.width * ORB_CENTER_X, y = size.height * ORB_CENTER_Y)
                val radius = size.width * ORB_RADIUS
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to Color.White.copy(alpha = 0.30f),
                            0.85f to Color.White.copy(alpha = 0.22f),
                            1.0f to Color.White.copy(alpha = 0.0f),
                        ),
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                    center = center,
                )
            }
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        CompositionLocalProvider(LocalContentColor provides onPrimary) {
            content()
        }
    }
}

private const val ORB_CENTER_X = 0.84f
private const val ORB_CENTER_Y = 0.08f
private const val ORB_RADIUS = 0.32f
