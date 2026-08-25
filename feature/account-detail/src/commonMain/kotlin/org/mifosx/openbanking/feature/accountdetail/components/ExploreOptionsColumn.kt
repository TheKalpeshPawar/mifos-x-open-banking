/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accountdetail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LocalAtm
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.feature.accountdetail.AccountDetailChip
import org.mifosx.openbanking.feature.accountdetail.AccountDetailTestTags
import org.mifosx.openbanking.feature.accountdetail.generated.resources.Res
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_chip_atm
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_chip_atm_accessibility
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_chip_beneficiaries
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_chip_beneficiaries_accessibility
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_chip_direct_debits
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_chip_direct_debits_accessibility
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_chip_party
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_chip_party_accessibility
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_chip_product
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_chip_product_accessibility
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_chip_row_accessibility
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_chip_scheduled
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_chip_scheduled_accessibility
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_chip_standing_orders
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_chip_standing_orders_accessibility
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_chip_statements
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_chip_statements_accessibility
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_chip_transactions
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_chip_transactions_accessibility
import template.core.base.designsystem.theme.KptTheme

/**
 * Full-width, edge-to-edge column of the account sub-screens this account can reach.
 *
 * Order is [AccountDetailChip]'s declaration order, so Standing Orders sits before Direct Debits
 * as designed. Each option hands the caller its destination; the screen supplies the `accountId`.
 * A thin divider separates adjacent options; the last option has none.
 *
 * @param availableChips Which destinations to render. Destinations the account's HSBC product
 *   cannot serve are omitted rather than disabled — a savings account has no standing orders to
 *   show, so a greyed-out option would only advertise a dead end.
 */
@Composable
internal fun ExploreOptionsColumn(
    availableChips: Set<AccountDetailChip>,
    onChipClick: (AccountDetailChip) -> Unit,
    modifier: Modifier = Modifier,
) {
    val columnDescription = stringResource(Res.string.feature_account_detail_chip_row_accessibility)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(AccountDetailTestTags.CHIP_ROW)
            .semantics { contentDescription = columnDescription },
    ) {
        // Iterate `entries` and filter, never iterate `availableChips` — a Set has no guaranteed
        // order, and the UI suites assert declaration order.
        val chips = AccountDetailChip.entries.filter { it in availableChips }
        chips.forEachIndexed { index, chip ->
            ExploreOption(chip = chip, onClick = { onChipClick(chip) })
            if (index != chips.lastIndex) {
                HorizontalDivider(color = KptTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun ExploreOption(
    chip: AccountDetailChip,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(chip.labelResource())
    val description = stringResource(chip.accessibilityResource())
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(AccountDetailTestTags.chip(chip))
            .semantics { contentDescription = description }
            .padding(horizontal = KptTheme.spacing.md, vertical = KptTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
    ) {
        Icon(
            imageVector = chip.icon(),
            contentDescription = null,
            tint = KptTheme.colorScheme.primary,
            modifier = Modifier.size(DesignToken.sizes.iconMedium),
        )
        Text(
            text = label,
            style = KptTheme.typography.titleMedium,
            color = KptTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = KptTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DesignToken.sizes.iconSmall),
        )
    }
}

private fun AccountDetailChip.icon(): ImageVector = when (this) {
    AccountDetailChip.Transactions -> Icons.Outlined.ReceiptLong
    AccountDetailChip.Statements -> Icons.Outlined.Description
    AccountDetailChip.StandingOrders -> Icons.Outlined.Autorenew
    AccountDetailChip.DirectDebits -> Icons.Outlined.Subscriptions
    AccountDetailChip.ScheduledPayments -> Icons.Outlined.Schedule
    AccountDetailChip.Beneficiaries -> Icons.Outlined.People
    AccountDetailChip.AtmLocator -> Icons.Outlined.LocalAtm
    AccountDetailChip.Product -> Icons.Outlined.Description
    AccountDetailChip.Party -> Icons.Outlined.Person
}

private fun AccountDetailChip.labelResource(): StringResource = when (this) {
    AccountDetailChip.Transactions -> Res.string.feature_account_detail_chip_transactions
    AccountDetailChip.Statements -> Res.string.feature_account_detail_chip_statements
    AccountDetailChip.StandingOrders -> Res.string.feature_account_detail_chip_standing_orders
    AccountDetailChip.DirectDebits -> Res.string.feature_account_detail_chip_direct_debits
    AccountDetailChip.ScheduledPayments -> Res.string.feature_account_detail_chip_scheduled
    AccountDetailChip.Beneficiaries -> Res.string.feature_account_detail_chip_beneficiaries
    AccountDetailChip.AtmLocator -> Res.string.feature_account_detail_chip_atm
    AccountDetailChip.Product -> Res.string.feature_account_detail_chip_product
    AccountDetailChip.Party -> Res.string.feature_account_detail_chip_party
}

private fun AccountDetailChip.accessibilityResource(): StringResource = when (this) {
    AccountDetailChip.Transactions -> Res.string.feature_account_detail_chip_transactions_accessibility
    AccountDetailChip.Statements -> Res.string.feature_account_detail_chip_statements_accessibility
    AccountDetailChip.StandingOrders -> Res.string.feature_account_detail_chip_standing_orders_accessibility
    AccountDetailChip.DirectDebits -> Res.string.feature_account_detail_chip_direct_debits_accessibility
    AccountDetailChip.ScheduledPayments -> Res.string.feature_account_detail_chip_scheduled_accessibility
    AccountDetailChip.Beneficiaries -> Res.string.feature_account_detail_chip_beneficiaries_accessibility
    AccountDetailChip.AtmLocator -> Res.string.feature_account_detail_chip_atm_accessibility
    AccountDetailChip.Product -> Res.string.feature_account_detail_chip_product_accessibility
    AccountDetailChip.Party -> Res.string.feature_account_detail_chip_party_accessibility
}
