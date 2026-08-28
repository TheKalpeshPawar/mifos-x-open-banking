/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.transactiondetail

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.feature.transactiondetail.ui.TransactionDetailAction
import org.mifosx.openbanking.feature.transactiondetail.ui.TransactionDetailViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * One case per `ui.yaml` action_contract, proving each interactive surface routes the action it
 * declares (`back`, `copy_reference`, `retry_load`, `go_back`, `empty_go_back`) — the 100% on_click
 * coverage the behaviour gate requires.
 *
 * The top-bar back is owned by the container [TransactionDetailScreen] (delegated to `onBack`), so that
 * one case renders the whole screen over a fake repository; the four in-content actions drive
 * [TransactionDetailScreenContent] directly.
 *
 * Test names are camelCase — this source set compiles for Kotlin/Native, whose frontend rejects
 * punctuation inside backticked names.
 */
@OptIn(ExperimentalTestApi::class)
class TransactionDetailActionTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** back — the top-app-bar navigation icon delegates upward through the screen's onBack. */
    @Test
    fun topBarBackInvokesOnBack() = runComposeUiTest {
        var backCount = 0
        val viewModel = TransactionDetailViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    TransactionDetailViewModel.TRANSACTION_ID_ARG to TransactionDetailFixtures.DEBIT_ID,
                    TransactionDetailViewModel.ACCOUNT_ID_ARG to TransactionDetailFixtures.ACCOUNT_ID,
                ),
            ),
            repository = FakeTransactionDetailRepository(
                TransactionDetailFixtures.contentStreamState(),
            ),
        )
        setContent {
            TransactionDetailScreen(onBack = { backCount++ }, viewModel = viewModel)
        }

        onNodeWithContentDescription("Navigation").performClick()

        assertEquals(1, backCount)
    }

    /** copy_reference — the reference row dispatches CopyReference with the reference string. */
    @Test
    fun referenceRowDispatchesCopyReference() = runComposeUiTest {
        val actions = mutableListOf<TransactionDetailAction>()
        setContent {
            TransactionDetailScreenContent(
                state = TransactionDetailFixtures.contentState(),
                onAction = { actions.add(it) },
                onBack = {},
            )
        }

        onNodeWithTag(TransactionDetailTestTags.REFERENCE_ROW).performScrollTo().performClick()

        assertEquals(
            listOf<TransactionDetailAction>(
                TransactionDetailAction.CopyReference("TESCO STORES 3476 LONDON"),
            ),
            actions,
        )
    }

    /** retry_load — the recoverable-error Retry button dispatches RetryLoad. */
    @Test
    fun retryButtonDispatchesRetryLoad() = runComposeUiTest {
        val actions = mutableListOf<TransactionDetailAction>()
        setContent {
            TransactionDetailScreenContent(
                state = TransactionDetailFixtures.recoverableErrorState(),
                onAction = { actions.add(it) },
                onBack = {},
            )
        }

        onNodeWithText("Retry").performClick()

        assertEquals(listOf<TransactionDetailAction>(TransactionDetailAction.RetryLoad), actions)
        assertTrue(actions.isNotEmpty())
    }

    /** empty_go_back — the empty-state Go Back button routes through onBack, not an action. */
    @Test
    fun emptyBackButtonInvokesOnBack() = runComposeUiTest {
        val actions = mutableListOf<TransactionDetailAction>()
        var backCount = 0
        setContent {
            TransactionDetailScreenContent(
                state = TransactionDetailFixtures.emptyState(),
                onAction = { actions.add(it) },
                onBack = { backCount++ },
            )
        }

        onNodeWithTag(TransactionDetailTestTags.EMPTY_BACK_BUTTON).performClick()

        assertEquals(1, backCount)
        assertTrue(actions.isEmpty())
    }
}
