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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.feature.statementdetail.ui.DownloadState
import org.mifosx.openbanking.feature.statementdetail.ui.StatementDetailAction
import org.mifosx.openbanking.feature.statementdetail.ui.StatementDetailState
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

private const val ROBOLECTRIC_SDK = 34

/**
 * Renders [StatementDetailScreenContent] across its states under Robolectric (JVM, no device) and drives
 * it through the shared [StatementDetailTestTags]. A verbatim on-device mirror lives in
 * [StatementDetailScreenInstrumentedTest].
 *
 * The body is a `LazyColumn`, so a node below the fold must be scrolled to via the list before it is
 * composed and assertable.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [ROBOLECTRIC_SDK])
class StatementDetailScreenRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<StatementDetailAction>()
    private var navigated: Pair<String, String>? = null
    private val firstTxnId = StatementDetailFixtures.FIRST_TRANSACTION_ID

    private fun render(state: StatementDetailState) {
        composeRule.setContent {
            StatementDetailScreenContent(
                state = state,
                onAction = { actions.add(it) },
                onRowClick = { transactionId, accountId -> navigated = transactionId to accountId },
            )
        }
    }

    private fun scrollTo(listTag: String, targetTag: String) {
        composeRule.onNodeWithTag(listTag).performScrollToNode(hasTestTag(targetTag))
    }

    @Test
    fun contentRendersHeaderBalancesFeesInterestAndTransactions() {
        render(StatementDetailFixtures.contentState())

        composeRule.onNodeWithTag(StatementDetailTestTags.CONTENT_LIST).assertExists()
        scrollTo(StatementDetailTestTags.CONTENT_LIST, StatementDetailTestTags.HEADER_CARD)
        composeRule.onNodeWithTag(StatementDetailTestTags.HEADER_CARD, useUnmergedTree = true).assertExists()
        scrollTo(StatementDetailTestTags.CONTENT_LIST, StatementDetailTestTags.FEES_SECTION)
        composeRule.onNodeWithTag(StatementDetailTestTags.FEES_SECTION, useUnmergedTree = true).assertExists()
        scrollTo(StatementDetailTestTags.CONTENT_LIST, StatementDetailTestTags.INTEREST_SECTION)
        composeRule.onNodeWithTag(StatementDetailTestTags.INTEREST_SECTION, useUnmergedTree = true)
            .assertExists()
        scrollTo(StatementDetailTestTags.CONTENT_LIST, StatementDetailTestTags.txnRow(firstTxnId))
        composeRule.onNodeWithTag(StatementDetailTestTags.txnRow(firstTxnId)).assertExists()
    }

    @Test
    fun tappingATransactionRowRoutesBothIds() {
        render(StatementDetailFixtures.contentState())

        scrollTo(StatementDetailTestTags.CONTENT_LIST, StatementDetailTestTags.txnRow(firstTxnId))
        composeRule.onNodeWithTag(StatementDetailTestTags.txnRow(firstTxnId)).performClick()

        assertEquals(firstTxnId to StatementDetailFixtures.ACCOUNT_ID, navigated)
    }

    @Test
    fun tappingDownloadDispatchesDownloadPdf() {
        render(StatementDetailFixtures.contentState())

        scrollTo(StatementDetailTestTags.CONTENT_LIST, StatementDetailTestTags.DOWNLOAD_BUTTON)
        composeRule.onNodeWithTag(StatementDetailTestTags.DOWNLOAD_BUTTON).performClick()

        assertEquals(listOf<StatementDetailAction>(StatementDetailAction.DownloadPdf), actions)
        assertEquals(null, navigated)
    }

    @Test
    fun downloadingShowsTheProgressBar() {
        render(StatementDetailFixtures.contentState(downloadState = DownloadState.Downloading))

        scrollTo(StatementDetailTestTags.CONTENT_LIST, StatementDetailTestTags.DOWNLOAD_PROGRESS)
        composeRule.onNodeWithTag(StatementDetailTestTags.DOWNLOAD_PROGRESS, useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun emptyRendersBalancesAndTheEmptyBlockButHidesFeesAndTransactions() {
        render(StatementDetailFixtures.emptyState())

        composeRule.onNodeWithTag(StatementDetailTestTags.EMPTY_LIST).assertExists()
        scrollTo(StatementDetailTestTags.EMPTY_LIST, StatementDetailTestTags.BALANCES_SECTION)
        composeRule.onNodeWithTag(StatementDetailTestTags.BALANCES_SECTION, useUnmergedTree = true)
            .assertExists()
        scrollTo(StatementDetailTestTags.EMPTY_LIST, StatementDetailTestTags.EMPTY_TRANSACTIONS)
        composeRule.onNodeWithTag(StatementDetailTestTags.EMPTY_TRANSACTIONS, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(StatementDetailTestTags.FEES_SECTION, useUnmergedTree = true)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(StatementDetailTestTags.TRANSACTIONS_SECTION, useUnmergedTree = true)
            .assertDoesNotExist()
    }
}
