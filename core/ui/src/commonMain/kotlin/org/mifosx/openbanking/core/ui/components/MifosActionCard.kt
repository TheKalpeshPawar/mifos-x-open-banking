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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.designsystem.icon.AppIcons
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import org.mifosx.openbanking.core.ui.generated.resources.Res
import org.mifosx.openbanking.core.ui.generated.resources.core_ui_no_data
import template.core.base.designsystem.theme.KptTheme

/** Clickable action row with a round icon, title, subtitle and chevron. */
@Composable
fun MifosActionCard(
    title: StringResource,
    subTitle: StringResource,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = KptTheme.spacing.md),
    ) {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = KptTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier
                    .background(
                        color = KptTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                        shape = CircleShape,
                    )
                    .padding(KptTheme.spacing.sm),
            )

            Spacer(modifier = Modifier.width(KptTheme.spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(title),
                    style = KptTheme.typography.titleSmall,
                    color = KptTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(subTitle),
                    style = KptTheme.typography.bodySmall,
                    color = KptTheme.colorScheme.secondary,
                )
            }

            Spacer(modifier = Modifier.width(KptTheme.spacing.md))

            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(DesignToken.sizes.iconSmall),
            )
        }
    }
}

@Preview
@Composable
private fun MifosActionCardPreview() {
    MifosXOpenBankingTheme {
        MifosActionCard(
            title = Res.string.core_ui_no_data,
            subTitle = Res.string.core_ui_no_data,
            icon = AppIcons.AttachMoney,
            onClick = {},
        )
    }
}
