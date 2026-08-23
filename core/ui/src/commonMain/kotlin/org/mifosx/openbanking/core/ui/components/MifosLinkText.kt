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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import template.core.base.designsystem.theme.KptTheme

/** A tappable text link, underlined by default. */
@Composable
fun MifosLinkText(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isUnderlined: Boolean = true,
) {
    Text(
        text = text,
        style = KptTheme.typography.bodyMedium.copy(
            textDecoration = if (isUnderlined) TextDecoration.Underline else null,
        ),
        modifier = modifier
            .padding(vertical = KptTheme.spacing.xs)
            .clickable {
                onClick()
            },
    )
}

@Preview
@Composable
private fun MifosLinkTextPreview() {
    MifosXOpenBankingTheme {
        MifosLinkText(text = "Link Text", onClick = {})
    }
}
