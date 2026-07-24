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

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.feature.consentdetail.ui.ConsentDetailAction
import org.mifosx.openbanking.feature.consentdetail.ui.ConsentDetailState
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val ROBOLECTRIC_SDK = 34
private const val WARNING_DAYS = 3

/**
 * Renders [ConsentDetailScreenContent] across its states under Robolectric (JVM, no device) and
 * drives it through the shared [ConsentDetailTestTags]. A verbatim on-device mirror lives in
 * [ConsentDetailScreenInstrumentedTest].
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [ROBOLECTRIC_SDK])
class ConsentDetailScreenRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<ConsentDetailAction>()
    private var reconfirmed = false
    private var wentBack = false

    private fun render(state: ConsentDetailState) {
        composeRule.setContent {
            ConsentDetailScreenContent(
                state = state,
                onAction = { actions.add(it) },
                onReconfirm = { reconfirmed = true },
                onGoBack = { wentBack = true },
            )
        }
    }

    /** The body is a `LazyColumn`; the actions sit below ten permission rows. */
    private fun scrollTo(targetTag: String) {
        composeRule.onNodeWithTag(ConsentDetailTestTags.CONTENT_LIST)
            .performScrollToNode(hasTestTag(targetTag))
    }

    @Test
    fun contentRendersStatusDatesAndPermissions() {
        render(ConsentDetailFixtures.contentState())

        composeRule.onNodeWithTag(ConsentDetailTestTags.STATUS_CARD).assertExists()
        composeRule.onNodeWithTag(ConsentDetailTestTags.DATES_LIST).assertExists()
        scrollTo(ConsentDetailTestTags.PERMISSIONS_HEADER)
        composeRule.onNodeWithTag(ConsentDetailTestTags.PERMISSIONS_HEADER).assertExists()
    }

    @Test
    fun theExpiryBannerRendersOnlyInsideTheWarningWindow() {
        render(ConsentDetailFixtures.contentState(expiryWarningDays = WARNING_DAYS))

        composeRule.onNodeWithTag(ConsentDetailTestTags.EXPIRY_BANNER).assertExists()
    }

    @Test
    fun theExpiryBannerIsAbsentOutsideTheWarningWindow() {
        render(ConsentDetailFixtures.contentState())

        composeRule.onNodeWithTag(ConsentDetailTestTags.EXPIRY_BANNER).assertDoesNotExist()
    }

    @Test
    fun theReconfirmButtonRaisesTheHostRoute() {
        render(ConsentDetailFixtures.contentState())

        scrollTo(ConsentDetailTestTags.RECONFIRM_BUTTON)
        composeRule.onNodeWithTag(ConsentDetailTestTags.RECONFIRM_BUTTON).performClick()

        assertTrue(reconfirmed)
    }

    @Test
    fun theRevokeButtonOnlyOpensTheGate() {
        render(ConsentDetailFixtures.contentState())

        scrollTo(ConsentDetailTestTags.REVOKE_BUTTON)
        composeRule.onNodeWithTag(ConsentDetailTestTags.REVOKE_BUTTON).performClick()

        assertEquals(listOf<ConsentDetailAction>(ConsentDetailAction.ConfirmRevoke), actions)
    }

    @Test
    fun theGateOffersKeepAccessAndYesRevoke() {
        render(ConsentDetailFixtures.revokeConfirmState())

        composeRule.onNodeWithTag(ConsentDetailTestTags.REVOKE_DIALOG_TITLE).assertExists()
        composeRule.onNodeWithTag(ConsentDetailTestTags.REVOKE_DIALOG_CANCEL).assertExists()
        composeRule.onNodeWithTag(ConsentDetailTestTags.REVOKE_DIALOG_CONFIRM).assertExists()
    }

    @Test
    fun keepAccessDismissesTheGate() {
        render(ConsentDetailFixtures.revokeConfirmState())

        composeRule.onNodeWithTag(ConsentDetailTestTags.REVOKE_DIALOG_CANCEL).performClick()

        assertEquals(listOf<ConsentDetailAction>(ConsentDetailAction.DismissRevokeConfirm), actions)
    }

    @Test
    fun yesRevokeIsTheOnlySurfaceThatExecutes() {
        render(ConsentDetailFixtures.revokeConfirmState())

        composeRule.onNodeWithTag(ConsentDetailTestTags.REVOKE_DIALOG_CONFIRM).performClick()

        assertEquals(listOf<ConsentDetailAction>(ConsentDetailAction.ExecuteRevoke), actions)
    }

    @Test
    fun theRevokingStateLocksTheScreenDown() {
        render(ConsentDetailFixtures.revokingState())

        composeRule.onNodeWithTag(ConsentDetailTestTags.REVOKING_PROGRESS).assertExists()
        composeRule.onNodeWithTag(ConsentDetailTestTags.CONTENT_LIST).assertDoesNotExist()
        composeRule.onNodeWithTag(ConsentDetailTestTags.REVOKE_BUTTON).assertDoesNotExist()
        composeRule.onNodeWithTag(ConsentDetailTestTags.REVOKE_DIALOG).assertDoesNotExist()
    }

    @Test
    fun loadingRendersTheSpinnerNotTheContent() {
        render(ConsentDetailFixtures.loadingState())

        composeRule.onNodeWithTag(ConsentDetailTestTags.LOADING).assertExists()
        composeRule.onNodeWithTag(ConsentDetailTestTags.CONTENT_LIST).assertDoesNotExist()
    }

    @Test
    fun emptyOffersOnlyTheRouteBackToTheList() {
        render(ConsentDetailFixtures.emptyState())

        composeRule.onNodeWithTag(ConsentDetailTestTags.GO_BACK_BUTTON).performClick()

        assertTrue(wentBack)
        assertTrue(actions.isEmpty())
    }

    @Test
    fun errorDispatchesRetryLoad() {
        render(ConsentDetailFixtures.errorState())

        composeRule.onNodeWithTag(ConsentDetailTestTags.RETRY_BUTTON).performClick()

        assertEquals(listOf<ConsentDetailAction>(ConsentDetailAction.RetryLoad), actions)
    }
}
