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

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.model.banking.TransactionCategory
import org.mifosx.openbanking.feature.transactions.TransactionsTestTags
import org.mifosx.openbanking.feature.transactions.generated.resources.Res
import org.mifosx.openbanking.feature.transactions.generated.resources.feature_transactions_category_dining
import org.mifosx.openbanking.feature.transactions.generated.resources.feature_transactions_category_groceries
import org.mifosx.openbanking.feature.transactions.generated.resources.feature_transactions_category_other
import org.mifosx.openbanking.feature.transactions.generated.resources.feature_transactions_category_shopping
import org.mifosx.openbanking.feature.transactions.generated.resources.feature_transactions_category_subscriptions
import org.mifosx.openbanking.feature.transactions.generated.resources.feature_transactions_category_transfer
import org.mifosx.openbanking.feature.transactions.generated.resources.feature_transactions_category_transport
import org.mifosx.openbanking.feature.transactions.generated.resources.feature_transactions_credit_icon_description
import org.mifosx.openbanking.feature.transactions.generated.resources.feature_transactions_debit_icon_description
import org.mifosx.openbanking.feature.transactions.generated.resources.feature_transactions_pending
import org.mifosx.openbanking.feature.transactions.ui.TransactionRowUi
import template.core.base.designsystem.theme.KptTheme

private val IconCircleSize = 40.dp
private val LeadingIconSize = 20.dp
private const val ICON_BG_ALPHA = 0.12f

/** A single transaction row: direction icon, merchant + category + pending, and the signed amount. */
@Composable
internal fun TransactionRow(
    row: TransactionRowUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = if (row.isCredit) KptTheme.colorScheme.primary else KptTheme.colorScheme.error
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(TransactionsTestTags.row(row.key))
            .padding(horizontal = KptTheme.spacing.md, vertical = KptTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(IconCircleSize).clip(CircleShape).background(accent.copy(alpha = ICON_BG_ALPHA)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (row.isCredit) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                contentDescription = stringResource(
                    if (row.isCredit) {
                        Res.string.feature_transactions_credit_icon_description
                    } else {
                        Res.string.feature_transactions_debit_icon_description
                    },
                ),
                tint = accent,
                modifier = Modifier.size(LeadingIconSize),
            )
        }
        Spacer(Modifier.width(KptTheme.spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.description,
                style = KptTheme.typography.titleSmall,
                color = KptTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
                modifier = Modifier.padding(top = KptTheme.spacing.xs),
            ) {
                CategoryChip(row.category)
                if (row.isPending) PendingBadge(rowKey = row.key)
            }
        }
        Spacer(Modifier.width(KptTheme.spacing.sm))
        Text(
            text = row.amountLabel,
            style = KptTheme.typography.titleSmall,
            color = accent,
        )
    }
}

@Composable
private fun CategoryChip(category: TransactionCategory) {
    Surface(
        color = KptTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(percent = 50),
    ) {
        Text(
            text = categoryLabel(category),
            style = KptTheme.typography.labelSmall,
            color = KptTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = KptTheme.spacing.sm, vertical = 2.dp),
        )
    }
}

@Composable
private fun PendingBadge(rowKey: String) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(percent = 50),
        border = BorderStroke(1.dp, KptTheme.colorScheme.outline),
        modifier = Modifier.testTag(TransactionsTestTags.pendingBadge(rowKey)),
    ) {
        Text(
            text = stringResource(Res.string.feature_transactions_pending),
            style = KptTheme.typography.labelSmall,
            color = KptTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = KptTheme.spacing.sm, vertical = 2.dp),
        )
    }
}

@Composable
private fun categoryLabel(category: TransactionCategory): String = stringResource(
    when (category) {
        TransactionCategory.GROCERIES -> Res.string.feature_transactions_category_groceries
        TransactionCategory.DINING -> Res.string.feature_transactions_category_dining
        TransactionCategory.SUBSCRIPTIONS -> Res.string.feature_transactions_category_subscriptions
        TransactionCategory.SHOPPING -> Res.string.feature_transactions_category_shopping
        TransactionCategory.TRANSPORT -> Res.string.feature_transactions_category_transport
        TransactionCategory.TRANSFER -> Res.string.feature_transactions_category_transfer
        TransactionCategory.OTHER -> Res.string.feature_transactions_category_other
    },
)
