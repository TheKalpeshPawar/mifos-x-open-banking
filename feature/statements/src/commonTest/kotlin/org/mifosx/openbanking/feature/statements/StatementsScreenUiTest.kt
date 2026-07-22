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

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import org.mifosx.openbanking.feature.statements.ui.DownloadState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Headless smoke coverage for [StatementsScreenContent] on the desktop renderer, over the same
 * [StatementsTestTags] the Robolectric and instrumented suites drive.
 *
 * This class lives in `commonTest`, so it also compiles into `androidUnitTest`, where there is no
 * Robolectric runner to stand up a composition. The module's build script filters it out of the
 * JVM unit-test tasks; the depth belongs to [StatementsScreenRobolectricTest].
 *
 * Test names are camelCase — this source set compiles for Kotlin/Native, whose frontend rejects
 * punctuation inside backticked names.
 */
@OptIn(ExperimentalTestApi::class)
class StatementsScreenUiTest {

    private val mayId = StatementsFixtures.MAY_STATEMENT_ID

    @Test
    fun contentRendersEverySixRowsEachWithADownloadButton() = runComposeUiTest {
        setContent {
            StatementsScreenContent(
                state = StatementsFixtures.contentState(),
                onAction = {},
                onRowClick = {},
            )
        }

        onNodeWithTag(StatementsTestTags.CONTENT_LIST).assertExists()
        StatementsFixtures.rowUiModels().forEach { row ->
            onNodeWithTag(StatementsTestTags.row(row.statementId)).assertExists()
            onNodeWithTag(StatementsTestTags.downloadButton(row.statementId)).assertExists()
        }
    }

    @Test
    fun loadingRendersTheSkeletonNotTheContent() = runComposeUiTest {
        setContent {
            StatementsScreenContent(
                state = StatementsFixtures.loadingState(),
                onAction = {},
                onRowClick = {},
            )
        }

        onNodeWithTag(StatementsTestTags.LOADING_SKELETON).assertExists()
        onNodeWithTag(StatementsTestTags.CONTENT_LIST).assertDoesNotExist()
    }

    @Test
    fun emptyRendersItsTitleAndBody() = runComposeUiTest {
        setContent {
            StatementsScreenContent(
                state = StatementsFixtures.emptyState(),
                onAction = {},
                onRowClick = {},
            )
        }

        onNodeWithTag(StatementsTestTags.EMPTY_STATE).assertExists()
        onNodeWithTag(StatementsTestTags.EMPTY_TITLE, useUnmergedTree = true).assertExists()
        onNodeWithTag(StatementsTestTags.EMPTY_BODY, useUnmergedTree = true).assertExists()
    }

    @Test
    fun errorRendersTitleAndRetry() = runComposeUiTest {
        setContent {
            StatementsScreenContent(
                state = StatementsFixtures.errorState(),
                onAction = {},
                onRowClick = {},
            )
        }

        onNodeWithTag(StatementsTestTags.ERROR_STATE).assertExists()
        onNodeWithTag(StatementsTestTags.ERROR_TITLE, useUnmergedTree = true).assertExists()
        onNodeWithTag(StatementsTestTags.RETRY_BUTTON).assertExists()
    }

    @Test
    fun aRowWithAnInFlightDownloadShowsItsSpinnerNotTheButton() = runComposeUiTest {
        setContent {
            StatementsScreenContent(
                state = StatementsFixtures.contentState(
                    downloadState = mapOf(mayId to DownloadState.InProgress),
                ),
                onAction = {},
                onRowClick = {},
            )
        }

        onNodeWithTag(StatementsTestTags.downloadSpinner(mayId), useUnmergedTree = true).assertExists()
        onNodeWithTag(StatementsTestTags.downloadButton(mayId)).assertDoesNotExist()
    }

    @Test
    fun contentRendersExactlyOneNodeForTheMayRow() = runComposeUiTest {
        setContent {
            StatementsScreenContent(
                state = StatementsFixtures.contentState(),
                onAction = {},
                onRowClick = {},
            )
        }

        assertEquals(
            1,
            onAllNodesWithTag(StatementsTestTags.row(mayId), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .size,
        )
    }
}
