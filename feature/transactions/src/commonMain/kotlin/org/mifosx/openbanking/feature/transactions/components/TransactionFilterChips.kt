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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.transactions.TransactionsTestTags
import org.mifosx.openbanking.feature.transactions.generated.resources.Res
import org.mifosx.openbanking.feature.transactions.generated.resources.feature_transactions_filter_all
import org.mifosx.openbanking.feature.transactions.generated.resources.feature_transactions_filter_date_range
import org.mifosx.openbanking.feature.transactions.generated.resources.feature_transactions_filter_money_in
import org.mifosx.openbanking.feature.transactions.generated.resources.feature_transactions_filter_money_out
import org.mifosx.openbanking.feature.transactions.ui.TransactionFilter
import template.core.base.designsystem.theme.KptTheme

/** The money-direction filter chips plus a date-range chip, horizontally scrollable. */
@Composable
internal fun TransactionFilterChips(
    active: TransactionFilter,
    dateRangeActive: Boolean,
    onFilter: (TransactionFilter) -> Unit,
    onDateRange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth().testTag(TransactionsTestTags.FILTER_ROW),
        contentPadding = PaddingValues(horizontal = KptTheme.spacing.md, vertical = KptTheme.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
    ) {
        item {
            DirectionChip(TransactionFilter.ALL, active, Res.string.feature_transactions_filter_all, null, onFilter)
        }
        item {
            DirectionChip(
                TransactionFilter.MONEY_IN,
                active,
                Res.string.feature_transactions_filter_money_in,
                Icons.Filled.ArrowDownward,
                onFilter,
            )
        }
        item {
            DirectionChip(
                TransactionFilter.MONEY_OUT,
                active,
                Res.string.feature_transactions_filter_money_out,
                Icons.Filled.ArrowUpward,
                onFilter,
            )
        }
        item {
            FilterChip(
                selected = dateRangeActive,
                onClick = onDateRange,
                label = { Text(stringResource(Res.string.feature_transactions_filter_date_range)) },
                leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                modifier = Modifier.testTag(TransactionsTestTags.FILTER_DATE_RANGE),
            )
        }
    }
}

@Composable
private fun DirectionChip(
    filter: TransactionFilter,
    active: TransactionFilter,
    label: StringResource,
    leadingIcon: ImageVector?,
    onFilter: (TransactionFilter) -> Unit,
) {
    val selected = filter == active
    FilterChip(
        selected = selected,
        onClick = { onFilter(filter) },
        label = { Text(stringResource(label)) },
        leadingIcon = {
            Icon(
                imageVector = if (selected) Icons.Filled.Check else leadingIcon ?: Icons.Filled.Check,
                contentDescription = null,
            )
        },
        modifier = Modifier.testTag(TransactionsTestTags.filterChip(filter)),
    )
}
