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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.accounts.AccountsTestTags
import org.mifosx.openbanking.feature.accounts.generated.resources.Res
import org.mifosx.openbanking.feature.accounts.generated.resources.feature_accounts_account_count
import org.mifosx.openbanking.feature.accounts.generated.resources.feature_accounts_total_label
import template.core.base.designsystem.theme.KptTheme

/**
 * Header summary: the net balance across the PSU's accounts with a count sublabel. The total is
 * pre-formatted by the ViewModel; this only renders the strings.
 */
@Composable
internal fun AccountTotalSummary(
    totalLabel: String,
    accountCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = KptTheme.spacing.md, vertical = KptTheme.spacing.sm)
            .testTag(AccountsTestTags.TOTAL_SUMMARY),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
    ) {
        Text(
            text = stringResource(Res.string.feature_accounts_total_label),
            style = KptTheme.typography.labelMedium,
            color = KptTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = totalLabel,
            style = KptTheme.typography.headlineLarge,
            color = KptTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(Res.string.feature_accounts_account_count, accountCount),
            style = KptTheme.typography.bodyMedium,
            color = KptTheme.colorScheme.onSurfaceVariant,
        )
    }
}
