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

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What each part of the home dashboard puts on screen, driven through the stateless content
 * composable.
 *
 * Assertions are on test tags, never on copy — the tags are the contract and the wording is a string
 * resource that may be reworded without breaking the journey.
 */
@OptIn(ExperimentalTestApi::class)
class HomeScreenUiTest {

    @Test
    fun contentRendersHeaderQuickActionsCardsAndAccounts() = runComposeUiTest {
        setContent { HomeContent(data = HomeFixtures.homeData(), {}, {}, {}, {}, {}, {}) }

        onNodeWithTag(HomeTestTags.CONTENT).assertIsDisplayed()
        onNodeWithTag(HomeTestTags.HEADER).assertIsDisplayed()
        onNodeWithTag(HomeTestTags.QUICK_ACTIONS).assertIsDisplayed()
        onNodeWithTag(HomeTestTags.CARDS_SECTION).assertExists()
        onNodeWithTag(HomeTestTags.ACCOUNTS_SECTION).assertExists()
    }

    @Test
    fun allFourQuickActionsArePresent() = runComposeUiTest {
        setContent { HomeContent(data = HomeFixtures.homeData(), {}, {}, {}, {}, {}, {}) }

        listOf("send_money", "schedule", "standing_order", "vrp").forEach { key ->
            onNodeWithTag(HomeTestTags.quickAction(key)).assertIsDisplayed()
        }
    }

    @Test
    fun eachQuickActionDispatchesItsOwnCallback() = runComposeUiTest {
        val fired = mutableListOf<String>()
        setContent {
            HomeContent(
                data = HomeFixtures.homeData(),
                onSendMoney = { fired += "send_money" },
                onSchedule = { fired += "schedule" },
                onStandingOrder = { fired += "standing_order" },
                onVrp = { fired += "vrp" },
                onAccountClick = {},
                onAvatarClick = {},
            )
        }

        listOf("send_money", "schedule", "standing_order", "vrp").forEach { key ->
            onNodeWithTag(HomeTestTags.quickAction(key)).performClick()
        }

        assertEquals(listOf("send_money", "schedule", "standing_order", "vrp"), fired)
    }

    @Test
    fun everyAccountIsRenderedAsItsOwnRow() = runComposeUiTest {
        setContent { HomeContent(data = HomeFixtures.homeData(), {}, {}, {}, {}, {}, {}) }

        listOf(HomeFixtures.CURRENT_ID, HomeFixtures.SAVINGS_ID, HomeFixtures.GLOBAL_MONEY_ID)
            .forEach { id ->
                onNodeWithTag(HomeTestTags.accountRow(id)).assertExists()
            }
    }

    @Test
    fun tappingAnAccountRowEmitsItsAccountId() = runComposeUiTest {
        var clicked: String? = null
        setContent {
            HomeContent(HomeFixtures.homeData(), {}, {}, {}, {}, onAccountClick = { clicked = it }, onAvatarClick = {})
        }

        onNodeWithTag(HomeTestTags.accountRow(HomeFixtures.CURRENT_ID)).performScrollTo().performClick()

        assertEquals(HomeFixtures.CURRENT_ID, clicked)
    }

    @Test
    fun tappingACardFaceEmitsItsAccountId() = runComposeUiTest {
        var clicked: String? = null
        setContent {
            HomeContent(HomeFixtures.homeData(), {}, {}, {}, {}, onAccountClick = { clicked = it }, onAvatarClick = {})
        }

        onNodeWithTag(HomeTestTags.cardFace(HomeFixtures.CARD_ID)).performScrollTo().performClick()

        assertEquals(HomeFixtures.CARD_ID, clicked)
    }

    @Test
    fun tappingTheAvatarDispatchesTheSettingsCallback() = runComposeUiTest {
        var opened = false
        setContent {
            HomeContent(HomeFixtures.homeData(), {}, {}, {}, {}, onAccountClick = {}, onAvatarClick = { opened = true })
        }

        onNodeWithTag(HomeTestTags.AVATAR).performClick()

        assertTrue(opened)
    }

    @Test
    fun theCardsSectionIsOmittedEntirelyWhenThereIsNoCard() = runComposeUiTest {
        setContent {
            HomeContent(HomeFixtures.homeData(cards = emptyList()), {}, {}, {}, {}, {}, {})
        }

        onNodeWithTag(HomeTestTags.CARDS_SECTION).assertDoesNotExist()
        onNodeWithTag(HomeTestTags.ACCOUNTS_SECTION).assertExists()
    }

    @Test
    fun theAccountsSectionIsOmittedEntirelyWhenEveryAccountIsACard() = runComposeUiTest {
        setContent {
            HomeContent(HomeFixtures.homeData(accounts = emptyList()), {}, {}, {}, {}, {}, {})
        }

        onNodeWithTag(HomeTestTags.ACCOUNTS_SECTION).assertDoesNotExist()
        onNodeWithTag(HomeTestTags.CARDS_SECTION).assertExists()
    }

    @Test
    fun theGreetingIsRenderedAndCarriesNoName() = runComposeUiTest {
        setContent { HomeContent(data = HomeFixtures.homeData(), {}, {}, {}, {}, {}, {}) }

        onNodeWithTag(HomeTestTags.GREETING).assertIsDisplayed()
    }
}
