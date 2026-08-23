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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import template.core.base.designsystem.theme.KptTheme

/**
 * A labelled radio button with animated border/scale feedback and accessibility semantics.
 *
 * @param label The text label for the radio button.
 * @param selected Whether this radio button is currently selected.
 * @param onClick Callback when the radio button is clicked.
 * @param enabled Whether the radio button is enabled.
 * @param isError Whether the radio button shows an error state.
 * @param selectedColor Colour when selected.
 * @param unselectedColor Colour when unselected.
 * @param errorColor Colour in the error state.
 * @param selectedTextStyle Text style when selected.
 * @param unselectedTextStyle Text style when unselected.
 * @param borderWidth Width of the row's border.
 * @param animationDurationMs Duration of the animations.
 */
@Suppress("CyclomaticComplexMethod")
@Composable
fun MifosRadioButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    contentDescription: String? = null,
    selectedColor: Color = KptTheme.colorScheme.primary,
    unselectedColor: Color = KptTheme.colorScheme.outline,
    errorColor: Color = KptTheme.colorScheme.error,
    selectedTextStyle: TextStyle = KptTheme.typography.titleSmall,
    unselectedTextStyle: TextStyle = KptTheme.typography.titleSmall,
    borderWidth: Dp = DesignToken.strokes.hairline,
    animationDurationMs: Int = 200,
) {
    val animatedBorderColor by animateColorAsState(
        targetValue = when {
            isError -> errorColor
            selected -> selectedColor
            else -> KptTheme.colorScheme.outlineVariant
        },
        animationSpec = tween(animationDurationMs),
        label = "border_color_animation",
    )

    val animatedRadioColor by animateColorAsState(
        targetValue = when {
            isError -> errorColor
            selected -> selectedColor
            else -> unselectedColor
        },
        animationSpec = tween(animationDurationMs),
        label = "radio_color_animation",
    )

    val animatedScale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1.0f,
        animationSpec = tween(animationDurationMs),
        label = "scale_animation",
    )

    val textStyle = if (selected) selectedTextStyle else unselectedTextStyle
    val interactionSource = remember { MutableInteractionSource() }

    val stateDescription = when {
        isError -> "Error state"
        selected -> "Selected"
        else -> "Not selected"
    }

    Box(
        modifier = Modifier
            .scale(animatedScale)
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription ?: "Radio button for $label"
                this.stateDescription = stateDescription
            },
    ) {
        Row(
            modifier = modifier
                .padding(vertical = KptTheme.spacing.xs)
                .border(
                    width = borderWidth,
                    color = animatedBorderColor.copy(
                        alpha = if (enabled) 1.0f else 0.6f,
                    ),
                    shape = KptTheme.shapes.medium,
                )
                .clickable(
                    enabled = enabled,
                    onClick = onClick,
                    role = Role.RadioButton,
                    interactionSource = interactionSource,
                    indication = ripple(
                        bounded = true,
                        radius = DesignToken.sizes.iconMedium,
                    ),
                )
                .padding(KptTheme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = selected,
                // Handled by the Row's clickable.
                onClick = null,
                enabled = enabled,
                colors = RadioButtonColors(
                    selectedColor = animatedRadioColor,
                    unselectedColor = animatedRadioColor,
                    disabledSelectedColor = animatedRadioColor.copy(alpha = 0.6f),
                    disabledUnselectedColor = animatedRadioColor.copy(alpha = 0.6f),
                ),
                modifier = Modifier.clearAndSetSemantics { },
            )

            Text(
                text = label,
                style = textStyle.copy(
                    color = textStyle.color.copy(
                        alpha = if (enabled) 1.0f else 0.6f,
                    ),
                ),
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
    }
}

/**
 * A mutually exclusive group of [MifosRadioButton]s.
 *
 * @param options Options to display.
 * @param selectedOption The currently selected option.
 * @param onSelectionChange Callback when the selection changes.
 * @param enabled Whether the group is enabled.
 * @param isError Whether the group shows an error state.
 * @param spacing Spacing between the radio buttons.
 */
@Composable
fun MifosRadioButtonGroup(
    options: List<String>,
    selectedOption: String?,
    onSelectionChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    spacing: Dp = KptTheme.spacing.md,
) {
    Column(
        modifier = modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        options.forEach { option ->
            MifosRadioButton(
                label = option,
                selected = option == selectedOption,
                onClick = { onSelectionChange(option) },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                isError = isError,
                contentDescription = "Select $option",
            )
        }
    }
}

@Preview
@Composable
private fun MifosRadioButtonPreview() {
    var selectedLanguage by remember { mutableStateOf("English") }

    MifosXOpenBankingTheme {
        Column(
            modifier = Modifier.padding(KptTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
        ) {
            MifosRadioButton(
                label = "English (Selected)",
                selected = true,
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
            )
            MifosRadioButton(
                label = "Hindi (Unselected)",
                selected = false,
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
            )
            MifosRadioButton(
                label = "French (Error State)",
                selected = false,
                isError = true,
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
            )
            MifosRadioButtonGroup(
                options = listOf("English", "Hindi", "Telugu"),
                selectedOption = selectedLanguage,
                onSelectionChange = { selectedLanguage = it },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
