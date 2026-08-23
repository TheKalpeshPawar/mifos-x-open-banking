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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Shape
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.designsystem.icon.AppIcons
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import org.mifosx.openbanking.core.ui.generated.resources.Res
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_retry
import template.core.base.designsystem.theme.KptTheme

/** An exposed-dropdown text field whose options are plain strings. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MifosDropDownTextField(
    onClick: (Int, String) -> Unit,
    labelResId: StringResource,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    supportingText: String? = null,
    error: Boolean = false,
    shape: Shape = KptTheme.shapes.medium,
    optionsList: List<String> = listOf(),
    selectedOption: String? = null,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded && isEnabled,
        onExpandedChange = {
            if (isEnabled) {
                expanded = !expanded
            }
        },
        modifier = modifier.alpha(if (!isEnabled) 0.4f else 1f).fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selectedOption ?: "",
            shape = shape,
            onValueChange = { },
            label = { Text(stringResource(labelResId)) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
            readOnly = true,
            enabled = isEnabled,
            textStyle = KptTheme.typography.labelMedium,
            supportingText = { if (error) Text(text = supportingText ?: "") },
            isError = error,
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) AppIcons.ArrowDropUp else AppIcons.ArrowDropDown,
                    contentDescription = if (expanded) "Arrow Up Icon" else "Arrow Down Icon",
                )
            },
            colors = mifosOutlinedTextFieldColors(),
        )

        ExposedDropdownMenu(
            expanded = expanded && isEnabled,
            onDismissRequest = { expanded = false },
        ) {
            optionsList.forEachIndexed { index, item ->
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        onClick(index, item)
                    },
                    text = { Text(text = item) },
                )
            }
        }
    }
}

/** An exposed-dropdown text field whose options are label/sub-label pairs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MifosDropDownDoubleTextField(
    onClick: (Int, Pair<String, String>) -> Unit,
    labelResId: StringResource,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    supportingText: String? = null,
    error: Boolean = false,
    shape: Shape = KptTheme.shapes.medium,
    optionsList: List<Pair<String, String>> = listOf(),
    selectedOption: String? = null,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded && isEnabled,
        onExpandedChange = {
            if (isEnabled) {
                expanded = !expanded
            }
        },
        modifier = modifier.alpha(if (!isEnabled) 0.4f else 1f).fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selectedOption ?: "",
            onValueChange = { },
            label = { Text(stringResource(labelResId)) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
            readOnly = true,
            shape = shape,
            enabled = isEnabled,
            textStyle = KptTheme.typography.labelSmall,
            supportingText = { if (error) Text(text = supportingText ?: "") },
            isError = error,
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) AppIcons.ArrowDropUp else AppIcons.ArrowDropDown,
                    contentDescription = if (expanded) "Arrow Up Icon" else "Arrow Down Icon",
                )
            },
            colors = mifosOutlinedTextFieldColors(),
        )

        ExposedDropdownMenu(
            expanded = expanded && isEnabled,
            onDismissRequest = { expanded = false },
        ) {
            optionsList.forEachIndexed { index, item ->
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        onClick(index, item)
                    },
                    text = {
                        Column {
                            Text(text = item.first)
                            Text(text = item.second)
                        }
                    },
                )
            }
        }
    }
}

@Preview
@Composable
private fun MifosDropDownTextFieldPreview() {
    MifosXOpenBankingTheme {
        MifosDropDownTextField(
            onClick = { _, _ -> },
            labelResId = Res.string.core_ui_retry,
            optionsList = listOf("Option 1", "Option 2", "Option 3"),
            selectedOption = null,
        )
    }
}

@Preview
@Composable
private fun MifosDropDownDoubleTextFieldPreview() {
    MifosXOpenBankingTheme {
        MifosDropDownDoubleTextField(
            onClick = { _, _ -> },
            labelResId = Res.string.core_ui_retry,
            optionsList = listOf(
                Pair("Option 1", "Option 1 Description"),
                Pair("Option 2", "Option 2 Description"),
            ),
            selectedOption = null,
        )
    }
}
