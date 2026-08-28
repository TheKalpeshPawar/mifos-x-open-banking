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
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.feature.standingorders.ui.StandingOrderRowUi
import org.mifosx.openbanking.feature.standingorders.ui.StandingOrdersState
import org.mifosx.openbanking.feature.standingorders.ui.StandingOrdersUiState

private const val ACCOUNT_ID = "40051512345678"

/**
 * androidInstrumentedTest does not see commonTest, so the display fixture is inlined here.
 */
private fun rows(): List<StandingOrderRowUi> = listOf(
    StandingOrderRowUi(
        standingOrderId = "SO-001",
        payeeName = "Jameson Lettings",
        statusLabel = "Active",
        isActive = true,
        amountLabel = "£1,200.00",
        currencyLabel = "GBP",
        frequencyLabel = "Monthly on the 1st",
        nextDateLabel = "1 Jul 2026",
        finalDateLabel = "",
        hasFinalPayment = false,
        sortCodeLabel = "40-12-09 65872310",
        referenceLabel = "RENT-FLAT12",
    ),
    StandingOrderRowUi(
        standingOrderId = "SO-002",
        payeeName = "ISA Saver",
        statusLabel = "Active",
        isActive = true,
        amountLabel = "£200.00",
        currencyLabel = "GBP",
        frequencyLabel = "Monthly on the 1st",
        nextDateLabel = "1 Jul 2026",
        finalDateLabel = "",
        hasFinalPayment = false,
        sortCodeLabel = "60-16-13 31926819",
        referenceLabel = "ISA-TOPUP",
    ),
    StandingOrderRowUi(
        standingOrderId = "SO-003",
        payeeName = "PureGym",
        statusLabel = "Active",
        isActive = true,
        amountLabel = "£24.99",
        currencyLabel = "GBP",
        frequencyLabel = "Monthly on the 15th",
        nextDateLabel = "15 Jul 2026",
        finalDateLabel = "",
        hasFinalPayment = false,
        sortCodeLabel = "20-00-00 55512345",
        referenceLabel = "GYM-MBR",
    ),
    StandingOrderRowUi(
        standingOrderId = "SO-005",
        payeeName = "Marcus Savings",
        statusLabel = "Active",
        isActive = true,
        amountLabel = "£50.00",
        currencyLabel = "GBP",
        frequencyLabel = "Weekly every Friday",
        nextDateLabel = "4 Jul 2026",
        finalDateLabel = "25 Dec 2026",
        hasFinalPayment = true,
        sortCodeLabel = "30-96-22 41227714",
        referenceLabel = "SAVINGS-SWEEP",
    ),
    StandingOrderRowUi(
        standingOrderId = "SO-004",
        payeeName = "Oxfam GB",
        statusLabel = "Inactive",
        isActive = false,
        amountLabel = "£10.00",
        currencyLabel = "GBP",
        frequencyLabel = "Monthly on the 28th",
        nextDateLabel = "",
        finalDateLabel = "28 Dec 2025",
        hasFinalPayment = true,
        sortCodeLabel = "08-60-01 20321982",
        referenceLabel = "CHARITY-DON",
    ),
)

private fun contentState(): StandingOrdersState = StandingOrdersState(
    accountId = ACCOUNT_ID,
    uiState = StandingOrdersUiState.Content(orders = rows()),
)

/**
 * On-device mirror of [StandingOrdersScreenRobolectricTest], driving the same
 * [StandingOrdersTestTags] so a divergence between the JVM and device renderers is visible.
 */
@RunWith(AndroidJUnit4::class)
class StandingOrdersScreenInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun render(state: StandingOrdersState) {
        composeRule.setContent {
            StandingOrdersScreenContent(state = state, onAction = {})
        }
    }

    @Test
    fun contentStateRendersEveryOrderCard() {
        render(contentState())

        composeRule.onNodeWithTag(StandingOrdersTestTags.CONTENT).assertExists()
        rows().forEach { row ->
            composeRule.onNodeWithTag(StandingOrdersTestTags.CONTENT)
                .performScrollToNode(hasTestTag(StandingOrdersTestTags.card(row.standingOrderId)))
            composeRule.onNodeWithTag(StandingOrdersTestTags.card(row.standingOrderId)).assertExists()
        }
    }

    @Test
    fun eachCardRendersItsBadgeAmountFrequencyDateSortCodeAndReference() {
        render(contentState())

        val first = rows().first()
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
        render(contentState())

        val first = rows().first()
        composeRule.onNodeWithTag(
            StandingOrdersTestTags.finalDate(first.standingOrderId),
            useUnmergedTree = true,
        ).assertDoesNotExist()
    }

    @Test
    fun inactiveOrderIsRenderedRatherThanFilteredOut() {
        render(contentState())

        val inactive = rows().last()
        composeRule.onNodeWithTag(StandingOrdersTestTags.CONTENT)
            .performScrollToNode(hasTestTag(StandingOrdersTestTags.card(inactive.standingOrderId)))
        composeRule.onNodeWithTag(StandingOrdersTestTags.card(inactive.standingOrderId)).assertExists()
        composeRule.onNodeWithTag(
            StandingOrdersTestTags.statusBadge(inactive.standingOrderId),
            useUnmergedTree = true,
        ).assertExists()
    }
}
