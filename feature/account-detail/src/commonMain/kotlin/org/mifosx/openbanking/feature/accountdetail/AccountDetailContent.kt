/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accountdetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.core.model.banking.AccountDetail
import org.mifosx.openbanking.feature.accountdetail.components.AccountDescriptionCard
import org.mifosx.openbanking.feature.accountdetail.components.AccountHeaderCard
import org.mifosx.openbanking.feature.accountdetail.components.ExploreOptionsColumn
import org.mifosx.openbanking.feature.accountdetail.generated.resources.Res
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_balances_empty_accessibility
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_balances_empty_body
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_balances_empty_title
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_balances_list_accessibility
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_section_balances
import org.mifosx.openbanking.feature.accountdetail.generated.resources.feature_account_detail_section_explore
import org.mifosx.openbanking.feature.accountdetail.ui.BalanceRowUi
import template.core.base.designsystem.theme.KptTheme

/**
 * The account-detail body: header card, Open Banking assurance badge, the typed balance rows and
 * the Explore options column.
 *
 * An account with no balance rows renders the empty block in place of the list while keeping the
 * header and — importantly — the Explore options. Dropping them here would strand the account: this
 * screen is the only route to its standing orders and direct debits. That reasoning still holds for
 * whichever chips [availableChips] leaves in.
 */
@Composable
internal fun AccountDetailContent(
    detail: AccountDetail,
    balances: List<BalanceRowUi>,
    availableChips: Set<AccountDetailChip>,
    onChipClick: (AccountDetailChip) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(KptTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
    ) {
        AccountHeaderCard(detail = detail)

        // Omitted rather than drawn empty — several sandbox accounts carry no Description at all.
        if (detail.description.isNotBlank()) {
            AccountDescriptionCard(description = detail.description)
        }

        SectionHeader(
            text = stringResource(Res.string.feature_account_detail_section_balances),
            testTag = AccountDetailTestTags.BALANCES_HEADER,
        )

        if (balances.isEmpty()) {
            BalancesEmptyBlock()
        } else {
            BalancesList(balances = balances)
        }

        SectionHeader(
            text = stringResource(Res.string.feature_account_detail_section_explore),
            testTag = AccountDetailTestTags.EXPLORE_HEADER,
        )

        ExploreOptionsColumn(availableChips = availableChips, onChipClick = onChipClick)
    }
}

@Composable
private fun SectionHeader(text: String, testTag: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = KptTheme.typography.labelLarge,
        color = KptTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .padding(top = KptTheme.spacing.sm, bottom = KptTheme.spacing.xs)
            .testTag(testTag),
    )
}

/**
 * Every balance row at equal weight — there is no hero figure here. The amount is monospaced and
 * tinted primary so the numbers align and read as data rather than prose.
 */
@Composable
private fun BalancesList(balances: List<BalanceRowUi>, modifier: Modifier = Modifier) {
    val listDescription = stringResource(Res.string.feature_account_detail_balances_list_accessibility)
    Surface(
        color = KptTheme.colorScheme.surfaceContainer,
        shape = KptTheme.shapes.medium,
        border = BorderStroke(DesignToken.strokes.hairline, KptTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .testTag(AccountDetailTestTags.BALANCES_LIST)
            .semantics { contentDescription = listDescription },
    ) {
        Column {
            balances.forEachIndexed { index, row ->
                BalanceRow(row = row)
                if (index != balances.lastIndex) {
                    HorizontalDivider(color = KptTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun BalanceRow(row: BalanceRowUi, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = DesignToken.sizes.cardRow)
            .padding(horizontal = KptTheme.spacing.md, vertical = KptTheme.spacing.md)
            .testTag(AccountDetailTestTags.balanceRow(row.type)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = row.type,
            style = KptTheme.typography.bodyLarge,
            color = KptTheme.colorScheme.onSurface,
        )
        Text(
            text = row.amountLabel,
            style = KptTheme.typography.bodyLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
            ),
            color = KptTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun BalancesEmptyBlock(modifier: Modifier = Modifier) {
    val description = stringResource(Res.string.feature_account_detail_balances_empty_accessibility)
    Surface(
        color = KptTheme.colorScheme.surfaceContainer,
        shape = KptTheme.shapes.medium,
        border = BorderStroke(DesignToken.strokes.hairline, KptTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .testTag(AccountDetailTestTags.BALANCES_EMPTY_STATE)
            .semantics { contentDescription = description },
    ) {
        Column(
            modifier = Modifier.padding(KptTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
        ) {
            Text(
                text = stringResource(Res.string.feature_account_detail_balances_empty_title),
                style = KptTheme.typography.titleMedium,
                color = KptTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(Res.string.feature_account_detail_balances_empty_body),
                style = KptTheme.typography.bodyMedium,
                color = KptTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
