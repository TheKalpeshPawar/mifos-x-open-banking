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
import org.mifosx.openbanking.feature.accounts.ui.AccountFilter
import org.mifosx.openbanking.feature.accounts.ui.AccountRowUi
import org.mifosx.openbanking.feature.accounts.ui.AccountUiType
import org.mifosx.openbanking.feature.accounts.ui.AccountsData
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
    fun emptyStateManageConsentsDispatches() {
        var managed = false
        composeRule.setContent { AccountsEmpty(onManageConsents = { managed = true }) }

        composeRule.onNodeWithTag(AccountsTestTags.EMPTY).assertExists()
        composeRule.onNodeWithTag(AccountsTestTags.MANAGE_CONSENTS).performClick()

        assertTrue(managed)
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

    @Test
    fun consentBannerRendersAndReconfirmDispatches() {
        var reconfirmed = false
        composeRule.showContent(consentExpiring = true, onReconfirm = { reconfirmed = true })

        composeRule.onNodeWithTag(AccountsTestTags.CONSENT_BANNER).assertExists()
        composeRule.onNodeWithTag(AccountsTestTags.CONSENT_RECONFIRM).performClick()

        assertTrue(reconfirmed)
    }
}

private const val ROBOLECTRIC_SDK = 34

private fun ComposeContentTestRule.showContent(
    consentExpiring: Boolean = false,
    onFilterChange: (AccountFilter) -> Unit = {},
    onAccountClick: (String) -> Unit = {},
    onReconfirm: () -> Unit = {},
) = setContent {
    AccountsContent(
        data = sampleAccountsData(consentExpiring),
        onFilterChange = onFilterChange,
        onAccountClick = onAccountClick,
        onReconfirmConsent = onReconfirm,
    )
}

private fun sampleAccountsData(consentExpiring: Boolean): AccountsData = AccountsData(
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
    isConsentExpiring = consentExpiring,
    consentDaysRemaining = 14,
)
