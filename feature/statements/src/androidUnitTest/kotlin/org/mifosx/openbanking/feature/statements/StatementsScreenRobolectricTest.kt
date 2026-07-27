/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.statements

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.feature.statements.ui.DownloadState
import org.mifosx.openbanking.feature.statements.ui.StatementsAction
import org.mifosx.openbanking.feature.statements.ui.StatementsState
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

private const val ROBOLECTRIC_SDK = 34

/**
 * Renders [StatementsScreenContent] across its states under Robolectric (JVM, no device) and drives
 * it through the shared [StatementsTestTags]. A verbatim on-device mirror lives in
 * [StatementsScreenInstrumentedTest].
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [ROBOLECTRIC_SDK])
class StatementsScreenRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<StatementsAction>()
    private var navigatedId: String? = null
    private val mayId = StatementsFixtures.MAY_STATEMENT_ID

    private fun render(state: StatementsState) {
        composeRule.setContent {
            StatementsScreenContent(
                state = state,
                onAction = { actions.add(it) },
                onRowClick = { navigatedId = it },
            )
        }
    }

    @Test
    fun contentRendersTheListWithTheMayRowAndItsDownloadButton() {
        render(StatementsFixtures.contentState())

        composeRule.onNodeWithTag(StatementsTestTags.CONTENT_LIST).assertExists()
        composeRule.onNodeWithTag(StatementsTestTags.row(mayId)).assertExists()
        composeRule.onNodeWithTag(StatementsTestTags.downloadButton(mayId)).assertExists()
    }

    @Test
    fun tappingTheMayRowRoutesItsStatementId() {
        render(StatementsFixtures.contentState())

        composeRule.onNodeWithTag(StatementsTestTags.row(mayId)).performClick()

        assertEquals(mayId, navigatedId)
    }

    @Test
    fun tappingTheDownloadButtonDispatchesDownloadStatement() {
        render(StatementsFixtures.contentState())

        composeRule.onNodeWithTag(StatementsTestTags.downloadButton(mayId)).performClick()

        assertEquals(listOf<StatementsAction>(StatementsAction.DownloadStatement(mayId)), actions)
        assertEquals(null, navigatedId)
    }

    @Test
    fun anInFlightDownloadShowsTheSpinnerInPlaceOfTheButton() {
        render(StatementsFixtures.contentState(downloadState = mapOf(mayId to DownloadState.InProgress)))

        composeRule.onNodeWithTag(StatementsTestTags.downloadSpinner(mayId), useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(StatementsTestTags.downloadButton(mayId)).assertDoesNotExist()
    }

    @Test
    fun loadingRendersTheSkeleton() {
        render(StatementsFixtures.loadingState())

        composeRule.onNodeWithTag(StatementsTestTags.LOADING_SKELETON).assertExists()
        composeRule.onNodeWithTag(StatementsTestTags.CONTENT_LIST).assertDoesNotExist()
    }

    @Test
    fun emptyRendersItsTitleAndBody() {
        render(StatementsFixtures.emptyState())

        composeRule.onNodeWithTag(StatementsTestTags.EMPTY_STATE).assertExists()
        composeRule.onNodeWithTag(StatementsTestTags.EMPTY_TITLE, useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag(StatementsTestTags.EMPTY_BODY, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun errorRendersRetryAndDispatchesRetryLoad() {
        render(StatementsFixtures.errorState())

        composeRule.onNodeWithTag(StatementsTestTags.ERROR_STATE).assertExists()
        composeRule.onNodeWithTag(StatementsTestTags.ERROR_TITLE, useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag(StatementsTestTags.RETRY_BUTTON).performClick()

        assertEquals(listOf<StatementsAction>(StatementsAction.RetryLoad), actions)
    }
}
