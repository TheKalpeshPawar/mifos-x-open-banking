/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.scheduledpayments

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.feature.scheduledpayments.ui.ScheduledPaymentsAction
import org.mifosx.openbanking.feature.scheduledpayments.ui.ScheduledPaymentsState
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

private const val ROBOLECTRIC_SDK = 34

/**
 * Renders [ScheduledPaymentsScreenContent] across its states under Robolectric (JVM, no device) and
 * drives it through the shared [ScheduledPaymentsTestTags]. A verbatim on-device mirror lives in
 * [ScheduledPaymentsScreenInstrumentedTest].
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [ROBOLECTRIC_SDK])
class ScheduledPaymentsScreenRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<ScheduledPaymentsAction>()

    private fun render(state: ScheduledPaymentsState) {
        composeRule.setContent {
            ScheduledPaymentsScreenContent(
                state = state,
                onAction = { actions.add(it) },
            )
        }
    }

    @Test
    fun contentRendersTheCardsAndBothTypeChips() {
        render(ScheduledPaymentsFixtures.contentState())

        composeRule.onNodeWithTag(ScheduledPaymentsTestTags.CONTENT_LIST).assertExists()
        composeRule.onNodeWithTag(ScheduledPaymentsTestTags.card(ScheduledPaymentsFixtures.EXECUTION_ID))
            .assertExists()
        composeRule.onNodeWithTag(ScheduledPaymentsTestTags.typeChip(ScheduledPaymentsFixtures.EXECUTION_ID))
            .assertExists()
        composeRule.onNodeWithTag(ScheduledPaymentsTestTags.card(ScheduledPaymentsFixtures.ARRIVAL_ID))
            .assertExists()
        composeRule.onNodeWithTag(ScheduledPaymentsTestTags.typeChip(ScheduledPaymentsFixtures.ARRIVAL_ID))
            .assertExists()
    }

    @Test
    fun loadingRendersTheSpinner() {
        render(ScheduledPaymentsFixtures.loadingState())

        composeRule.onNodeWithTag(ScheduledPaymentsTestTags.LOADING).assertExists()
        composeRule.onNodeWithTag(ScheduledPaymentsTestTags.CONTENT_LIST).assertDoesNotExist()
    }

    @Test
    fun emptyRendersItsTitleAndBody() {
        render(ScheduledPaymentsFixtures.emptyState())

        composeRule.onNodeWithTag(ScheduledPaymentsTestTags.EMPTY_STATE).assertExists()
        composeRule.onNodeWithTag(ScheduledPaymentsTestTags.EMPTY_TITLE, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(ScheduledPaymentsTestTags.EMPTY_BODY, useUnmergedTree = true).assertExists()
    }

    @Test
    fun errorShowsRetryAndDispatchesRetryLoad() {
        render(ScheduledPaymentsFixtures.errorState())

        composeRule.onNodeWithTag(ScheduledPaymentsTestTags.ERROR_STATE).assertExists()
        composeRule.onNodeWithTag(ScheduledPaymentsTestTags.ERROR_TITLE, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(ScheduledPaymentsTestTags.RETRY_BUTTON).performClick()

        assertEquals(listOf<ScheduledPaymentsAction>(ScheduledPaymentsAction.RetryLoad), actions)
    }
}
