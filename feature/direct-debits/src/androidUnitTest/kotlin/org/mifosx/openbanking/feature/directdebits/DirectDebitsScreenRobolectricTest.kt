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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.feature.directdebits.ui.DirectDebitRowUi
import org.mifosx.openbanking.feature.directdebits.ui.DirectDebitsAction
import org.mifosx.openbanking.feature.directdebits.ui.DirectDebitsState
import org.mifosx.openbanking.feature.directdebits.ui.DirectDebitsUiState
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

private const val ROBOLECTRIC_SDK = 34

/**
 * Renders [DirectDebitsScreenContent] under Robolectric (JVM, no device) and drives it through the
 * shared [DirectDebitsTestTags]. A verbatim on-device mirror lives in
 * [DirectDebitsScreenInstrumentedTest].
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [ROBOLECTRIC_SDK])
class DirectDebitsScreenRobolectricTest {

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
        render(DirectDebitsFixtures.contentState())

        composeRule.onNodeWithTag(DirectDebitsTestTags.CONTENT).assertExists()
        composeRule.onNodeWithTag(DirectDebitsTestTags.SUMMARY_CHIPS, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(DirectDebitsTestTags.ACTIVE_CHIP, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(DirectDebitsTestTags.INACTIVE_CHIP, useUnmergedTree = true)
            .assertExists()
        DirectDebitsFixtures.rows().forEach { row ->
            composeRule.onNodeWithTag(DirectDebitsTestTags.CONTENT)
                .performScrollToNode(hasTestTag(DirectDebitsTestTags.card(row.mandateId)))
            composeRule.onNodeWithTag(DirectDebitsTestTags.card(row.mandateId)).assertExists()
        }
    }

    @Test
    fun eachCardRendersItsBadgeAmountDateAndMandateReference() {
        render(DirectDebitsFixtures.contentState())

        val first = DirectDebitsFixtures.rows().first()
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
        render(DirectDebitsFixtures.contentState())

        val inactive = DirectDebitsFixtures.rows().last()
        assertTrue(!inactive.isActive)
        composeRule.onNodeWithTag(DirectDebitsTestTags.CONTENT)
            .performScrollToNode(hasTestTag(DirectDebitsTestTags.card(inactive.mandateId)))
        composeRule.onNodeWithTag(DirectDebitsTestTags.card(inactive.mandateId)).assertExists()
        composeRule.onNodeWithTag(DirectDebitsTestTags.statusBadge(inactive.mandateId), useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(DirectDebitsTestTags.amount(inactive.mandateId), useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun summaryChipsAreDisplayOnlyAndDispatchNothingWhenTapped() {
        render(DirectDebitsFixtures.contentState())

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
                accountId = DirectDebitsFixtures.ACCOUNT_ID,
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
