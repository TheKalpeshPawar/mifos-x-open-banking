/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.common.AccountScheme
import org.mifosx.openbanking.core.common.formatAccountIdentifier
import org.mifosx.openbanking.core.common.formatMoney
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import org.mifosx.openbanking.core.model.banking.AccountBalance
import org.mifosx.openbanking.core.model.banking.AccountWithBalance
import org.mifosx.openbanking.core.model.banking.BankAccount
import org.mifosx.openbanking.core.model.hsbcProduct.HsbcProductType
import org.mifosx.openbanking.core.ui.account.accountTypeLabel
import template.core.base.designsystem.theme.KptTheme

/**
 * One account in a list: product icon, product label, holder name, identification, and the available
 * balance in the trailing slot. Tapping the row calls [onClick].
 *
 * @param account The account and its balance.
 * @param onClick Invoked when the row is tapped.
 * @param testTag Node tag for the row, or null to carry none.
 */
@Composable
fun MifosAccountRow(
    account: AccountWithBalance,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    val product = HsbcProductType.resolve(
        accountTypeCode = account.account.accountTypeCode,
        description = account.account.description,
    )
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .then(if (testTag == null) Modifier else Modifier.testTag(testTag)),
        shape = KptTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = KptTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = KptTheme.elevation.level1),
        border = BorderStroke(DesignToken.strokes.hairline, KptTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(KptTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ProductIcon(product)
            Spacer(Modifier.width(KptTheme.spacing.md))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
            ) {
                Text(
                    text = accountTypeLabel(product),
                    style = KptTheme.typography.labelMedium,
                    color = KptTheme.colorScheme.onSurfaceVariant,
                )

                Text(
                    text = account.account.accountHolderName,
                    style = KptTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = KptTheme.colorScheme.onSurface,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )

                Text(
                    text = formatAccountIdentifier(account.account.scheme, account.account.identification),
                    style = KptTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = KptTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(KptTheme.spacing.sm))
            Text(
                text = account.balance?.let { formatMoney(it.availableAmount, it.currency) }.orEmpty(),
                style = KptTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = KptTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ProductIcon(product: HsbcProductType, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(DesignToken.sizes.avatarSmall),
        shape = DesignToken.shapes.circle,
        color = KptTheme.colorScheme.primaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = product.icon(),
                contentDescription = null,
                tint = KptTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(DesignToken.sizes.iconMedium),
            )
        }
    }
}

private fun HsbcProductType.icon(): ImageVector = when (this) {
    HsbcProductType.PersonalCurrentAccount -> Icons.Filled.AccountBalance
    HsbcProductType.Savings -> Icons.Filled.Savings
    HsbcProductType.CreditCard -> Icons.Filled.CreditCard
    HsbcProductType.ForeignCurrency -> Icons.Filled.CurrencyExchange
    HsbcProductType.GlobalMoney -> Icons.Filled.Public
    HsbcProductType.Unknown -> Icons.Filled.AccountBalanceWallet
}

@Preview
@Composable
private fun MifosAccountRowPreview() {
    MifosXOpenBankingTheme {
        MifosAccountRow(
            account = AccountWithBalance(
                account = BankAccount(
                    accountId = "acc-current",
                    accountTypeCode = "CACC",
                    currency = "GBP",
                    identification = "80200110203349",
                    scheme = AccountScheme.SortCode,
                    description = "Description of the account",
                    accountHolderName = "Mr Nico",
                ),
                balance = AccountBalance(
                    accountId = "acc-current",
                    currency = "GBP",
                    currentAmount = "2900.00",
                    availableAmount = "2847.63",
                ),
            ),
            onClick = {},
        )
    }
}
