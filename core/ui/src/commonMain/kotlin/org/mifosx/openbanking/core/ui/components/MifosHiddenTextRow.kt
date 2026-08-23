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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.designsystem.icon.AppIcons
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import template.core.base.designsystem.theme.KptTheme

/** A title with a value that toggles between hidden and shown behind a visibility icon. */
@Composable
fun MifosHiddenTextRow(
    title: String,
    hiddenText: String,
    hiddenColor: Color,
    hidingText: String,
    visibilityIcon: ImageVector,
    visibilityOffIcon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isHidden by remember { mutableStateOf(true) }

    Row(
        modifier.clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = KptTheme.typography.labelMedium,
            modifier = Modifier
                .alpha(0.7f)
                .weight(1f),
        )
        Text(
            text = if (isHidden) {
                hidingText
            } else {
                hiddenText
            },
            style = KptTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = hiddenColor,
        )
        IconButton(
            onClick = { isHidden = !isHidden },
            modifier = Modifier
                .padding(start = KptTheme.spacing.xs)
                .size(DesignToken.sizes.iconMedium),
        ) {
            Icon(
                imageVector = if (isHidden) visibilityIcon else visibilityOffIcon,
                contentDescription = "Show or hide total amount",
            )
        }
    }
}

@Preview
@Composable
private fun MifosHiddenTextRowPreview() {
    MifosXOpenBankingTheme {
        MifosHiddenTextRow(
            title = "Title",
            hiddenText = "Hidden Text",
            hiddenColor = KptTheme.colorScheme.primary,
            hidingText = "Hiding Text",
            visibilityIcon = AppIcons.Visibility,
            visibilityOffIcon = AppIcons.VisibilityOff,
            onClick = {},
        )
    }
}
