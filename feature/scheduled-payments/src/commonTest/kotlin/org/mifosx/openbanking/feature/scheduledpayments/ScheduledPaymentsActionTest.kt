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

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.feature.scheduledpayments.ui.ScheduledPaymentsAction
import org.mifosx.openbanking.feature.scheduledpayments.ui.ScheduledPaymentsViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * One case per `ui.yaml` action_contract, proving each interactive surface routes the action it
 * declares (`back_button`, `retry_button`) — the 100% on_click coverage the behaviour gate requires.
 *
 * The top-bar back is owned by the container [ScheduledPaymentsScreen] (delegated to `onBack`), so
 * that case renders the whole screen over a fake repository; the retry case drives
 * [ScheduledPaymentsScreenContent] directly.
 *
 * Test names are camelCase — this source set compiles for Kotlin/Native, whose frontend rejects
 * punctuation inside backticked names.
 */
@OptIn(ExperimentalTestApi::class)
class ScheduledPaymentsActionTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** back_button — the top-app-bar navigation icon delegates upward through the screen's onBack. */
    @Test
    fun topBarBackInvokesOnBack() = runComposeUiTest {
        var backCount = 0
        val viewModel = ScheduledPaymentsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(ScheduledPaymentsViewModel.ACCOUNT_ID_ARG to ScheduledPaymentsFixtures.ACCOUNT_ID),
            ),
            repository = FakeScheduledPaymentsRepository(ScheduledPaymentsFixtures.contentStreamState()),
        )
        setContent {
            ScheduledPaymentsScreen(onBack = { backCount++ }, viewModel = viewModel)
        }

        onNodeWithContentDescription("Navigation").performClick()

        assertEquals(1, backCount)
    }

    /** retry_button — the error-state Retry button dispatches RetryLoad. */
    @Test
    fun retryButtonDispatchesRetryLoad() = runComposeUiTest {
        val actions = mutableListOf<ScheduledPaymentsAction>()
        setContent {
            ScheduledPaymentsScreenContent(
                state = ScheduledPaymentsFixtures.errorState(),
                onAction = { actions.add(it) },
            )
        }

        onNodeWithTag(ScheduledPaymentsTestTags.RETRY_BUTTON).performClick()

        assertEquals(listOf<ScheduledPaymentsAction>(ScheduledPaymentsAction.RetryLoad), actions)
        assertTrue(actions.isNotEmpty())
    }
}
