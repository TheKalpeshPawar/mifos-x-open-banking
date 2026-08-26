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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The home dashboard against the real Android Compose runtime under Robolectric — JVM, no device. The
 * sibling `HomeScreenInstrumentedTest` runs the same assertions on a real device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [ROBOLECTRIC_SDK])
class HomeScreenRobolectricTest {

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

        composeRule.onNodeWithTag(HomeTestTags.accountRow(HomeFixtures.CURRENT_ID))
            .performScrollTo()
            .performClick()

        assertEquals(HomeFixtures.CURRENT_ID, clickedId)
    }
}

private const val ROBOLECTRIC_SDK = 34

private fun ComposeContentTestRule.showHomeContent(
    onSendMoney: () -> Unit = {},
    onAccountClick: (String) -> Unit = {},
) = setContent {
    HomeContent(
        data = HomeFixtures.homeData(),
        onSendMoney = onSendMoney,
        onSchedule = {},
        onStandingOrder = {},
        onVrp = {},
        onAccountClick = onAccountClick,
        onAvatarClick = {},
    )
}
