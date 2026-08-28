/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accounts

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.core.common.AccountScheme
import org.mifosx.openbanking.core.model.banking.AccountBalance
import org.mifosx.openbanking.core.model.banking.AccountWithBalance
import org.mifosx.openbanking.core.model.banking.BankAccount
import org.mifosx.openbanking.feature.accounts.ui.AccountFilter
import org.mifosx.openbanking.feature.accounts.ui.AccountsData
import kotlin.test.assertEquals

/**
 * The accounts states on a real device / emulator. The same surfaces are covered device-free by
 * `AccountsScreenRobolectricTest`; this exercises them against the on-device Compose runtime.
 */
@RunWith(AndroidJUnit4::class)
class AccountsScreenInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun contentRendersSummaryFilterAndCards() {
        composeRule.showContent()

        composeRule.onNodeWithTag(AccountsTestTags.CONTENT).assertExists()
        composeRule.onNodeWithTag(AccountsTestTags.FILTER_ROW).assertExists()
        composeRule.onNodeWithTag(AccountsTestTags.accountCard("acc-current")).assertExists()
    }

    @Test
    fun filterChipDispatchesFilter() {
        var selected: AccountFilter? = null
        composeRule.showContent(onFilterChange = { selected = it })

        composeRule.onNodeWithTag(AccountsTestTags.filterChip(AccountFilter.CREDIT)).performClick()

        assertEquals(AccountFilter.CREDIT, selected)
    }

    @Test
    fun accountCardClickDispatchesAccountId() {
        var clickedId: String? = null
        composeRule.showContent(onAccountClick = { clickedId = it })

        composeRule.onNodeWithTag(AccountsTestTags.accountCard("acc-current")).performClick()

        assertEquals("acc-current", clickedId)
    }
}

private fun ComposeContentTestRule.showContent(
    onFilterChange: (AccountFilter) -> Unit = {},
    onAccountClick: (String) -> Unit = {},
) = setContent {
    AccountsContent(
        data = sampleAccountsData(),
        onFilterChange = onFilterChange,
        onAccountClick = onAccountClick,
    )
}

private fun sampleAccountsData(): AccountsData = AccountsData(
    rows = listOf(
        AccountWithBalance(
            account = BankAccount(
                accountId = "acc-current",
                accountHolderName = "Everyday Current",
                accountTypeCode = "CACC",
                currency = "GBP",
                identification = "40051512345678",
                scheme = AccountScheme.SortCode,
            ),
            balance = AccountBalance("acc-current", "GBP", "2847.63", "2847.63"),
        ),
        AccountWithBalance(
            account = BankAccount(
                accountId = "acc-credit",
                accountHolderName = "Platinum Mastercard",
                accountTypeCode = "CARD",
                currency = "GBP",
                identification = "•••• 7654",
                scheme = AccountScheme.Pan,
            ),
            balance = AccountBalance("acc-credit", "GBP", "342.18", "342.18"),
        ),
    ),
    activeFilter = AccountFilter.ALL,
)
