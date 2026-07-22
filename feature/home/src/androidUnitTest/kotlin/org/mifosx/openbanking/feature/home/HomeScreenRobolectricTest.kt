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

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.feature.home.ui.AccountChipUi
import org.mifosx.openbanking.feature.home.ui.HomeData
import org.mifosx.openbanking.feature.home.ui.SpendingRowUi
import org.mifosx.openbanking.feature.home.ui.TransactionRowUi
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The home dashboard states against the real Android Compose runtime under Robolectric — JVM, no
 * device. The sibling `HomeScreenInstrumentedTest` runs the same assertions on a real device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [ROBOLECTRIC_SDK])
class HomeScreenRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun contentStateRendersHeroQuickActionsTransactionsAndSpending() {
        composeRule.showHomeContent()

        composeRule.onNodeWithTag(HomeTestTags.CONTENT).assertExists()
        composeRule.onNodeWithTag(HomeTestTags.HERO_CARD).assertExists()
        composeRule.onNodeWithTag(HomeTestTags.QUICK_ACTIONS).assertExists()
        composeRule.onNodeWithTag(HomeTestTags.RECENT_TRANSACTIONS).assertExists()
        composeRule.onNodeWithTag(HomeTestTags.SPENDING_CARD).assertExists()
    }

    @Test
    fun skeletonStateRenders() {
        composeRule.setContent { HomeSkeleton() }

        composeRule.onNodeWithTag(HomeTestTags.SKELETON).assertExists()
    }

    @Test
    fun emptyStateConnectBankDispatches() {
        var connected = false
        composeRule.setContent { HomeEmpty(onConnectBank = { connected = true }) }

        composeRule.onNodeWithTag(HomeTestTags.CONNECT_BANK).performClick()

        assertTrue(connected)
    }

    @Test
    fun errorStateRetryDispatches() {
        var retried = false
        composeRule.setContent { HomeError(onRetry = { retried = true }) }

        composeRule.onNodeWithTag(HomeTestTags.ERROR_RETRY).performClick()

        assertTrue(retried)
    }

    @Test
    fun accountChipSelectionDispatchesAccountId() {
        var selectedId: String? = null
        composeRule.showHomeContent(onSelectAccount = { selectedId = it })

        composeRule.onNodeWithTag(HomeTestTags.accountChip("acc-2")).performClick()

        assertEquals("acc-2", selectedId)
    }
}

private const val ROBOLECTRIC_SDK = 34

private fun ComposeContentTestRule.showHomeContent(onSelectAccount: (String) -> Unit = {}) = setContent {
    HomeContent(
        data = sampleHomeData(),
        onSelectAccount = onSelectAccount,
        onNavigateToTransactions = {},
        onNavigateToAccountDetail = {},
        onNavigateToStatements = {},
        onNavigateToConsents = {},
        onNavigateToTransactionDetail = { _, _ -> },
        onNavigateToSpending = {},
    )
}

private fun sampleHomeData(): HomeData = HomeData(
    accounts = listOf(
        AccountChipUi(id = "acc-1", nickname = "Everyday"),
        AccountChipUi(id = "acc-2", nickname = "Savings"),
    ),
    selectedAccountId = "acc-1",
    accountTypeLabel = "CURRENT ACCOUNT",
    accountNickname = "Everyday",
    balanceLabel = "£2,900.00",
    availableAmountLabel = "£2,847.63",
    accountNumberLabel = "40-05-15  12345678",
    recentTransactions = listOf(
        TransactionRowUi(
            id = "t1",
            description = "TESCO STORES",
            dateLabel = "27 Jun",
            amountLabel = "- £42.17",
            isCredit = false,
        ),
    ),
    spending = SpendingRowUi(totalLabel = "£1,204.00", topCategory = "Groceries"),
    statementsAvailable = false,
)
