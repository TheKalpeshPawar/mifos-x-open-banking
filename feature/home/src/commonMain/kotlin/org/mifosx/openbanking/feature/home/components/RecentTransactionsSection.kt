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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.feature.home.HomeTestTags
import org.mifosx.openbanking.feature.home.generated.resources.Res
import org.mifosx.openbanking.feature.home.generated.resources.feature_home_recent_transactions
import org.mifosx.openbanking.feature.home.generated.resources.feature_home_view_all
import org.mifosx.openbanking.feature.home.ui.TransactionRowUi
import template.core.base.designsystem.theme.KptTheme

/**
 * The recent-transactions section: a header with a "View all" action, followed by a short list of
 * pre-formatted transaction rows. Each row opens transaction detail via [onTransactionClick].
 */
@Composable
internal fun RecentTransactionsSection(
    transactions: List<TransactionRowUi>,
    onViewAll: () -> Unit,
    onTransactionClick: (transactionId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().testTag(HomeTestTags.RECENT_TRANSACTIONS),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.feature_home_recent_transactions),
                style = KptTheme.typography.titleSmall,
                color = KptTheme.colorScheme.onSurface,
            )
            TextButton(onClick = onViewAll) {
                Text(stringResource(Res.string.feature_home_view_all), style = KptTheme.typography.labelLarge)
            }
        }
        transactions.forEach { transaction ->
            TransactionRow(transaction = transaction, onClick = { onTransactionClick(transaction.id) })
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: TransactionRowUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = KptTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
    ) {
        Icon(
            imageVector = Icons.Filled.ShoppingCart,
            contentDescription = null,
            tint = KptTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(DesignToken.sizes.iconExtraLarge)
                .background(KptTheme.colorScheme.surfaceContainerHighest, CircleShape)
                .padding(KptTheme.spacing.sm),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.description,
                style = KptTheme.typography.bodyMedium,
                color = KptTheme.colorScheme.onSurface,
            )
            Text(
                text = transaction.dateLabel,
                style = KptTheme.typography.bodySmall,
                color = KptTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = transaction.amountLabel,
            style = KptTheme.typography.labelLarge,
            color = if (transaction.isCredit) KptTheme.colorScheme.primary else KptTheme.colorScheme.error,
        )
    }
}
