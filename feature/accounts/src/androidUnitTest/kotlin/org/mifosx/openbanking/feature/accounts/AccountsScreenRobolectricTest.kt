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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.core.model.banking.AccountBalance
import org.mifosx.openbanking.core.model.banking.AccountWithBalance
import org.mifosx.openbanking.core.model.banking.BankAccount
import org.mifosx.openbanking.feature.accounts.ui.AccountFilter
import org.mifosx.openbanking.feature.accounts.ui.AccountsData
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * The accounts states against the real Android Compose runtime under Robolectric — JVM, no device.
 * The sibling `AccountsScreenInstrumentedTest` runs the same assertions on a real device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [ROBOLECTRIC_SDK])
class AccountsScreenRobolectricTest {

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

private const val ROBOLECTRIC_SDK = 34

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
                accountSubType = "CurrentAccount",
                currency = "GBP",
                sortCode = "400515",
                accountNumber = "12345678",
            ),
            balance = AccountBalance("acc-current", "GBP", "2847.63", "2847.63"),
        ),
        AccountWithBalance(
            account = BankAccount(
                accountId = "acc-credit",
                accountHolderName = "Platinum Mastercard",
                accountSubType = "CreditCard",
                currency = "GBP",
                sortCode = "",
                accountNumber = "7654",
                rawIdentification = "•••• 7654",
            ),
            balance = AccountBalance("acc-credit", "GBP", "342.18", "342.18"),
        ),
    ),
    activeFilter = AccountFilter.ALL,
)
