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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.feature.accounts.AccountsTestTags
import org.mifosx.openbanking.feature.accounts.generated.resources.Res
import org.mifosx.openbanking.feature.accounts.generated.resources.feature_accounts_balance_owed
import org.mifosx.openbanking.feature.accounts.generated.resources.feature_accounts_type_credit
import org.mifosx.openbanking.feature.accounts.generated.resources.feature_accounts_type_current
import org.mifosx.openbanking.feature.accounts.generated.resources.feature_accounts_type_global_money
import org.mifosx.openbanking.feature.accounts.generated.resources.feature_accounts_type_global_wallet
import org.mifosx.openbanking.feature.accounts.generated.resources.feature_accounts_type_other
import org.mifosx.openbanking.feature.accounts.generated.resources.feature_accounts_type_savings
import org.mifosx.openbanking.feature.accounts.ui.AccountRowUi
import org.mifosx.openbanking.feature.accounts.ui.AccountUiType
import template.core.base.designsystem.theme.KptTheme

private val AccountIconSize = 40.dp

/**
 * A single account row: type icon, subtype label, nickname, identifier, and the balance. Credit-card
 * balances render in the error colour with a "Balance owed" badge. Tapping opens account detail.
 */
@Composable
internal fun AccountCard(
    row: AccountRowUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().testTag(AccountsTestTags.accountCard(row.id)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(KptTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
        ) {
            Icon(
                imageVector = row.type.icon(),
                contentDescription = null,
                tint = KptTheme.colorScheme.primary,
                modifier = Modifier.size(AccountIconSize),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
            ) {
                Text(
                    text = stringResource(row.type.labelRes()),
                    style = KptTheme.typography.labelSmall,
                    color = KptTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = row.nickname,
                    style = KptTheme.typography.titleMedium,
                    color = KptTheme.colorScheme.onSurface,
                )
                Text(
                    text = row.identifier,
                    style = KptTheme.typography.bodySmall,
                    color = KptTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
            ) {
                Text(
                    text = row.balanceLabel,
                    style = KptTheme.typography.titleMedium,
                    color = if (row.isBalanceOwed) KptTheme.colorScheme.error else KptTheme.colorScheme.onSurface,
                )
                if (row.isBalanceOwed) {
                    BalanceOwedBadge()
                }
            }
        }
    }
}

@Composable
private fun BalanceOwedBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.testTag(AccountsTestTags.BALANCE_OWED_BADGE),
        color = KptTheme.colorScheme.errorContainer,
        shape = KptTheme.shapes.small,
    ) {
        Text(
            text = stringResource(Res.string.feature_accounts_balance_owed),
            style = KptTheme.typography.labelSmall,
            color = KptTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(horizontal = KptTheme.spacing.sm, vertical = KptTheme.spacing.xs),
        )
    }
}

private fun AccountUiType.icon(): ImageVector = when (this) {
    AccountUiType.CURRENT -> Icons.Filled.AccountBalance
    AccountUiType.SAVINGS -> Icons.Filled.Savings
    AccountUiType.CREDIT -> Icons.Filled.CreditCard
    AccountUiType.GLOBAL_MONEY -> Icons.Filled.Public
    AccountUiType.GLOBAL_WALLET -> Icons.Filled.CurrencyExchange
    AccountUiType.OTHER -> Icons.Filled.AccountBalanceWallet
}

private fun AccountUiType.labelRes(): StringResource = when (this) {
    AccountUiType.CURRENT -> Res.string.feature_accounts_type_current
    AccountUiType.SAVINGS -> Res.string.feature_accounts_type_savings
    AccountUiType.CREDIT -> Res.string.feature_accounts_type_credit
    AccountUiType.GLOBAL_MONEY -> Res.string.feature_accounts_type_global_money
    AccountUiType.GLOBAL_WALLET -> Res.string.feature_accounts_type_global_wallet
    AccountUiType.OTHER -> Res.string.feature_accounts_type_other
}
