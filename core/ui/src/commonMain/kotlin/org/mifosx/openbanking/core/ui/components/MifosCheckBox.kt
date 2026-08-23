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

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme

/** A checkbox with a trailing text label. */
@Composable
fun MifosCheckBox(
    text: String,
    checked: Boolean,
    onCheckChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    checkboxColors: CheckboxColors = CheckboxDefaults.colors(),
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onCheckChanged.invoke(it) },
            colors = checkboxColors,
        )
        Text(text = text)
    }
}

@Preview
@Composable
private fun MifosCheckBoxPreview() {
    var checked by remember { mutableStateOf(false) }
    MifosXOpenBankingTheme {
        MifosCheckBox(
            text = "I agree to the terms",
            checked = checked,
            onCheckChanged = { checked = it },
        )
    }
}
