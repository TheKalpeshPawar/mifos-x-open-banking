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

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.toSize
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.designsystem.icon.AppIcons
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import template.core.base.designsystem.theme.KptTheme

/** An outlined dropdown field built on the config-based [MifosOutlinedTextField]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MifosOutlineDropdown(
    selectedText: String,
    items: Map<Long, String>,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onItemSelected: (Long, String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var textFieldSize by remember { mutableStateOf(Size.Zero) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier,
    ) {
        MifosOutlinedTextField(
            value = selectedText,
            onValueChange = {},
            label = label,
            config = MifosTextFieldConfig(
                trailingIcon = {
                    Icon(
                        modifier = Modifier.size(DesignToken.sizes.iconSmall),
                        imageVector = if (expanded) AppIcons.KeyboardArrowUp else AppIcons.KeyboardArrowDown,
                        contentDescription = null,
                        tint = if (enabled) {
                            KptTheme.colorScheme.onSurface
                        } else {
                            KptTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        },
                    )
                },
                showClearIcon = false,
                readOnly = true,
                enabled = enabled,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                .onGloballyPositioned {
                    textFieldSize = it.size.toSize()
                }
                .then(if (enabled) Modifier.clickable { expanded = true } else Modifier),
            shape = KptTheme.shapes.medium,
            colors = mifosOutlinedTextFieldColors(),
        )

        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(with(LocalDensity.current) { textFieldSize.width.toDp() })
                .border(
                    width = DesignToken.strokes.hairline,
                    color = KptTheme.colorScheme.outline,
                    shape = KptTheme.shapes.medium,
                ),
            tonalElevation = KptTheme.elevation.level1,
            shadowElevation = KptTheme.elevation.level1,
            containerColor = KptTheme.colorScheme.surface,
            shape = KptTheme.shapes.large,
        ) {
            items.forEach { (productID, product) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = product,
                            style = KptTheme.typography.bodyLarge,
                            color = KptTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    onClick = {
                        expanded = false
                        onItemSelected(productID, product)
                    },
                )
            }
        }
    }
}

@Preview
@Composable
private fun MifosOutlineDropdownPreview() {
    var selected by remember { mutableStateOf("") }
    MifosXOpenBankingTheme {
        Column(
            modifier = Modifier.fillMaxSize().padding(KptTheme.spacing.md),
        ) {
            MifosOutlineDropdown(
                selectedText = selected,
                items = mapOf(
                    1L to "New Product",
                    2L to "Savings Account",
                    3L to "Fixed Deposit",
                ),
                onItemSelected = { _, product -> selected = product },
                label = "New Product",
            )
        }
    }
}
