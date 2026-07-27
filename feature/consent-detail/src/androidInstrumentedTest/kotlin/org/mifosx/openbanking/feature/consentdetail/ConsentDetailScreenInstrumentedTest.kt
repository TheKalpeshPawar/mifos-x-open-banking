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
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.core.model.callback.ConsentStatus
import org.mifosx.openbanking.feature.consentdetail.ui.ConsentDetailAction
import org.mifosx.openbanking.feature.consentdetail.ui.ConsentDetailErrorKind
import org.mifosx.openbanking.feature.consentdetail.ui.ConsentDetailState
import org.mifosx.openbanking.feature.consentdetail.ui.ConsentDetailUi
import org.mifosx.openbanking.feature.consentdetail.ui.ConsentDetailUiState
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val CONSENT_ID = "aac-fb2c4e8a-7d31-4c9e-9f2a-1b3c5d7e9f01"
private const val WARNING_DAYS = 3

/**
 * androidInstrumentedTest does not see commonTest, so the state fixtures are inlined here.
 */
private fun consentUi(expiryWarningDays: Int? = null) = ConsentDetailUi(
    consentId = CONSENT_ID,
    status = ConsentStatus.Authorised,
    permissions = listOf("ReadAccountsDetail", "ReadBalances", "ReadTransactionsDetail"),
    connectedDate = "28 Jun 2026",
    expiresDate = "26 Sep 2026",
    transactionFromDate = "30 Mar 2026",
    transactionToDate = "28 Jun 2026",
    expiryWarningDays = expiryWarningDays,
)

private fun contentState(expiryWarningDays: Int? = null) = ConsentDetailState(
    consentId = CONSENT_ID,
    uiState = ConsentDetailUiState.Content(consentUi(expiryWarningDays)),
)

private fun revokeConfirmState() = ConsentDetailState(
    consentId = CONSENT_ID,
    uiState = ConsentDetailUiState.RevokeConfirm(consentUi()),
)

private fun revokingState() = ConsentDetailState(
    consentId = CONSENT_ID,
    uiState = ConsentDetailUiState.Revoking(consentUi()),
)

private fun loadingState() = ConsentDetailState(consentId = CONSENT_ID, uiState = ConsentDetailUiState.Loading)

private fun emptyState() = ConsentDetailState(consentId = CONSENT_ID, uiState = ConsentDetailUiState.Empty)

private fun errorState() = ConsentDetailState(
    consentId = CONSENT_ID,
    uiState = ConsentDetailUiState.Error(ConsentDetailErrorKind.ConsentNotFound),
)

/**
 * On-device mirror of [ConsentDetailScreenRobolectricTest], driving the same
 * [ConsentDetailTestTags] so a divergence between the JVM and device renderers is visible.
 */
@RunWith(AndroidJUnit4::class)
class ConsentDetailScreenInstrumentedTest {

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

    private fun scrollTo(targetTag: String) {
        composeRule.onNodeWithTag(ConsentDetailTestTags.CONTENT_LIST)
            .performScrollToNode(hasTestTag(targetTag))
    }

    @Test
    fun contentRendersStatusAndDates() {
        render(contentState())

        composeRule.onNodeWithTag(ConsentDetailTestTags.STATUS_CARD).assertExists()
        composeRule.onNodeWithTag(ConsentDetailTestTags.DATES_LIST).assertExists()
    }

    @Test
    fun theExpiryBannerRendersOnlyInsideTheWarningWindow() {
        render(contentState(expiryWarningDays = WARNING_DAYS))

        composeRule.onNodeWithTag(ConsentDetailTestTags.EXPIRY_BANNER).assertExists()
    }

    @Test
    fun theReconfirmButtonRaisesTheHostRoute() {
        render(contentState())

        scrollTo(ConsentDetailTestTags.RECONFIRM_BUTTON)
        composeRule.onNodeWithTag(ConsentDetailTestTags.RECONFIRM_BUTTON).performClick()

        assertTrue(reconfirmed)
    }

    @Test
    fun theRevokeButtonOnlyOpensTheGate() {
        render(contentState())

        scrollTo(ConsentDetailTestTags.REVOKE_BUTTON)
        composeRule.onNodeWithTag(ConsentDetailTestTags.REVOKE_BUTTON).performClick()

        assertEquals(listOf<ConsentDetailAction>(ConsentDetailAction.ConfirmRevoke), actions)
    }

    @Test
    fun keepAccessDismissesTheGate() {
        render(revokeConfirmState())

        composeRule.onNodeWithTag(ConsentDetailTestTags.REVOKE_DIALOG_CANCEL).performClick()

        assertEquals(listOf<ConsentDetailAction>(ConsentDetailAction.DismissRevokeConfirm), actions)
    }

    @Test
    fun yesRevokeIsTheOnlySurfaceThatExecutes() {
        render(revokeConfirmState())

        composeRule.onNodeWithTag(ConsentDetailTestTags.REVOKE_DIALOG_CONFIRM).performClick()

        assertEquals(listOf<ConsentDetailAction>(ConsentDetailAction.ExecuteRevoke), actions)
    }

    @Test
    fun theRevokingStateLocksTheScreenDown() {
        render(revokingState())

        composeRule.onNodeWithTag(ConsentDetailTestTags.REVOKING_PROGRESS).assertExists()
        composeRule.onNodeWithTag(ConsentDetailTestTags.CONTENT_LIST).assertDoesNotExist()
        composeRule.onNodeWithTag(ConsentDetailTestTags.REVOKE_BUTTON).assertDoesNotExist()
    }

    @Test
    fun loadingRendersTheSpinnerNotTheContent() {
        render(loadingState())

        composeRule.onNodeWithTag(ConsentDetailTestTags.LOADING).assertExists()
        composeRule.onNodeWithTag(ConsentDetailTestTags.CONTENT_LIST).assertDoesNotExist()
    }

    @Test
    fun emptyOffersOnlyTheRouteBackToTheList() {
        render(emptyState())

        composeRule.onNodeWithTag(ConsentDetailTestTags.GO_BACK_BUTTON).performClick()

        assertTrue(wentBack)
        assertTrue(actions.isEmpty())
    }

    @Test
    fun errorDispatchesRetryLoad() {
        render(errorState())

        composeRule.onNodeWithTag(ConsentDetailTestTags.RETRY_BUTTON).performClick()

        assertEquals(listOf<ConsentDetailAction>(ConsentDetailAction.RetryLoad), actions)
    }
}
