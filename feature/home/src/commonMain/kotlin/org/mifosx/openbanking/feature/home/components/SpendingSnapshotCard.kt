/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.home.HomeTestTags
import org.mifosx.openbanking.feature.home.generated.resources.Res
import org.mifosx.openbanking.feature.home.generated.resources.feature_home_spending_open_desc
import org.mifosx.openbanking.feature.home.generated.resources.feature_home_spending_title
import org.mifosx.openbanking.feature.home.generated.resources.feature_home_spending_top
import org.mifosx.openbanking.feature.home.generated.resources.feature_home_spending_total
import org.mifosx.openbanking.feature.home.ui.SpendingRowUi
import template.core.base.designsystem.theme.KptTheme

/**
 * Month-to-date spending summary card with a pre-formatted total and an optional top-category label.
 * Tapping the card opens spending insights via [onClick].
 */
@Composable
internal fun SpendingSnapshotCard(
    spending: SpendingRowUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(KptTheme.colorScheme.surfaceContainerHigh, KptTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(KptTheme.spacing.md)
            .testTag(HomeTestTags.SPENDING_CARD),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs)) {
            Text(
                text = stringResource(Res.string.feature_home_spending_title),
                style = KptTheme.typography.titleSmall,
                color = KptTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(Res.string.feature_home_spending_total),
                style = KptTheme.typography.bodySmall,
                color = KptTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = spending.totalLabel,
                style = KptTheme.typography.headlineMedium,
                color = KptTheme.colorScheme.error,
            )
            if (spending.topCategory.isNotBlank()) {
                Text(
                    text = stringResource(Res.string.feature_home_spending_top, spending.topCategory),
                    style = KptTheme.typography.bodySmall,
                    color = KptTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = stringResource(Res.string.feature_home_spending_open_desc),
            tint = KptTheme.colorScheme.onSurfaceVariant,
        )
    }
}
