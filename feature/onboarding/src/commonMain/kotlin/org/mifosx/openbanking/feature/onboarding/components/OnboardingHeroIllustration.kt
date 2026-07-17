/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.onboarding.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import template.core.base.designsystem.theme.KptTheme

/**
 * The onboarding hero: a hexagonal shield holding a padlock, ringed by soft halos and a few
 * "data node" dots — reproduced from the design mockup's inline SVG (viewBox 0 0 200 200).
 *
 * Drawn with Canvas rather than shipped as a static vector so it stays theme-aware: every colour is
 * a live [KptTheme] token, so it reads correctly in both light and dark. The mockup's two tiny text
 * glyphs (a "£" and a Material-Symbols badge) are decorative and intentionally omitted — they do not
 * translate to vector geometry.
 */
@Composable
fun OnboardingHeroIllustration(modifier: Modifier = Modifier) {
    val primary = KptTheme.colorScheme.primary
    val primaryContainer = KptTheme.colorScheme.primaryContainer
    val onPrimary = KptTheme.colorScheme.onPrimary
    val secondary = KptTheme.colorScheme.secondary
    val secondaryContainer = KptTheme.colorScheme.secondaryContainer
    val tertiary = KptTheme.colorScheme.tertiary
    val tertiaryContainer = KptTheme.colorScheme.tertiaryContainer
    val outlineVariant = KptTheme.colorScheme.outlineVariant

    Canvas(modifier) {
        val s = size.minDimension / VIEWBOX
        translate(
            left = (size.width - VIEWBOX * s) / 2f,
            top = (size.height - VIEWBOX * s) / 2f,
        ) {
            fun at(x: Float, y: Float) = Offset(x * s, y * s)

            drawCircle(primaryContainer.copy(alpha = 0.25f), radius = 88f * s, center = at(100f, 100f))
            drawCircle(primaryContainer.copy(alpha = 0.35f), radius = 72f * s, center = at(100f, 100f))

            val shield = Path().apply {
                moveTo(100f * s, 22f * s)
                lineTo(162f * s, 48f * s)
                lineTo(162f * s, 104f * s)
                cubicTo(162f * s, 144f * s, 134f * s, 168f * s, 100f * s, 180f * s)
                cubicTo(66f * s, 168f * s, 38f * s, 144f * s, 38f * s, 104f * s)
                lineTo(38f * s, 48f * s)
                close()
            }
            drawPath(shield, primaryContainer)

            val border = Path().apply {
                moveTo(100f * s, 28f * s)
                lineTo(156f * s, 52f * s)
                lineTo(156f * s, 104f * s)
                cubicTo(156f * s, 140f * s, 130f * s, 162f * s, 100f * s, 174f * s)
                cubicTo(70f * s, 162f * s, 44f * s, 140f * s, 44f * s, 104f * s)
                lineTo(44f * s, 52f * s)
                close()
            }
            drawPath(border, primary.copy(alpha = 0.6f), style = Stroke(width = 2f * s))

            val dash = PathEffect.dashPathEffect(floatArrayOf(4f * s, 3f * s))
            val connector = outlineVariant.copy(alpha = 0.7f)
            drawLine(connector, at(50f, 75f), at(80f, 88f), strokeWidth = 1.5f * s, pathEffect = dash)
            drawLine(connector, at(152f, 125f), at(122f, 110f), strokeWidth = 1.5f * s, pathEffect = dash)
            drawCircle(tertiaryContainer, 6f * s, at(44f, 75f))
            drawCircle(tertiary, 3f * s, at(44f, 75f))
            drawCircle(secondaryContainer, 5f * s, at(158f, 130f))
            drawCircle(secondary, 2.5f * s, at(158f, 130f))
            drawCircle(secondaryContainer.copy(alpha = 0.9f), 9f * s, at(157f, 70f))

            drawRoundRect(
                primary,
                topLeft = at(82f, 95f),
                size = Size(36f * s, 28f * s),
                cornerRadius = CornerRadius(5f * s, 5f * s),
            )
            val shackle = Path().apply {
                moveTo(90f * s, 95f * s)
                lineTo(90f * s, 86f * s)
                cubicTo(90f * s, 76f * s, 110f * s, 76f * s, 110f * s, 86f * s)
                lineTo(110f * s, 95f * s)
            }
            drawPath(shackle, primary, style = Stroke(width = 4f * s, cap = StrokeCap.Round))
            drawCircle(onPrimary, 4f * s, at(100f, 109f))
            drawRoundRect(
                onPrimary,
                topLeft = at(98f, 109f),
                size = Size(4f * s, 7f * s),
                cornerRadius = CornerRadius(2f * s, 2f * s),
            )
        }
    }
}

private const val VIEWBOX = 200f
