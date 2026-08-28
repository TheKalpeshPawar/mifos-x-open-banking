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

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import template.core.base.designsystem.theme.KptTheme

@Composable
fun MifosSectionHeading(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = KptTheme.typography.labelMedium,
    color: Color = KptTheme.colorScheme.onSurfaceVariant,
    fontWeight: FontWeight = FontWeight.SemiBold,
) {
    Text(
        text = text,
        style = style,
        fontWeight = fontWeight,
        color = color,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun MifosSectionHeadingPreview() {
    MifosXOpenBankingTheme {
        MifosSectionHeading(text = "Accounts")
    }
}
