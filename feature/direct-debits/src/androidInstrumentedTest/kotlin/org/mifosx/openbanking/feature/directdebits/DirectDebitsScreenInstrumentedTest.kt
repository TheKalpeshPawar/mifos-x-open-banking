/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.directdebits

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.feature.directdebits.ui.DirectDebitRowUi
import org.mifosx.openbanking.feature.directdebits.ui.DirectDebitsAction
import org.mifosx.openbanking.feature.directdebits.ui.DirectDebitsState
import org.mifosx.openbanking.feature.directdebits.ui.DirectDebitsUiState
import kotlin.test.assertTrue

private const val ACCOUNT_ID = "40051512345678"

/**
 * androidInstrumentedTest does not see commonTest, so the display fixture is inlined here.
 */
private fun rows(): List<DirectDebitRowUi> = listOf(
    DirectDebitRowUi(
        mandateId = "DD-BG-44120",
        name = "British Gas",
        statusLabel = "Active",
        isActive = true,
        amountLabel = "£78.00",
        lastCollectedLabel = "15 Jun 2026",
    ),
    DirectDebitRowUi(
        mandateId = "DD-VF-88301",
        name = "Vodafone",
        statusLabel = "Active",
        isActive = true,
        amountLabel = "£29.00",
        lastCollectedLabel = "20 Jun 2026",
    ),
    DirectDebitRowUi(
        mandateId = "DD-AV-10293",
        name = "Aviva Insurance",
        statusLabel = "Active",
        isActive = true,
        amountLabel = "£41.50",
        lastCollectedLabel = "5 Jun 2026",
    ),
    DirectDebitRowUi(
        mandateId = "DD-TVL-55667",
        name = "TV Licensing",
        statusLabel = "Inactive",
        isActive = false,
        amountLabel = "£13.25",
        lastCollectedLabel = "1 Mar 2026",
    ),
)

private fun contentState(): DirectDebitsState = DirectDebitsState(
    accountId = ACCOUNT_ID,
    uiState = DirectDebitsUiState.Content(mandates = rows(), activeCount = 3, inactiveCount = 1),
)

/**
 * On-device mirror of [DirectDebitsScreenRobolectricTest], driving the same
 * [DirectDebitsTestTags] so a divergence between the JVM and device renderers is visible.
 */
@RunWith(AndroidJUnit4::class)
class DirectDebitsScreenInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<DirectDebitsAction>()

    private fun render(state: DirectDebitsState) {
        composeRule.setContent {
            DirectDebitsScreenContent(state = state, onAction = { actions.add(it) })
        }
    }

    @Test
    fun contentStateRendersSummaryChipsAndEveryMandateCard() {
        render(contentState())

        composeRule.onNodeWithTag(DirectDebitsTestTags.CONTENT).assertExists()
        composeRule.onNodeWithTag(DirectDebitsTestTags.ACTIVE_CHIP, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(DirectDebitsTestTags.INACTIVE_CHIP, useUnmergedTree = true).assertExists()
        rows().forEach { row ->
            composeRule.onNodeWithTag(DirectDebitsTestTags.CONTENT)
                .performScrollToNode(hasTestTag(DirectDebitsTestTags.card(row.mandateId)))
            composeRule.onNodeWithTag(DirectDebitsTestTags.card(row.mandateId)).assertExists()
        }
    }

    @Test
    fun eachCardRendersItsBadgeAmountDateAndMandateReference() {
        render(contentState())

        val first = rows().first()
        composeRule.onNodeWithTag(DirectDebitsTestTags.statusBadge(first.mandateId), useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(DirectDebitsTestTags.amount(first.mandateId), useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(DirectDebitsTestTags.lastCollected(first.mandateId), useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(
            DirectDebitsTestTags.mandateReference(first.mandateId),
            useUnmergedTree = true,
        ).assertExists()
    }

    @Test
    fun inactiveMandateIsRenderedRatherThanFilteredOut() {
        render(contentState())

        val inactive = rows().last()
        composeRule.onNodeWithTag(DirectDebitsTestTags.CONTENT)
            .performScrollToNode(hasTestTag(DirectDebitsTestTags.card(inactive.mandateId)))
        composeRule.onNodeWithTag(DirectDebitsTestTags.card(inactive.mandateId)).assertExists()
        composeRule.onNodeWithTag(DirectDebitsTestTags.statusBadge(inactive.mandateId), useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun summaryChipsAreDisplayOnlyAndDispatchNothingWhenTapped() {
        render(contentState())

        composeRule.onNodeWithTag(DirectDebitsTestTags.ACTIVE_CHIP, useUnmergedTree = true).performClick()
        composeRule.onNodeWithTag(DirectDebitsTestTags.INACTIVE_CHIP, useUnmergedTree = true).performClick()

        assertTrue(actions.isEmpty())
    }

    @Test
    fun aMandateWithNoAmountDateOrReferenceOmitsThoseLines() {
        val bare = DirectDebitRowUi(
            mandateId = "",
            name = "Unknown Originator",
            statusLabel = "",
            isActive = false,
            amountLabel = "",
            lastCollectedLabel = "",
        )
        render(
            DirectDebitsState(
                accountId = ACCOUNT_ID,
                uiState = DirectDebitsUiState.Content(
                    mandates = listOf(bare),
                    activeCount = 0,
                    inactiveCount = 1,
                ),
            ),
        )

        composeRule.onNodeWithTag(DirectDebitsTestTags.card("")).assertExists()
        composeRule.onNodeWithTag(DirectDebitsTestTags.amount(""), useUnmergedTree = true)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(DirectDebitsTestTags.lastCollected(""), useUnmergedTree = true)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(DirectDebitsTestTags.mandateReference(""), useUnmergedTree = true)
            .assertDoesNotExist()
    }
}
