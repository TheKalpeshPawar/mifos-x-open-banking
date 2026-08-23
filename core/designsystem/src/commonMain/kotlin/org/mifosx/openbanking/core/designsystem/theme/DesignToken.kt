/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * App-level dimension tokens, sized to the Material 3 scale. Components read these instead of
 * hardcoding `dp` literals. `spacing`, the standard `shapes` and `elevation` stay on
 * [template.core.base.designsystem.theme.KptTheme].
 */
object DesignToken {
    /** Fixed component dimensions. */
    val sizes: AppSizes = AppSizes()

    /** Border widths. */
    val strokes: AppStrokes = AppStrokes()

    /** Fully-rounded shapes [template.core.base.designsystem.theme.KptTheme.shapes] lacks. */
    val shapes: AppShapes = AppShapes()
}

/** Fixed component dimensions, Material 3 scale. */
data class AppSizes(
    val buttonHeight: Dp = 48.dp,
    val inputHeight: Dp = 56.dp,
    val iconExtraSmall: Dp = 18.dp,
    val iconSmall: Dp = 20.dp,
    val iconMedium: Dp = 24.dp,
    val iconLarge: Dp = 36.dp,
    val iconExtraLarge: Dp = 40.dp,
    val iconHuge: Dp = 48.dp,
    val avatarSmall: Dp = 40.dp,
    val avatarMedium: Dp = 48.dp,
    val avatarLarge: Dp = 64.dp,
    val avatarXLarge: Dp = 96.dp,
    val rowMin: Dp = 48.dp,
    val cardRow: Dp = 56.dp,
    val rowTall: Dp = 64.dp,
    val cardMin: Dp = 120.dp,
    val cardHeight: Dp = 128.dp,
    val heroCard: Dp = 180.dp,
    val step: Dp = 40.dp,
    val badge: Dp = 96.dp,
    val imageSmall: Dp = 50.dp,
    val imageLarge: Dp = 100.dp,
    val imageWide: Dp = 28.dp,
    val navigationRailWidth: Dp = 80.dp,
    val illustration: Dp = 212.dp,
)

/** Border widths. */
data class AppStrokes(
    val thin: Dp = 0.5.dp,
    val hairline: Dp = 1.dp,
    val medium: Dp = 1.5.dp,
    val thick: Dp = 2.dp,
)

/** Fully-rounded shapes. */
data class AppShapes(
    val pill: RoundedCornerShape = RoundedCornerShape(percent = 50),
    val circle: RoundedCornerShape = CircleShape,
)
