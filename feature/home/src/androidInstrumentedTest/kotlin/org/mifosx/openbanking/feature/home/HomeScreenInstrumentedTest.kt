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
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.core.common.AccountScheme
import org.mifosx.openbanking.core.model.banking.AccountBalance
import org.mifosx.openbanking.core.model.banking.AccountWithBalance
import org.mifosx.openbanking.core.model.banking.BankAccount
import org.mifosx.openbanking.feature.home.ui.Greeting
import org.mifosx.openbanking.feature.home.ui.HomeData
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The home dashboard on a real device / emulator. The same surfaces are covered device-free by
 * `HomeScreenRobolectricTest`; this exercises them against the on-device Compose runtime.
 *
 * androidInstrumentedTest does not see commonTest, so the fixtures are inlined here rather than taken
 * from `HomeFixtures`.
 */
@RunWith(AndroidJUnit4::class)
class HomeScreenInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun contentRendersEverySection() {
        composeRule.showHomeContent()

        composeRule.onNodeWithTag(HomeTestTags.CONTENT).assertExists()
        composeRule.onNodeWithTag(HomeTestTags.HEADER).assertExists()
        composeRule.onNodeWithTag(HomeTestTags.QUICK_ACTIONS).assertExists()
        composeRule.onNodeWithTag(HomeTestTags.CARDS_SECTION).assertExists()
        composeRule.onNodeWithTag(HomeTestTags.ACCOUNTS_SECTION).assertExists()
    }

    @Test
    fun quickActionTapDispatchesItsCallback() {
        var fired = false
        composeRule.showHomeContent(onSendMoney = { fired = true })

        composeRule.onNodeWithTag(HomeTestTags.quickAction("send_money")).performClick()

        assertTrue(fired)
    }

    @Test
    fun accountRowClickDispatchesAccountId() {
        var clickedId: String? = null
        composeRule.showHomeContent(onAccountClick = { clickedId = it })

        composeRule.onNodeWithTag(HomeTestTags.accountRow(CURRENT_ID))
            .performScrollTo()
            .performClick()

        assertEquals(CURRENT_ID, clickedId)
    }
}

private const val CURRENT_ID = "acc-current"
private const val CARD_ID = "acc-card"

private fun ComposeContentTestRule.showHomeContent(
    onSendMoney: () -> Unit = {},
    onAccountClick: (String) -> Unit = {},
) = setContent {
    HomeContent(
        data = sampleHomeData(),
        onSendMoney = onSendMoney,
        onSchedule = {},
        onStandingOrder = {},
        onVrp = {},
        onAccountClick = onAccountClick,
        onAvatarClick = {},
    )
}

private fun sampleHomeData(): HomeData = HomeData(
    accounts = listOf(
        AccountWithBalance(
            account = BankAccount(
                accountId = CURRENT_ID,
                accountTypeCode = "CACC",
                currency = "GBP",
                identification = "80200110203349",
                scheme = AccountScheme.SortCode,
                description = "Description of the account",
                accountHolderName = "Mr Nico",
            ),
            balance = AccountBalance(CURRENT_ID, "GBP", "2900.00", "2847.63"),
        ),
    ),
    cards = listOf(
        AccountWithBalance(
            account = BankAccount(
                accountId = CARD_ID,
                accountTypeCode = "CARD",
                currency = "GBP",
                identification = "xxxx-xxxx-xxxx-3456",
                scheme = AccountScheme.Pan,
                accountHolderName = "Mr Bantu",
            ),
            balance = AccountBalance(CARD_ID, "GBP", "342.18", "342.18"),
        ),
    ),
    greeting = Greeting.Morning,
)
