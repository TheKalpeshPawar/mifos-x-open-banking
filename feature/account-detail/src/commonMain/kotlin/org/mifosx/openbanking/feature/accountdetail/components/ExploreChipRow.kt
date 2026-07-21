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

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LocalAtm
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
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

private val CHIP_GAP = 8.dp
private val CHIP_ICON_SIZE = 18.dp

/**
 * Horizontally scrolling row of the account sub-screens this account can reach.
 *
 * Order is [AccountDetailChip]'s declaration order, so Standing Orders sits before Direct Debits
 * as designed. Each chip hands the caller its destination; the screen supplies the `accountId`.
 *
 * @param availableChips Which destinations to render. Chips the account's HSBC product cannot serve
 *   are omitted rather than disabled — a savings account has no standing orders to show, so a
 *   greyed-out chip would only advertise a dead end.
 */
@Composable
internal fun ExploreChipRow(
    availableChips: Set<AccountDetailChip>,
    onChipClick: (AccountDetailChip) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowDescription = stringResource(Res.string.feature_account_detail_chip_row_accessibility)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 6.dp)
            .testTag(AccountDetailTestTags.CHIP_ROW)
            .semantics { contentDescription = rowDescription },
        horizontalArrangement = Arrangement.spacedBy(CHIP_GAP),
    ) {
        // Iterate `entries` and filter, never iterate `availableChips` — a Set has no guaranteed
        // order, and the UI suites assert declaration order.
        AccountDetailChip.entries.filter { it in availableChips }.forEach { chip ->
            ExploreChip(chip = chip, onClick = { onChipClick(chip) })
        }
    }
}

@Composable
private fun ExploreChip(
    chip: AccountDetailChip,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(chip.labelResource())
    val description = stringResource(chip.accessibilityResource())
    AssistChip(
        onClick = onClick,
        label = { Text(text = label, style = MaterialTheme.typography.labelLarge) },
        leadingIcon = {
            Icon(
                imageVector = chip.icon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(CHIP_ICON_SIZE),
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            labelColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = modifier
            .testTag(AccountDetailTestTags.chip(chip))
            .semantics { contentDescription = description },
    )
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
