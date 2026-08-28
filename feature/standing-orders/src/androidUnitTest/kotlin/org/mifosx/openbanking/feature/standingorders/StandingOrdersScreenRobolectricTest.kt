/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.standingorders

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.feature.standingorders.ui.StandingOrderRowUi
import org.mifosx.openbanking.feature.standingorders.ui.StandingOrdersState
import org.mifosx.openbanking.feature.standingorders.ui.StandingOrdersUiState
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

private const val ROBOLECTRIC_SDK = 34

/**
 * Renders [StandingOrdersScreenContent] across its states under Robolectric (JVM, no device) and
 * drives it through the shared [StandingOrdersTestTags]. A verbatim on-device mirror lives in
 * [StandingOrdersScreenInstrumentedTest].
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [ROBOLECTRIC_SDK])
class StandingOrdersScreenRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun render(state: StandingOrdersState) {
        composeRule.setContent {
            StandingOrdersScreenContent(state = state, onAction = {})
        }
    }

    @Test
    fun contentStateRendersEveryOrderCard() {
        render(StandingOrdersFixtures.contentState())

        composeRule.onNodeWithTag(StandingOrdersTestTags.CONTENT).assertExists()
        StandingOrdersFixtures.rows().forEach { row ->
            composeRule.onNodeWithTag(StandingOrdersTestTags.CONTENT)
                .performScrollToNode(hasTestTag(StandingOrdersTestTags.card(row.standingOrderId)))
            composeRule.onNodeWithTag(StandingOrdersTestTags.card(row.standingOrderId)).assertExists()
        }
    }

    @Test
    fun eachCardRendersItsBadgeAmountFrequencyDateSortCodeAndReference() {
        render(StandingOrdersFixtures.contentState())

        val first = StandingOrdersFixtures.rows().first()
        listOf(
            StandingOrdersTestTags.statusBadge(first.standingOrderId),
            StandingOrdersTestTags.amount(first.standingOrderId),
            StandingOrdersTestTags.frequency(first.standingOrderId),
            StandingOrdersTestTags.nextDate(first.standingOrderId),
            StandingOrdersTestTags.sortCode(first.standingOrderId),
            StandingOrdersTestTags.reference(first.standingOrderId),
        ).forEach { tag ->
            composeRule.onNodeWithTag(tag, useUnmergedTree = true).assertExists()
        }
    }

    @Test
    fun anOrderWithoutAFinalPaymentOmitsThatLine() {
        render(StandingOrdersFixtures.contentState())

        val first = StandingOrdersFixtures.rows().first()
        assertTrue(!first.hasFinalPayment)
        composeRule.onNodeWithTag(
            StandingOrdersTestTags.finalDate(first.standingOrderId),
            useUnmergedTree = true,
        ).assertDoesNotExist()
    }

    @Test
    fun anOrderWithAFinalPaymentRendersThatLine() {
        render(StandingOrdersFixtures.contentState())

        val ending = StandingOrdersFixtures.rows().single { it.standingOrderId == "SO-005" }
        composeRule.onNodeWithTag(StandingOrdersTestTags.CONTENT)
            .performScrollToNode(hasTestTag(StandingOrdersTestTags.card(ending.standingOrderId)))
        composeRule.onNodeWithTag(
            StandingOrdersTestTags.finalDate(ending.standingOrderId),
            useUnmergedTree = true,
        ).assertExists()
    }

    @Test
    fun inactiveOrderIsRenderedRatherThanFilteredOut() {
        render(StandingOrdersFixtures.contentState())

        val inactive = StandingOrdersFixtures.rows().last()
        assertTrue(!inactive.isActive)
        composeRule.onNodeWithTag(StandingOrdersTestTags.CONTENT)
            .performScrollToNode(hasTestTag(StandingOrdersTestTags.card(inactive.standingOrderId)))
        composeRule.onNodeWithTag(StandingOrdersTestTags.card(inactive.standingOrderId)).assertExists()
        composeRule.onNodeWithTag(
            StandingOrdersTestTags.statusBadge(inactive.standingOrderId),
            useUnmergedTree = true,
        ).assertExists()
    }

    @Test
    fun aCancelledOrderWithNoNextPaymentOmitsThatLineButKeepsTheFinalOne() {
        render(StandingOrdersFixtures.contentState())

        val inactive = StandingOrdersFixtures.rows().last()
        composeRule.onNodeWithTag(StandingOrdersTestTags.CONTENT)
            .performScrollToNode(hasTestTag(StandingOrdersTestTags.card(inactive.standingOrderId)))
        composeRule.onNodeWithTag(
            StandingOrdersTestTags.nextDate(inactive.standingOrderId),
            useUnmergedTree = true,
        ).assertDoesNotExist()
        composeRule.onNodeWithTag(
            StandingOrdersTestTags.finalDate(inactive.standingOrderId),
            useUnmergedTree = true,
        ).assertExists()
    }

    @Test
    fun anOrderWithNoAmountSortCodeOrReferenceOmitsThoseLines() {
        val bare = StandingOrderRowUi(
            standingOrderId = "",
            payeeName = "Unknown Payee",
            statusLabel = "",
            isActive = false,
            amountLabel = "",
            currencyLabel = "",
            frequencyLabel = "",
            nextDateLabel = "",
            finalDateLabel = "",
            hasFinalPayment = false,
            sortCodeLabel = "",
            referenceLabel = "",
        )
        render(
            StandingOrdersState(
                accountId = StandingOrdersFixtures.ACCOUNT_ID,
                uiState = StandingOrdersUiState.Content(
                    orders = listOf(bare),
                ),
            ),
        )

        composeRule.onNodeWithTag(StandingOrdersTestTags.card("")).assertExists()
        listOf(
            StandingOrdersTestTags.amount(""),
            StandingOrdersTestTags.frequency(""),
            StandingOrdersTestTags.nextDate(""),
            StandingOrdersTestTags.finalDate(""),
            StandingOrdersTestTags.sortCode(""),
            StandingOrdersTestTags.reference(""),
        ).forEach { tag ->
            composeRule.onNodeWithTag(tag, useUnmergedTree = true).assertDoesNotExist()
        }
    }
}
