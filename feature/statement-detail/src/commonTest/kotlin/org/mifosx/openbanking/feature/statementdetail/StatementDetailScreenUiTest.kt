/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.statementdetail

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.runComposeUiTest
import org.mifosx.openbanking.feature.statementdetail.ui.DownloadState
import kotlin.test.Test

/**
 * Headless smoke coverage for [StatementDetailScreenContent] on the desktop renderer, over the same
 * [StatementDetailTestTags] the Robolectric and instrumented suites drive.
 *
 * This class lives in `commonTest`, so it also compiles into `androidUnitTest`, where there is no
 * Robolectric runner; the module's build script filters it out of the JVM unit-test tasks.
 *
 * Test names are camelCase — this source set compiles for Kotlin/Native.
 */
@OptIn(ExperimentalTestApi::class)
class StatementDetailScreenUiTest {

    @Test
    fun contentRendersHeaderSectionsTransactionsAndDownload() = runComposeUiTest {
        setContent {
            StatementDetailScreenContent(
                state = StatementDetailFixtures.contentState(),
                onAction = {},
                onRowClick = { _, _ -> },
            )
        }

        onNodeWithTag(StatementDetailTestTags.CONTENT_LIST).assertExists()
        scrollTo(StatementDetailTestTags.CONTENT_LIST, StatementDetailTestTags.HEADER_CARD)
        onNodeWithTag(StatementDetailTestTags.HEADER_CARD, useUnmergedTree = true).assertExists()
        scrollTo(StatementDetailTestTags.CONTENT_LIST, StatementDetailTestTags.FEES_SECTION)
        onNodeWithTag(StatementDetailTestTags.FEES_SECTION, useUnmergedTree = true).assertExists()
        scrollTo(StatementDetailTestTags.CONTENT_LIST, StatementDetailTestTags.INTEREST_SECTION)
        onNodeWithTag(StatementDetailTestTags.INTEREST_SECTION, useUnmergedTree = true).assertExists()
        scrollTo(StatementDetailTestTags.CONTENT_LIST, StatementDetailTestTags.TRANSACTIONS_SECTION)
        onNodeWithTag(StatementDetailTestTags.TRANSACTIONS_SECTION, useUnmergedTree = true).assertExists()
        scrollTo(
            StatementDetailTestTags.CONTENT_LIST,
            StatementDetailTestTags.txnRow(StatementDetailFixtures.FIRST_TRANSACTION_ID),
        )
        onNodeWithTag(StatementDetailTestTags.txnRow(StatementDetailFixtures.FIRST_TRANSACTION_ID)).assertExists()
        scrollTo(StatementDetailTestTags.CONTENT_LIST, StatementDetailTestTags.DOWNLOAD_BUTTON)
        onNodeWithTag(StatementDetailTestTags.DOWNLOAD_BUTTON).assertExists()
    }

    @Test
    fun emptyRendersBalancesTheEmptyBlockAndDownloadButNoTransactionSections() = runComposeUiTest {
        setContent {
            StatementDetailScreenContent(
                state = StatementDetailFixtures.emptyState(),
                onAction = {},
                onRowClick = { _, _ -> },
            )
        }

        onNodeWithTag(StatementDetailTestTags.EMPTY_LIST).assertExists()
        scrollTo(StatementDetailTestTags.EMPTY_LIST, StatementDetailTestTags.BALANCES_SECTION)
        onNodeWithTag(StatementDetailTestTags.BALANCES_SECTION, useUnmergedTree = true).assertExists()
        scrollTo(StatementDetailTestTags.EMPTY_LIST, StatementDetailTestTags.EMPTY_TRANSACTIONS)
        onNodeWithTag(StatementDetailTestTags.EMPTY_TRANSACTIONS, useUnmergedTree = true).assertExists()
        scrollTo(StatementDetailTestTags.EMPTY_LIST, StatementDetailTestTags.DOWNLOAD_BUTTON)
        onNodeWithTag(StatementDetailTestTags.DOWNLOAD_BUTTON).assertExists()
        onNodeWithTag(StatementDetailTestTags.FEES_SECTION, useUnmergedTree = true).assertDoesNotExist()
        onNodeWithTag(StatementDetailTestTags.TRANSACTIONS_SECTION, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun downloadingRendersTheProgressBar() = runComposeUiTest {
        setContent {
            StatementDetailScreenContent(
                state = StatementDetailFixtures.contentState(downloadState = DownloadState.Downloading),
                onAction = {},
                onRowClick = { _, _ -> },
            )
        }

        scrollTo(StatementDetailTestTags.CONTENT_LIST, StatementDetailTestTags.DOWNLOAD_PROGRESS)
        onNodeWithTag(StatementDetailTestTags.DOWNLOAD_PROGRESS, useUnmergedTree = true).assertExists()
    }

    private fun androidx.compose.ui.test.ComposeUiTest.scrollTo(listTag: String, targetTag: String) {
        onNodeWithTag(listTag).performScrollToNode(hasTestTag(targetTag))
    }
}
