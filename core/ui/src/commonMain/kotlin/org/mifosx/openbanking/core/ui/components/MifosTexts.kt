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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.designsystem.icon.AppIcons
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import template.core.base.designsystem.theme.KptTheme

/** A title and a description on one row, title leading and description trailing. */
@Composable
fun MifosTextTitleDescSingleLine(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            style = KptTheme.typography.labelMedium,
            text = title,
            modifier = Modifier.alpha(0.7f),
        )
        Text(
            style = KptTheme.typography.bodyMedium,
            text = description,
        )
    }
}

/** A title over a description, each on its own line. */
@Composable
fun MifosTextTitleDescDoubleLine(
    title: String,
    description: String,
    descriptionStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = KptTheme.typography.labelMedium,
            modifier = Modifier.alpha(0.7f).fillMaxWidth(),
        )
        Text(
            text = description,
            style = descriptionStyle,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** A title, a description and a trailing clickable icon on one row. */
@Composable
fun MifosTextTitleDescDrawableSingleLine(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconSize: Dp = DesignToken.sizes.iconExtraSmall,
    onIconClick: () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            style = KptTheme.typography.labelMedium,
            text = title,
            modifier = Modifier.weight(1f).alpha(0.7f),
        )
        Text(
            style = KptTheme.typography.bodyMedium,
            text = description,
        )
        Spacer(modifier = Modifier.width(KptTheme.spacing.xs))
        Icon(
            imageVector = icon,
            contentDescription = "Image",
            modifier = Modifier
                .size(iconSize)
                .clickable { onIconClick() },
        )
    }
}

/** A title and a description that share the row width evenly. */
@Composable
fun MifosTitleDescSingleLineEqual(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            style = KptTheme.typography.labelMedium,
            text = title,
            modifier = Modifier.alpha(0.7f).weight(1f),
        )
        Text(
            style = KptTheme.typography.bodyMedium,
            text = description,
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview
@Composable
private fun MifosTextsPreview() {
    MifosXOpenBankingTheme {
        Column(
            modifier = Modifier.padding(KptTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
        ) {
            MifosTextTitleDescSingleLine(title = "Title", description = "Description")
            MifosTextTitleDescDoubleLine(
                title = "Title",
                description = "Description",
                descriptionStyle = KptTheme.typography.bodyMedium,
            )
            MifosTextTitleDescDrawableSingleLine(
                title = "Title",
                description = "Description",
                icon = AppIcons.AttachMoney,
            )
            MifosTitleDescSingleLineEqual(title = "Title", description = "Description")
        }
    }
}
