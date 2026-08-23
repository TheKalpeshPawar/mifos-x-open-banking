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
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.designsystem.icon.AppIcons
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import template.core.base.designsystem.theme.KptTheme

/** A single transaction row: a debit/credit symbol, a title, a timestamp and a signed amount. */
@Composable
fun TransactionScreenItem(
    title: String,
    date: String,
    time: String,
    transactionAmount: String,
    isCredited: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = KptTheme.spacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = if (isCredited) AppIcons.Add else AppIcons.Remove,
                contentDescription = "Symbol",
                tint = KptTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                modifier = Modifier
                    .size(DesignToken.sizes.iconExtraLarge)
                    .background(
                        color = KptTheme.colorScheme.background.copy(alpha = 0.2f),
                        shape = CircleShape,
                    )
                    .padding(KptTheme.spacing.sm),
            )

            Spacer(modifier = Modifier.width(KptTheme.spacing.md))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
            ) {
                Text(
                    text = title,
                    style = KptTheme.typography.titleSmall,
                    color = KptTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (time.isNotEmpty()) "$time; $date" else date,
                    style = KptTheme.typography.bodySmall,
                    color = KptTheme.colorScheme.secondary,
                )
            }

            Spacer(modifier = Modifier.width(KptTheme.spacing.md))

            Text(
                text = if (isCredited) {
                    "+ $transactionAmount"
                } else {
                    "- $transactionAmount"
                },
                style = KptTheme.typography.labelSmall,
                color = if (isCredited) {
                    KptTheme.colorScheme.primary
                } else {
                    KptTheme.colorScheme.error
                },
            )
        }
    }
}

@Preview
@Composable
private fun TransactionScreenItemPreview() {
    MifosXOpenBankingTheme {
        Column(modifier = Modifier.padding(KptTheme.spacing.md)) {
            TransactionScreenItem(
                title = "Add-Money Bank Card",
                date = "20-03-2020",
                time = "5:10",
                transactionAmount = "87289",
                isCredited = true,
            )
            TransactionScreenItem(
                title = "Add-Money Bank Card",
                date = "20-03-2020",
                time = "5:10",
                transactionAmount = "87289",
                isCredited = false,
            )
        }
    }
}
