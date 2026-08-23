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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.designsystem.icon.AppIcons
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import org.mifosx.openbanking.core.ui.generated.resources.Res
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_retry
import template.core.base.designsystem.theme.KptTheme

/** A labelled row with a trailing icon, used for tappable profile actions. */
@Composable
fun UserProfileField(
    text: StringResource,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(KptTheme.spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(text),
            style = KptTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        )
        Icon(
            imageVector = icon,
            contentDescription = "User Profile Icon",
        )
    }
    HorizontalDivider()
}

/** A labelled row with a trailing value. */
@Composable
fun UserProfileField(
    label: StringResource,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(KptTheme.spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(label),
            style = KptTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        )
        Text(
            text = value,
            style = KptTheme.typography.bodyMedium,
        )
    }
    HorizontalDivider()
}

@Preview
@Composable
private fun UserProfileFieldPreview() {
    MifosXOpenBankingTheme {
        UserProfileField(
            text = Res.string.core_ui_retry,
            icon = AppIcons.ChevronRight,
            onClick = {},
        )
        UserProfileField(
            label = Res.string.core_ui_retry,
            value = "Value",
        )
    }
}
