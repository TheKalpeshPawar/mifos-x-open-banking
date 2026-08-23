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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import template.core.base.designsystem.theme.KptTheme

/**
 * Filled button with a generic content slot. Wraps Material 3 [Button], sized from
 * [DesignToken.sizes.buttonHeight].
 */
@Composable
fun MifosButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit = {},
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(DesignToken.sizes.buttonHeight),
        enabled = enabled,
        colors = colors,
        shape = shape,
        elevation = elevation,
        contentPadding = contentPadding,
        content = content,
    )
}

/**
 * Filled button with a text slot, primary-tinted by default.
 */
@Composable
fun MifosButton(
    onClick: () -> Unit,
    text: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = KptTheme.shapes.medium,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    colors: ButtonColors = ButtonDefaults.buttonColors(containerColor = KptTheme.colorScheme.primary),
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(DesignToken.sizes.buttonHeight),
        enabled = enabled,
        colors = colors,
        shape = shape,
        elevation = elevation,
        contentPadding = contentPadding,
        content = { text() },
    )
}

/**
 * Outlined button with a generic content slot. Wraps Material 3 [OutlinedButton], sized from
 * [DesignToken.sizes.buttonHeight].
 */
@Composable
fun MifosOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = KptTheme.shapes.medium,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit = {},
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(DesignToken.sizes.buttonHeight),
        enabled = enabled,
        shape = shape,
        colors = colors,
        border = border,
        contentPadding = contentPadding,
        content = content,
    )
}

/**
 * Text button with a generic content slot. Wraps Material 3 [TextButton].
 */
@Composable
fun MifosTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit = {},
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = KptTheme.colorScheme.onBackground),
        content = content,
    )
}

/**
 * Text button with text and optional leading-icon slots.
 */
@Composable
fun MifosTextButton(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    MifosTextButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        MifosButtonContent(text = text, leadingIcon = leadingIcon)
    }
}

@Composable
private fun MifosButtonContent(
    text: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    Row(modifier) {
        if (leadingIcon != null) {
            Box(Modifier.sizeIn(maxHeight = ButtonDefaults.IconSize)) {
                leadingIcon()
            }
        }
        Box(
            Modifier.padding(
                start = if (leadingIcon != null) ButtonDefaults.IconSpacing else 0.dp,
            ),
        ) {
            text()
        }
    }
}

/** Default values for the Mifos button primitives. */
object MifosButtonDefaults {
    /** Alpha applied to an outlined button's border when disabled. */
    const val DISABLED_OUTLINED_BUTTON_BORDER_ALPHA = 0.12f

    /** Default outlined-button border width. */
    val OutlinedButtonBorderWidth = DesignToken.strokes.hairline
}
