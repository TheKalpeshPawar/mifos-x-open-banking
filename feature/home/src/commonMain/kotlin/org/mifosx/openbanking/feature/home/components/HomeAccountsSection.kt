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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.mifosx.openbanking.core.common.AccountScheme
import org.mifosx.openbanking.core.designsystem.theme.MifosXOpenBankingTheme
import org.mifosx.openbanking.core.model.banking.AccountBalance
import org.mifosx.openbanking.core.model.banking.AccountWithBalance
import org.mifosx.openbanking.core.model.banking.BankAccount
import org.mifosx.openbanking.core.ui.components.MifosAccountRow
import org.mifosx.openbanking.core.ui.components.MifosSectionHeading
import org.mifosx.openbanking.feature.home.HomeTestTags
import org.mifosx.openbanking.feature.home.generated.resources.Res
import org.mifosx.openbanking.feature.home.generated.resources.feature_home_accounts
import template.core.base.designsystem.theme.KptTheme

/**
 * Every non-card account, one row each, no truncation. Renders nothing at all — heading included —
 * when the customer holds only card accounts.
 *
 * @param accounts The non-card accounts.
 * @param onAccountClick Invoked with the account id when a row is tapped.
 */
@Composable
internal fun HomeAccountsSection(
    accounts: List<AccountWithBalance>,
    onAccountClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (accounts.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth().testTag(HomeTestTags.ACCOUNTS_SECTION),
        verticalArrangement = Arrangement.spacedBy(KptTheme.spacing.sm),
    ) {
        MifosSectionHeading(
            text = stringResource(Res.string.feature_home_accounts),
            style = KptTheme.typography.titleLarge,
            color = KptTheme.colorScheme.onSurface,
        )
        accounts.forEach { account ->
            MifosAccountRow(
                account = account,
                onClick = { onAccountClick(account.account.accountId) },
                testTag = HomeTestTags.accountRow(account.account.accountId),
            )
        }
    }
}

@Preview
@Composable
private fun HomeAccountsSectionPreview() {
    MifosXOpenBankingTheme {
        HomeAccountsSection(
            accounts = listOf(
                AccountWithBalance(
                    account = BankAccount(
                        accountId = "acc-current",
                        accountTypeCode = "CACC",
                        currency = "GBP",
                        identification = "80200110203349",
                        scheme = AccountScheme.SortCode,
                        description = "Description of the account",
                        accountHolderName = "Mr Nico",
                    ),
                    balance = AccountBalance("acc-current", "GBP", "2900.00", "2847.63"),
                ),
                AccountWithBalance(
                    account = BankAccount(
                        accountId = "acc-gm",
                        accountTypeCode = "CACC",
                        currency = "GBP",
                        identification = "80119770009652",
                        scheme = AccountScheme.SortCode,
                        description = "GLOBAL MONEY ACCOUNT",
                        accountHolderName = "Global Money GMA",
                    ),
                    balance = AccountBalance("acc-gm", "GBP", "120.00", "120.00"),
                ),
            ),
            onAccountClick = {},
        )
    }
}
