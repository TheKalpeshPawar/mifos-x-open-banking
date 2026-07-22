/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.mifosx.openbanking.feature.home.components.AccountSwitcherRow
import org.mifosx.openbanking.feature.home.components.HeroBalanceCard
import org.mifosx.openbanking.feature.home.components.QuickActionsRow
import org.mifosx.openbanking.feature.home.components.RecentTransactionsSection
import org.mifosx.openbanking.feature.home.components.SpendingSnapshotCard
import org.mifosx.openbanking.feature.home.ui.HomeData
import template.core.base.designsystem.theme.KptTheme

/**
 * Content state of the home dashboard: account switcher, hero balance card, quick actions, recent
 * transactions, and the optional spending summary.
 */
@Composable
internal fun HomeContent(
    data: HomeData,
    onSelectAccount: (String) -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToAccountDetail: (accountId: String) -> Unit,
    onNavigateToStatements: (accountId: String) -> Unit,
    onNavigateToConsents: () -> Unit,
    onNavigateToTransactionDetail: (transactionId: String, accountId: String) -> Unit,
    onNavigateToSpending: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(KptTheme.spacing.md)
            .testTag(HomeTestTags.CONTENT),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.md),
    ) {
        AccountSwitcherRow(
            accounts = data.accounts,
            selectedAccountId = data.selectedAccountId,
            onSelectAccount = onSelectAccount,
        )
        HeroBalanceCard(
            accountTypeLabel = data.accountTypeLabel,
            nickname = data.accountNickname,
            balanceLabel = data.balanceLabel,
            availableAmountLabel = data.availableAmountLabel,
            accountNumberLabel = data.accountNumberLabel,
            onClick = { onNavigateToAccountDetail(data.selectedAccountId) },
        )
        QuickActionsRow(
            onTransactions = onNavigateToTransactions,
            onStatements = { onNavigateToStatements(data.selectedAccountId) },
            onConsents = onNavigateToConsents,
            statementsEnabled = data.statementsAvailable,
        )
        RecentTransactionsSection(
            transactions = data.recentTransactions,
            onViewAll = onNavigateToTransactions,
            onTransactionClick = { transactionId ->
                onNavigateToTransactionDetail(transactionId, data.selectedAccountId)
            },
        )
        data.spending?.let { spending ->
            SpendingSnapshotCard(spending = spending, onClick = onNavigateToSpending)
        }
    }
}
