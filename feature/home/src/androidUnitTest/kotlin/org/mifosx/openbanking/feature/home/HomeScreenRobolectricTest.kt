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
import org.mifosx.openbanking.feature.home.components.AccountSelectorSheetContent
import org.mifosx.openbanking.feature.home.ui.AccountChipUi
import org.mifosx.openbanking.feature.home.ui.HomeData
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
    fun contentStateRendersHeroAndTransactions() {
        composeRule.showHomeContent()

        composeRule.onNodeWithTag(HomeTestTags.CONTENT).assertExists()
        composeRule.onNodeWithTag(HomeTestTags.HERO_CARD).assertExists()
        composeRule.onNodeWithTag(HomeTestTags.RECENT_TRANSACTIONS).assertExists()
    }

    @Test
    fun skeletonStateRenders() {
        composeRule.setContent { HomeSkeleton() }

        composeRule.onNodeWithTag(HomeTestTags.SKELETON).assertExists()
    }

    @Test
    fun errorStateRetryDispatches() {
        var retried = false
        composeRule.setContent { HomeError(onRetry = { retried = true }) }

        composeRule.onNodeWithTag(HomeTestTags.ERROR_RETRY).performClick()

        assertTrue(retried)
    }

    @Test
    fun heroCardTapOpensTheAccountSelector() {
        var opened = false
        composeRule.showHomeContent(onOpenAccountSelector = { opened = true })

        composeRule.onNodeWithTag(HomeTestTags.HERO_CARD).performClick()

        assertTrue(opened)
    }

    @Test
    fun accountSelectorRowSelectionDispatchesAccountId() {
        var selectedId: String? = null
        composeRule.setContent {
            AccountSelectorSheetContent(
                accounts = sampleAccounts(),
                selectedAccountId = "acc-1",
                onSelectAccount = { selectedId = it },
            )
        }

        composeRule.onNodeWithTag(HomeTestTags.ACCOUNT_SELECTOR_SHEET).assertExists()
        composeRule.onNodeWithTag(HomeTestTags.accountChip("acc-2")).performClick()

        assertEquals("acc-2", selectedId)
    }
}

private const val ROBOLECTRIC_SDK = 34

private fun ComposeContentTestRule.showHomeContent(
    onSelectAccount: (String) -> Unit = {},
    onOpenAccountSelector: () -> Unit = {},
) = setContent {
    HomeContent(
        data = sampleHomeData(),
        onSelectAccount = onSelectAccount,
        onNavigateToTransactions = { _ -> },
        onNavigateToTransactionDetail = { _, _ -> },
        isAccountSelectorVisible = false,
        onOpenAccountSelector = onOpenAccountSelector,
        onDismissAccountSelector = {},
    )
}

private fun sampleAccounts(): List<AccountChipUi> = listOf(
    AccountChipUi(id = "acc-1", nickname = "Everyday"),
    AccountChipUi(id = "acc-2", nickname = "Savings"),
)

private fun sampleHomeData(): HomeData = HomeData(
    accounts = sampleAccounts(),
    selectedAccountId = "acc-1",
    accountTypeLabel = "CURRENT ACCOUNT",
    accountHolderName = "Everyday",
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
)
