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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.mifosx.openbanking.feature.statements.ui.StatementsAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * One case per `ui.yaml` action_contract, proving each interactive surface dispatches the action it
 * declares (`navigate_statement_detail`, `download_statement`, `retry_load`) — the 100% on_click
 * coverage the behaviour gate requires.
 *
 * Test names are camelCase — this source set compiles for Kotlin/Native, whose frontend rejects
 * punctuation inside backticked names.
 */
@OptIn(ExperimentalTestApi::class)
class StatementsActionTest {

    private val mayId = StatementsFixtures.MAY_STATEMENT_ID

    /** navigate_statement_detail — a row-body tap routes the statement id upward. */
    @Test
    fun rowTapInvokesNavigateWithStatementId() = runComposeUiTest {
        var navigatedId: String? = null
        setContent {
            StatementsScreenContent(
                state = StatementsFixtures.contentState(),
                onAction = {},
                onRowClick = { navigatedId = it },
            )
        }

        onNodeWithTag(StatementsTestTags.row(mayId)).performClick()

        assertEquals(mayId, navigatedId)
    }

    /** download_statement — the trailing button dispatches Download and does NOT navigate (TC-007). */
    @Test
    fun downloadDispatchesActionAndDoesNotNavigate() = runComposeUiTest {
        val actions = mutableListOf<StatementsAction>()
        var navigatedId: String? = null
        setContent {
            StatementsScreenContent(
                state = StatementsFixtures.contentState(),
                onAction = { actions.add(it) },
                onRowClick = { navigatedId = it },
            )
        }

        onNodeWithTag(StatementsTestTags.downloadButton(mayId)).performClick()

        assertEquals(listOf<StatementsAction>(StatementsAction.DownloadStatement(mayId)), actions)
        assertNull(navigatedId)
    }

    /** retry_load — the error-state Retry button dispatches RetryLoad. */
    @Test
    fun retryButtonDispatchesRetryLoad() = runComposeUiTest {
        val actions = mutableListOf<StatementsAction>()
        setContent {
            StatementsScreenContent(
                state = StatementsFixtures.errorState(),
                onAction = { actions.add(it) },
                onRowClick = {},
            )
        }

        onNodeWithText("Retry").performClick()

        assertEquals(listOf<StatementsAction>(StatementsAction.RetryLoad), actions)
        assertTrue(actions.isNotEmpty())
    }
}
