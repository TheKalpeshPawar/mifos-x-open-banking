/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.consentdetail

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.feature.consentdetail.ui.ConsentDetailAction
import org.mifosx.openbanking.feature.consentdetail.ui.ConsentDetailState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * One case per `ui.yaml` action_contract, proving each interactive surface routes what it declares
 * (`navigate_reconfirm`, `confirm_revoke`, `dismiss_revoke_confirm`, `execute_revoke`, `retry_load`,
 * `navigate_back` from the empty state) — the 100% on_click coverage the behaviour gate requires.
 */
@OptIn(ExperimentalTestApi::class)
class ConsentDetailActionTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun androidx.compose.ui.test.ComposeUiTest.render(
        state: ConsentDetailState,
        onAction: (ConsentDetailAction) -> Unit = {},
        onReconfirm: () -> Unit = {},
        onGoBack: () -> Unit = {},
    ) {
        setContent {
            ConsentDetailScreenContent(
                state = state,
                onAction = onAction,
                onReconfirm = onReconfirm,
                onGoBack = onGoBack,
            )
        }
    }

    /** The body is a `LazyColumn`; the revoke button sits below ten permission rows. */
    private fun androidx.compose.ui.test.ComposeUiTest.scrollTo(targetTag: String) {
        onNodeWithTag(ConsentDetailTestTags.CONTENT_LIST).performScrollToNode(hasTestTag(targetTag))
    }

    /** navigate_reconfirm — the tonal button raises the host route, dispatching no action. */
    @Test
    fun theReconfirmButtonRaisesTheHostRoute() = runComposeUiTest {
        val actions = mutableListOf<ConsentDetailAction>()
        var reconfirmed = false
        render(
            state = ConsentDetailFixtures.contentState(),
            onAction = { actions.add(it) },
            onReconfirm = { reconfirmed = true },
        )

        scrollTo(ConsentDetailTestTags.RECONFIRM_BUTTON)
        onNodeWithTag(ConsentDetailTestTags.RECONFIRM_BUTTON).performClick()

        assertTrue(reconfirmed)
        assertTrue(actions.isEmpty())
    }

    /** confirm_revoke — tapping Revoke only opens the gate; it must not issue anything. */
    @Test
    fun theRevokeButtonDispatchesConfirmRevoke() = runComposeUiTest {
        val actions = mutableListOf<ConsentDetailAction>()
        render(state = ConsentDetailFixtures.contentState(), onAction = { actions.add(it) })

        scrollTo(ConsentDetailTestTags.REVOKE_BUTTON)
        onNodeWithTag(ConsentDetailTestTags.REVOKE_BUTTON).performClick()

        assertEquals(listOf<ConsentDetailAction>(ConsentDetailAction.ConfirmRevoke), actions)
    }

    /** dismiss_revoke_confirm — "Keep access" backs out without revoking. */
    @Test
    fun theDialogCancelDispatchesDismiss() = runComposeUiTest {
        val actions = mutableListOf<ConsentDetailAction>()
        render(state = ConsentDetailFixtures.revokeConfirmState(), onAction = { actions.add(it) })

        onNodeWithTag(ConsentDetailTestTags.REVOKE_DIALOG_CANCEL).performClick()

        assertEquals(listOf<ConsentDetailAction>(ConsentDetailAction.DismissRevokeConfirm), actions)
    }

    /** execute_revoke — "Yes, revoke" is the only surface that issues the DELETE. */
    @Test
    fun theDialogConfirmDispatchesExecuteRevoke() = runComposeUiTest {
        val actions = mutableListOf<ConsentDetailAction>()
        render(state = ConsentDetailFixtures.revokeConfirmState(), onAction = { actions.add(it) })

        onNodeWithTag(ConsentDetailTestTags.REVOKE_DIALOG_CONFIRM).performClick()

        assertEquals(listOf<ConsentDetailAction>(ConsentDetailAction.ExecuteRevoke), actions)
    }

    /** retry_load — the error state's Retry re-issues the load. */
    @Test
    fun theRetryButtonDispatchesRetryLoad() = runComposeUiTest {
        val actions = mutableListOf<ConsentDetailAction>()
        render(state = ConsentDetailFixtures.errorState(), onAction = { actions.add(it) })

        onNodeWithTag(ConsentDetailTestTags.RETRY_BUTTON).performClick()

        assertEquals(listOf<ConsentDetailAction>(ConsentDetailAction.RetryLoad), actions)
    }

    /** navigate_back — the empty state's only route out is the list. */
    @Test
    fun theEmptyStateGoBackRaisesTheHostRoute() = runComposeUiTest {
        val actions = mutableListOf<ConsentDetailAction>()
        var wentBack = false
        render(
            state = ConsentDetailFixtures.emptyState(),
            onAction = { actions.add(it) },
            onGoBack = { wentBack = true },
        )

        onNodeWithTag(ConsentDetailTestTags.GO_BACK_BUTTON).performClick()

        assertTrue(wentBack)
        assertTrue(actions.isEmpty())
    }

    /**
     * The content behind the gate has its actions disabled, so nothing can be re-dispatched from it.
     *
     * Rendered directly rather than through the gate state, because the dialog is modal: reaching
     * the button underneath it through the scrim is not something a real tap can do either, so
     * driving it that way would be testing the scrim rather than the disabled flag.
     */
    @Test
    fun theRevokeButtonIsInertWhenActionsAreDisabled() = runComposeUiTest {
        val actions = mutableListOf<ConsentDetailAction>()
        setContent {
            ConsentDetailContent(
                consent = ConsentDetailFixtures.consentUi(),
                onReconfirm = {},
                onRevokeClick = { actions.add(ConsentDetailAction.ConfirmRevoke) },
                actionsEnabled = false,
            )
        }

        scrollTo(ConsentDetailTestTags.REVOKE_BUTTON)
        onNodeWithTag(ConsentDetailTestTags.REVOKE_BUTTON).performClick()

        assertTrue(actions.isEmpty())
    }

    /** The revoking state offers nothing tappable — that is what prevents a second DELETE. */
    @Test
    fun theRevokingStateExposesNoActions() = runComposeUiTest {
        render(state = ConsentDetailFixtures.revokingState())

        onNodeWithTag(ConsentDetailTestTags.REVOKE_BUTTON).assertDoesNotExist()
        onNodeWithTag(ConsentDetailTestTags.REVOKE_DIALOG).assertDoesNotExist()
        onNodeWithTag(ConsentDetailTestTags.RETRY_BUTTON).assertDoesNotExist()
        onNodeWithTag(ConsentDetailTestTags.REVOKING_PROGRESS).assertExists()
    }
}
