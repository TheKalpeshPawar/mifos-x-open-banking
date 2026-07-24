/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.mifosx.openbanking.feature.accounts.components.AccountCard
import org.mifosx.openbanking.feature.accounts.components.AccountTypeFilterRow
import org.mifosx.openbanking.feature.accounts.ui.AccountFilter
import org.mifosx.openbanking.feature.accounts.ui.AccountsData
import template.core.base.designsystem.theme.KptTheme

/**
 * Content state: the net-balance summary and type-filter chips pinned above a scrolling list of
 * account cards.
 */
@Composable
internal fun AccountsContent(
    data: AccountsData,
    onFilterChange: (AccountFilter) -> Unit,
    onAccountClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().testTag(AccountsTestTags.CONTENT)) {
        AccountTypeFilterRow(active = data.activeFilter, onFilterChange = onFilterChange)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(
                horizontal = KptTheme.spacing.md,
                vertical = KptTheme.spacing.sm,
            ),
            verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
        ) {
            items(data.rows, key = { it.id }) { row ->
                AccountCard(row = row, onClick = { onAccountClick(row.id) })
            }
        }
    }
}
