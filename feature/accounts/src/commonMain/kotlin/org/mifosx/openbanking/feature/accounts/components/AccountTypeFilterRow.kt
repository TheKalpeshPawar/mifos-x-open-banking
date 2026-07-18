/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accounts.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.accounts.AccountsTestTags
import org.mifosx.openbanking.feature.accounts.generated.resources.Res
import org.mifosx.openbanking.feature.accounts.generated.resources.feature_accounts_filter_all
import org.mifosx.openbanking.feature.accounts.generated.resources.feature_accounts_filter_credit
import org.mifosx.openbanking.feature.accounts.generated.resources.feature_accounts_filter_current
import org.mifosx.openbanking.feature.accounts.generated.resources.feature_accounts_filter_global
import org.mifosx.openbanking.feature.accounts.generated.resources.feature_accounts_filter_savings
import org.mifosx.openbanking.feature.accounts.ui.AccountFilter
import template.core.base.designsystem.theme.KptTheme

/** Horizontal, single-select chip row filtering the account list by type. Filtering is client-side. */
@Composable
internal fun AccountTypeFilterRow(
    active: AccountFilter,
    onFilterChange: (AccountFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = KptTheme.spacing.md, vertical = KptTheme.spacing.xs)
            .testTag(AccountsTestTags.FILTER_ROW),
        horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
    ) {
        AccountFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == active,
                onClick = { onFilterChange(filter) },
                label = { Text(stringResource(filter.labelRes())) },
                modifier = Modifier.testTag(AccountsTestTags.filterChip(filter)),
            )
        }
    }
}

private fun AccountFilter.labelRes(): StringResource = when (this) {
    AccountFilter.ALL -> Res.string.feature_accounts_filter_all
    AccountFilter.CURRENT -> Res.string.feature_accounts_filter_current
    AccountFilter.SAVINGS -> Res.string.feature_accounts_filter_savings
    AccountFilter.CREDIT -> Res.string.feature_accounts_filter_credit
    AccountFilter.GLOBAL -> Res.string.feature_accounts_filter_global
}
