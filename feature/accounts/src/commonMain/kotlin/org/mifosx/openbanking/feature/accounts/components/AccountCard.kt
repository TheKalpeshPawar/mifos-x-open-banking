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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.common.formatMoney
import org.mifosx.openbanking.core.common.formatSortCode
import org.mifosx.openbanking.core.model.banking.AccountWithBalance
import org.mifosx.openbanking.feature.accounts.AccountsTestTags
import org.mifosx.openbanking.feature.accounts.generated.resources.Res
import org.mifosx.openbanking.feature.accounts.generated.resources.feature_accounts_type_credit
import org.mifosx.openbanking.feature.accounts.generated.resources.feature_accounts_type_current
import org.mifosx.openbanking.feature.accounts.generated.resources.feature_accounts_type_global_money
import org.mifosx.openbanking.feature.accounts.generated.resources.feature_accounts_type_global_wallet
import org.mifosx.openbanking.feature.accounts.generated.resources.feature_accounts_type_other
import org.mifosx.openbanking.feature.accounts.generated.resources.feature_accounts_type_savings
import org.mifosx.openbanking.feature.accounts.ui.AccountUiType
import template.core.base.designsystem.theme.KptTheme

private val AccountIconSize = 40.dp
private val AccountIconInnerSize = 22.dp
private val CardBorderWidth = 1.dp

/**
 * A single account: type icon in a tinted circle, subtype label, account number, sort code and
 * balance. The card is flat with a hairline border. Tapping opens account detail.
 */
@Composable
internal fun AccountCard(
    account: AccountWithBalance,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val type = AccountUiType.fromSubtype(account.account.accountSubType)
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().testTag(AccountsTestTags.accountCard(account.account.accountId)),
        shape = KptTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = KptTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(CardBorderWidth, KptTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(KptTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AccountTypeIcon(type)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
            ) {
                Text(
                    text = stringResource(type.labelRes()),
                    style = KptTheme.typography.bodySmall,
                    color = KptTheme.colorScheme.secondary,
                )
                Text(
                    text = account.account.accountNumber,
                    style = KptTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = KptTheme.colorScheme.onBackground,
                )
                Text(
                    text = formatSortCode(account.account.sortCode),
                    style = KptTheme.typography.labelSmall,
                    color = KptTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = account.balance?.let { formatMoney(it.availableAmount, it.currency) }.orEmpty(),
                style = KptTheme.typography.labelSmall,
                color = KptTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun AccountTypeIcon(type: AccountUiType, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(AccountIconSize),
        shape = CircleShape,
        color = KptTheme.colorScheme.primaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = type.icon(),
                contentDescription = null,
                tint = KptTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(AccountIconInnerSize),
            )
        }
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
