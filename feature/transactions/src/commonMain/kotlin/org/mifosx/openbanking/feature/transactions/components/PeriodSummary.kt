/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.transactions.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.transactions.TransactionsTestTags
import org.mifosx.openbanking.feature.transactions.generated.resources.Res
import org.mifosx.openbanking.feature.transactions.generated.resources.feature_transactions_money_in
import org.mifosx.openbanking.feature.transactions.generated.resources.feature_transactions_money_out
import template.core.base.designsystem.theme.KptTheme

private val DividerHeight = 36.dp

/** Period summary strip: money-in (primary) and money-out (error) totals for the current view. */
@Composable
internal fun PeriodSummary(
    moneyInLabel: String,
    moneyOutLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().testTag(TransactionsTestTags.PERIOD_SUMMARY)) {
        Surface(color = KptTheme.colorScheme.surfaceContainerLow) {
            Row(modifier = Modifier.fillMaxWidth().padding(KptTheme.spacing.md)) {
                PeriodColumn(
                    label = stringResource(Res.string.feature_transactions_money_in),
                    value = moneyInLabel,
                    valueColor = KptTheme.colorScheme.primary,
                    valueTag = TransactionsTestTags.MONEY_IN,
                    modifier = Modifier.weight(1f),
                )
                VerticalDivider(modifier = Modifier.height(DividerHeight), color = KptTheme.colorScheme.outlineVariant)
                PeriodColumn(
                    label = stringResource(Res.string.feature_transactions_money_out),
                    value = moneyOutLabel,
                    valueColor = KptTheme.colorScheme.error,
                    valueTag = TransactionsTestTags.MONEY_OUT,
                    modifier = Modifier.weight(1f).padding(start = KptTheme.spacing.md),
                )
            }
        }
        HorizontalDivider(color = KptTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun PeriodColumn(
    label: String,
    value: String,
    valueColor: Color,
    valueTag: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = KptTheme.typography.labelMedium,
            color = KptTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = KptTheme.typography.titleMedium,
            color = valueColor,
            modifier = Modifier.testTag(valueTag),
        )
    }
}
