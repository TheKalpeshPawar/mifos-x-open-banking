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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.designsystem.icon.AppIcons
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import template.core.base.designsystem.theme.KptTheme

/** A borderless search field with a search glyph placeholder and a clear trailing icon. */
@Composable
fun MifosSearchTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSearchDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    TextField(
        modifier = modifier,
        value = value,
        placeholder = {
            Icon(
                imageVector = AppIcons.Search,
                contentDescription = null,
            )
        },
        onValueChange = onValueChange,
        textStyle = KptTheme.typography.bodyLarge,
        trailingIcon = {
            AnimatedVisibility(visible = value.text.isNotEmpty()) {
                IconButton(onClick = onSearchDismiss) {
                    Icon(
                        imageVector = AppIcons.Close,
                        contentDescription = "Close Icon",
                    )
                }
            }
        },
        colors = TextFieldDefaults.colors().copy(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = KptTheme.colorScheme.outline,
            unfocusedIndicatorColor = KptTheme.colorScheme.outline,
            focusedTextColor = KptTheme.colorScheme.onSurface,
            unfocusedTextColor = KptTheme.colorScheme.onSurface,
        ),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search,
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                focusManager.clearFocus()
            },
        ),
        singleLine = true,
    )
}

@Preview
@Composable
private fun MifosSearchTextFieldPreview() {
    var value by remember { mutableStateOf(TextFieldValue("")) }
    MifosXOpenBankingTheme {
        MifosSearchTextField(
            value = value,
            onValueChange = { value = it },
            onSearchDismiss = { value = TextFieldValue("") },
        )
    }
}
