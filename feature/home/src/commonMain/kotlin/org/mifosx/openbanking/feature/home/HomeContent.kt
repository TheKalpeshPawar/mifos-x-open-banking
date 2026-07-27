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
import org.mifosx.openbanking.core.ui.account.accountDisplayName
import org.mifosx.openbanking.feature.home.components.AccountSelectorSheet
import org.mifosx.openbanking.feature.home.components.HeroBalanceCard
import org.mifosx.openbanking.feature.home.components.RecentTransactionsSection
import org.mifosx.openbanking.feature.home.ui.HomeData
import template.core.base.designsystem.theme.KptTheme

/**
 * Content state of the home dashboard: hero balance card and recent transactions. Tapping the hero
 * card opens the account selector sheet — the only way to switch account from here, now that the
 * chip row is gone. Account detail is reached from the Accounts tab instead.
 */
@Composable
internal fun HomeContent(
    data: HomeData,
    onSelectAccount: (String) -> Unit,
    onNavigateToTransactions: (accountId: String) -> Unit,
    onNavigateToTransactionDetail: (transactionId: String, accountId: String) -> Unit,
    isAccountSelectorVisible: Boolean,
    onOpenAccountSelector: () -> Unit,
    onDismissAccountSelector: () -> Unit,
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
        HeroBalanceCard(
            accountTypeLabel = data.accountTypeLabel,
            nickname = accountDisplayName(
                nickname = data.accountNickname,
                accountSubType = data.accountSubType,
                accountNumber = data.accountNumber,
                rawIdentification = data.rawIdentification,
            ),
            balanceLabel = data.balanceLabel,
            availableAmountLabel = data.availableAmountLabel,
            accountNumberLabel = data.accountNumberLabel,
            onClick = onOpenAccountSelector,
        )
        RecentTransactionsSection(
            transactions = data.recentTransactions,
            onViewAll = { onNavigateToTransactions(data.selectedAccountId) },
            onTransactionClick = { transactionId ->
                onNavigateToTransactionDetail(transactionId, data.selectedAccountId)
            },
        )
    }

    if (isAccountSelectorVisible) {
        AccountSelectorSheet(
            accounts = data.accounts,
            selectedAccountId = data.selectedAccountId,
            onSelectAccount = onSelectAccount,
            onDismiss = onDismissAccountSelector,
        )
    }
}
