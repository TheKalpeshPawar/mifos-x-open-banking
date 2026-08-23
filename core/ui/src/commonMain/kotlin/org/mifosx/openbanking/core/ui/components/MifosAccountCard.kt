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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.designsystem.icon.AppIcons
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import template.core.base.designsystem.theme.KptTheme

/** Clickable account row with a round icon, number/type, status and chevron. */
@Composable
fun MifosAccountCard(
    accountId: Long,
    accountNumber: String?,
    accountType: String?,
    accountStatus: String,
    accountStatusColor: Color,
    onAccountClick: (Long) -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onAccountClick(accountId) }
            .padding(vertical = KptTheme.spacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Person Account",
                tint = KptTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                modifier = Modifier
                    .background(
                        color = KptTheme.colorScheme.background.copy(alpha = 0.5f),
                        shape = CircleShape,
                    )
                    .padding(KptTheme.spacing.sm),
            )

            Spacer(modifier = Modifier.width(KptTheme.spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = accountNumber ?: "",
                    style = KptTheme.typography.titleSmall,
                    color = KptTheme.colorScheme.onBackground,
                )
                Text(
                    text = accountType ?: "",
                    style = KptTheme.typography.bodySmall,
                    color = KptTheme.colorScheme.secondary,
                )
            }

            Spacer(modifier = Modifier.width(KptTheme.spacing.md))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
            ) {
                Text(
                    text = accountStatus,
                    style = KptTheme.typography.labelSmall,
                    color = accountStatusColor,
                )
                Icon(
                    imageVector = AppIcons.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(DesignToken.sizes.iconSmall),
                )
            }
        }
    }
}

@Preview
@Composable
private fun MifosAccountCardPreview() {
    MifosXOpenBankingTheme {
        MifosAccountCard(
            accountId = 1L,
            accountNumber = "2689-7897-6666",
            accountType = "Wallet Account",
            accountStatus = "£ 23,315.50",
            accountStatusColor = KptTheme.colorScheme.primary,
            onAccountClick = {},
            icon = AppIcons.Person,
        )
    }
}
