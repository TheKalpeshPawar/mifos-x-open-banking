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

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import template.core.base.designsystem.theme.KptTheme

/**
 * The form's field colours: a primary border at rest as well as focused, on the same ground the
 * cards use.
 *
 * The error colours are left at their defaults on purpose — a refused value has to be able to turn
 * the border red, and naming primary for every state would bury it.
 */
@Composable
fun mifosOutlinedTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = KptTheme.colorScheme.primary,
    unfocusedBorderColor = KptTheme.colorScheme.primary,
    focusedContainerColor = KptTheme.colorScheme.surfaceContainerLowest,
    unfocusedContainerColor = KptTheme.colorScheme.surfaceContainerLowest,
)
