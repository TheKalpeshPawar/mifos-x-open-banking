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

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.feature.statements.ui.DownloadState
import org.mifosx.openbanking.feature.statements.ui.StatementRowUiModel
import org.mifosx.openbanking.feature.statements.ui.StatementsAction
import org.mifosx.openbanking.feature.statements.ui.StatementsErrorKind
import org.mifosx.openbanking.feature.statements.ui.StatementsState
import org.mifosx.openbanking.feature.statements.ui.StatementsUiState
import kotlin.test.assertEquals

private const val ACCOUNT_ID = "40051512345678"
private const val MAY_ID = "STMT-2026-05-40051512345678"

/**
 * androidInstrumentedTest does not see commonTest, so the state fixtures are inlined here.
 */
private fun rows(): List<StatementRowUiModel> = listOf(
    StatementRowUiModel(MAY_ID, "MAY-2026-STMT", "May 2026", "£2,847.63", "1 May 2026", "31 May 2026"),
    StatementRowUiModel(
        "STMT-2026-04-$ACCOUNT_ID",
        "APR-2026-STMT",
        "April 2026",
        "£2,610.40",
        "1 Apr 2026",
        "30 Apr 2026",
    ),
)

private fun contentState(
    downloadState: Map<String, DownloadState> = emptyMap(),
): StatementsState = StatementsState(
    accountId = ACCOUNT_ID,
    uiState = StatementsUiState.Content(rows()),
    downloadState = downloadState,
)

private fun emptyState(): StatementsState =
    StatementsState(accountId = ACCOUNT_ID, uiState = StatementsUiState.Empty)

private fun errorState(): StatementsState =
    StatementsState(accountId = ACCOUNT_ID, uiState = StatementsUiState.Error(StatementsErrorKind.ServerError))

private fun loadingState(): StatementsState =
    StatementsState(accountId = ACCOUNT_ID, uiState = StatementsUiState.Loading)

/**
 * On-device mirror of [StatementsScreenRobolectricTest], driving the same [StatementsTestTags] so a
 * divergence between the JVM and device renderers is visible.
 */
@RunWith(AndroidJUnit4::class)
class StatementsScreenInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<StatementsAction>()
    private var navigatedId: String? = null

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
        render(contentState())

        composeRule.onNodeWithTag(StatementsTestTags.CONTENT_LIST).assertExists()
        composeRule.onNodeWithTag(StatementsTestTags.row(MAY_ID)).assertExists()
        composeRule.onNodeWithTag(StatementsTestTags.downloadButton(MAY_ID)).assertExists()
    }

    @Test
    fun tappingTheMayRowRoutesItsStatementId() {
        render(contentState())

        composeRule.onNodeWithTag(StatementsTestTags.row(MAY_ID)).performClick()

        assertEquals(MAY_ID, navigatedId)
    }

    @Test
    fun tappingTheDownloadButtonDispatchesDownloadStatement() {
        render(contentState())

        composeRule.onNodeWithTag(StatementsTestTags.downloadButton(MAY_ID)).performClick()

        assertEquals(listOf<StatementsAction>(StatementsAction.DownloadStatement(MAY_ID)), actions)
        assertEquals(null, navigatedId)
    }

    @Test
    fun anInFlightDownloadShowsTheSpinnerInPlaceOfTheButton() {
        render(contentState(downloadState = mapOf(MAY_ID to DownloadState.InProgress)))

        composeRule.onNodeWithTag(StatementsTestTags.downloadSpinner(MAY_ID), useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(StatementsTestTags.downloadButton(MAY_ID)).assertDoesNotExist()
    }

    @Test
    fun loadingRendersTheSkeleton() {
        render(loadingState())

        composeRule.onNodeWithTag(StatementsTestTags.LOADING_SKELETON).assertExists()
    }

    @Test
    fun emptyRendersItsTitleAndBody() {
        render(emptyState())

        composeRule.onNodeWithTag(StatementsTestTags.EMPTY_STATE).assertExists()
        composeRule.onNodeWithTag(StatementsTestTags.EMPTY_TITLE, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(StatementsTestTags.EMPTY_BODY, useUnmergedTree = true).assertExists()
    }

    @Test
    fun errorRendersRetryAndDispatchesRetryLoad() {
        render(errorState())

        composeRule.onNodeWithTag(StatementsTestTags.ERROR_STATE).assertExists()
        composeRule.onNodeWithTag(StatementsTestTags.RETRY_BUTTON).performClick()

        assertEquals(listOf<StatementsAction>(StatementsAction.RetryLoad), actions)
    }
}
