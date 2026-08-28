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
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.feature.directdebits.ui.DirectDebitRowUi
import org.mifosx.openbanking.feature.directdebits.ui.DirectDebitsState
import org.mifosx.openbanking.feature.directdebits.ui.DirectDebitsUiState

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
        previousPaymentAmount = "£78.00",
        previousPaymentDateTime = "2026-06-15 00:00:00",
    ),
    DirectDebitRowUi(
        mandateId = "DD-VF-88301",
        name = "Vodafone",
        statusLabel = "Active",
        isActive = true,
        previousPaymentAmount = "£29.00",
        previousPaymentDateTime = "2026-06-20 00:00:00",
    ),
    DirectDebitRowUi(
        mandateId = "DD-AV-10293",
        name = "Aviva Insurance",
        statusLabel = "Active",
        isActive = true,
        previousPaymentAmount = "£41.50",
        previousPaymentDateTime = "2026-06-05 00:00:00",
    ),
    DirectDebitRowUi(
        mandateId = "DD-TVL-55667",
        name = "TV Licensing",
        statusLabel = "Inactive",
        isActive = false,
        previousPaymentAmount = "£13.25",
        previousPaymentDateTime = "2026-03-01 00:00:00",
    ),
)

private fun contentState(): DirectDebitsState = DirectDebitsState(
    accountId = ACCOUNT_ID,
    uiState = DirectDebitsUiState.Content(mandates = rows()),
)

/**
 * On-device mirror of [DirectDebitsScreenRobolectricTest], driving the same
 * [DirectDebitsTestTags] so a divergence between the JVM and device renderers is visible.
 */
@RunWith(AndroidJUnit4::class)
class DirectDebitsScreenInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun render(state: DirectDebitsState) {
        composeRule.setContent {
            DirectDebitsScreenContent(state = state, onAction = {})
        }
    }

    @Test
    fun contentStateRendersEveryMandateCard() {
        render(contentState())

        composeRule.onNodeWithTag(DirectDebitsTestTags.CONTENT).assertExists()
        rows().forEach { row ->
            composeRule.onNodeWithTag(DirectDebitsTestTags.CONTENT)
                .performScrollToNode(hasTestTag(DirectDebitsTestTags.card(row.mandateId)))
            composeRule.onNodeWithTag(DirectDebitsTestTags.card(row.mandateId)).assertExists()
        }
    }

    @Test
    fun eachCardRendersItsBadgeAmountAndCollectionTime() {
        render(contentState())

        val first = rows().first()
        composeRule.onNodeWithTag(DirectDebitsTestTags.statusBadge(first.mandateId), useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(DirectDebitsTestTags.amount(first.mandateId), useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(DirectDebitsTestTags.collectionTime(first.mandateId), useUnmergedTree = true)
            .assertExists()
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
    fun aMandateWithBlankValuesStillRendersItsCard() {
        val bare = DirectDebitRowUi(
            mandateId = "",
            name = "Unknown Originator",
            statusLabel = "",
            isActive = false,
            previousPaymentAmount = "",
            previousPaymentDateTime = "",
        )
        render(
            DirectDebitsState(
                accountId = ACCOUNT_ID,
                uiState = DirectDebitsUiState.Content(mandates = listOf(bare)),
            ),
        )

        composeRule.onNodeWithTag(DirectDebitsTestTags.card("")).assertExists()
        composeRule.onNodeWithTag(DirectDebitsTestTags.amount(""), useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(DirectDebitsTestTags.collectionTime(""), useUnmergedTree = true)
            .assertExists()
    }
}
