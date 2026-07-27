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
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.feature.statementdetail.ui.StatementDetailAction
import org.mifosx.openbanking.feature.statementdetail.ui.StatementDetailViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * One case per `ui.yaml` action_contract, proving each interactive surface routes the action it
 * declares (`navigate_transaction_detail`, `download_statement_pdf`, `retry_load`, `back`) — the 100%
 * on_click coverage the behaviour gate requires.
 *
 * The top-bar back is owned by the container [StatementDetailScreen] (delegated to `onBack`), so that
 * case renders the whole screen over a fake repository; the three in-content actions drive
 * [StatementDetailScreenContent] directly.
 *
 * Test names are camelCase — this source set compiles for Kotlin/Native, whose frontend rejects
 * punctuation inside backticked names.
 */
@OptIn(ExperimentalTestApi::class)
class StatementDetailActionTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** navigate_transaction_detail — a transaction-row tap routes both ids upward. */
    @Test
    fun rowTapInvokesNavigateWithTransactionAndAccountId() = runComposeUiTest {
        var navigated: Pair<String, String>? = null
        setContent {
            StatementDetailScreenContent(
                state = StatementDetailFixtures.contentState(),
                onAction = {},
                onRowClick = { transactionId, accountId -> navigated = transactionId to accountId },
            )
        }

        onNodeWithTag(StatementDetailTestTags.CONTENT_LIST).performScrollToNode(
            hasTestTag(StatementDetailTestTags.txnRow(StatementDetailFixtures.FIRST_TRANSACTION_ID)),
        )
        onNodeWithTag(StatementDetailTestTags.txnRow(StatementDetailFixtures.FIRST_TRANSACTION_ID))
            .performClick()

        assertEquals(
            StatementDetailFixtures.FIRST_TRANSACTION_ID to StatementDetailFixtures.ACCOUNT_ID,
            navigated,
        )
    }

    /** download_statement_pdf — the Download PDF button dispatches DownloadPdf and does not navigate. */
    @Test
    fun downloadButtonDispatchesDownloadPdf() = runComposeUiTest {
        val actions = mutableListOf<StatementDetailAction>()
        var navigated: Pair<String, String>? = null
        setContent {
            StatementDetailScreenContent(
                state = StatementDetailFixtures.contentState(),
                onAction = { actions.add(it) },
                onRowClick = { transactionId, accountId -> navigated = transactionId to accountId },
            )
        }

        onNodeWithTag(StatementDetailTestTags.CONTENT_LIST)
            .performScrollToNode(hasTestTag(StatementDetailTestTags.DOWNLOAD_BUTTON))
        onNodeWithTag(StatementDetailTestTags.DOWNLOAD_BUTTON).performClick()

        assertEquals(listOf<StatementDetailAction>(StatementDetailAction.DownloadPdf), actions)
        assertNull(navigated)
    }

    /** retry_load — the error-state Retry button dispatches RetryLoad. */
    @Test
    fun retryButtonDispatchesRetryLoad() = runComposeUiTest {
        val actions = mutableListOf<StatementDetailAction>()
        setContent {
            StatementDetailScreenContent(
                state = StatementDetailFixtures.errorState(),
                onAction = { actions.add(it) },
                onRowClick = { _, _ -> },
            )
        }

        onNodeWithTag(StatementDetailTestTags.RETRY_BUTTON).performClick()

        assertEquals(listOf<StatementDetailAction>(StatementDetailAction.RetryLoad), actions)
        assertTrue(actions.isNotEmpty())
    }

    /** back — the top-app-bar navigation icon delegates upward through the screen's onBack. */
    @Test
    fun topBarBackInvokesOnBack() = runComposeUiTest {
        var backCount = 0
        val viewModel = StatementDetailViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    StatementDetailViewModel.ACCOUNT_ID_ARG to StatementDetailFixtures.ACCOUNT_ID,
                    StatementDetailViewModel.STATEMENT_ID_ARG to StatementDetailFixtures.STATEMENT_ID,
                ),
            ),
            repository = FakeStatementDetailRepository(
                initialStatement = StatementDetailFixtures.statementContentStream(),
                initialTransactions = StatementDetailFixtures.transactionsContentStream(),
            ),
            fileRepository = FakeStatementFileRepository(),
            fileHandler = FakeStatementFileHandler(),
        )
        setContent {
            StatementDetailScreen(
                onBack = { backCount++ },
                onNavigateToTransactionDetail = { _, _ -> },
                viewModel = viewModel,
            )
        }

        onNodeWithContentDescription("Navigation").performClick()

        assertEquals(1, backCount)
    }
}
