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

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.designsystem.icon.AppIcons
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import org.mifosx.openbanking.core.designsystem.utils.nonLetterColorVisualTransformation
import org.mifosx.openbanking.core.designsystem.utils.tabNavigation
import template.core.base.designsystem.theme.KptTheme

/**
 * A password field with a show/hide toggle, built on the config-based [MifosOutlinedTextField].
 *
 * When [showPassword] is false the value is masked; when [readOnly] is set, digits and symbols are
 * tinted rather than masked. [hint] renders as the error/supporting line.
 */
@Composable
fun MifosPasswordField(
    label: String,
    value: String,
    showPassword: Boolean,
    showPasswordChange: (Boolean) -> Unit,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = KptTheme.shapes.medium,
    colors: TextFieldColors = mifosOutlinedTextFieldColors(),
    isError: Boolean = false,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    hint: String? = null,
    showPasswordTestTag: String? = null,
    autoFocus: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Password,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val focusRequester = remember { FocusRequester() }
    MifosOutlinedTextField(
        modifier = modifier
            .tabNavigation()
            .focusRequester(focusRequester),
        shape = shape,
        colors = colors,
        label = label,
        value = value,
        onValueChange = onValueChange,
        config = MifosTextFieldConfig(
            visualTransformation = when {
                !showPassword -> PasswordVisualTransformation()
                readOnly -> nonLetterColorVisualTransformation()
                else -> VisualTransformation.None
            },
            singleLine = singleLine,
            readOnly = readOnly,
            isError = isError,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction,
            ),
            showClearIcon = false,
            keyboardActions = keyboardActions,
            errorText = hint,
            trailingIcon = {
                val color = if (isError) {
                    KptTheme.colorScheme.error
                } else {
                    KptTheme.colorScheme.onSurface
                }
                IconButton(
                    onClick = { showPasswordChange.invoke(!showPassword) },
                ) {
                    val imageVector = if (showPassword) AppIcons.VisibilityOff else AppIcons.Visibility

                    Icon(
                        modifier = Modifier.semantics { showPasswordTestTag?.let { testTag = it } },
                        imageVector = imageVector,
                        contentDescription = "togglePassword",
                        tint = color,
                    )
                }
            },
        ),
    )
    if (autoFocus) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
}

/** A password field that owns its show/hide state. */
@Composable
fun MifosPasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = KptTheme.shapes.medium,
    colors: TextFieldColors = mifosOutlinedTextFieldColors(),
    isError: Boolean = false,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    hint: String? = null,
    initialShowPassword: Boolean = false,
    showPasswordTestTag: String? = null,
    autoFocus: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Password,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    var showPassword by rememberSaveable { mutableStateOf(initialShowPassword) }
    MifosPasswordField(
        modifier = modifier,
        label = label,
        shape = shape,
        colors = colors,
        isError = isError,
        value = value,
        showPassword = showPassword,
        showPasswordChange = { showPassword = !showPassword },
        onValueChange = onValueChange,
        readOnly = readOnly,
        singleLine = singleLine,
        hint = hint,
        showPasswordTestTag = showPasswordTestTag,
        autoFocus = autoFocus,
        keyboardType = keyboardType,
        imeAction = imeAction,
        keyboardActions = keyboardActions,
    )
}

@Preview
@Composable
private fun MifosPasswordFieldPreview() {
    MifosXOpenBankingTheme {
        MifosPasswordField(
            label = "Password",
            value = "",
            onValueChange = {},
            initialShowPassword = false,
            hint = "Minimum 8 characters",
        )
    }
}
