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
import org.mifosx.openbanking.feature.accounts.ui.AccountFilter
import org.mifosx.openbanking.feature.accounts.ui.AccountRowUi
import org.mifosx.openbanking.feature.accounts.ui.AccountUiType
import org.mifosx.openbanking.feature.accounts.ui.AccountsData
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The accounts states on a real device / emulator. The same surfaces are covered device-free by
 * `AccountsScreenRobolectricTest`; this exercises them against the on-device Compose runtime.
 */
@RunWith(AndroidJUnit4::class)
class AccountsScreenInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun contentRendersSummaryFilterCardsAndOwedBadge() {
        composeRule.showContent()

        composeRule.onNodeWithTag(AccountsTestTags.CONTENT).assertExists()
        composeRule.onNodeWithTag(AccountsTestTags.FILTER_ROW).assertExists()
        composeRule.onNodeWithTag(AccountsTestTags.accountCard("acc-current")).assertExists()
        composeRule.onNodeWithTag(AccountsTestTags.BALANCE_OWED_BADGE, useUnmergedTree = true).assertExists()
    }

    @Test
    fun skeletonStateRenders() {
        composeRule.setContent { AccountsSkeleton() }

        composeRule.onNodeWithTag(AccountsTestTags.SKELETON).assertExists()
    }

    @Test
    fun emptyStateRenders() {
        composeRule.setContent { AccountsEmpty() }

        composeRule.onNodeWithTag(AccountsTestTags.EMPTY).assertExists()
    }

    @Test
    fun errorStateRetryDispatches() {
        var retried = false
        composeRule.setContent { AccountsError(onRetry = { retried = true }) }

        composeRule.onNodeWithTag(AccountsTestTags.ERROR).assertExists()
        composeRule.onNodeWithTag(AccountsTestTags.ERROR_RETRY).performClick()

        assertTrue(retried)
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
        AccountRowUi(
            id = "acc-current",
            type = AccountUiType.CURRENT,
            nickname = "Everyday Current",
            identifier = "40-05-15  12345678",
            balanceLabel = "£2,847.63",
            isBalanceOwed = false,
        ),
        AccountRowUi(
            id = "acc-credit",
            type = AccountUiType.CREDIT,
            nickname = "Platinum Mastercard",
            identifier = "•••• 7654",
            balanceLabel = "£342.18",
            isBalanceOwed = true,
        ),
    ),
    activeFilter = AccountFilter.ALL,
)
