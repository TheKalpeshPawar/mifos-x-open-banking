/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.paymentconsent

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.feature.paymentconsent.ui.PaymentConsentAction
import org.mifosx.openbanking.feature.paymentconsent.ui.PaymentConsentErrorKind
import org.mifosx.openbanking.feature.paymentconsent.ui.PaymentConsentState
import org.mifosx.openbanking.feature.paymentconsent.ui.PaymentConsentUiState
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val CONSENT_ID = "812774903"

/**
 * androidInstrumentedTest does not see commonTest, so the state fixtures are inlined here.
 */
private fun validatingState(): PaymentConsentState =
    PaymentConsentState(uiState = PaymentConsentUiState.Validating, consentId = CONSENT_ID)

private fun exchangingState(): PaymentConsentState =
    PaymentConsentState(uiState = PaymentConsentUiState.Exchanging, consentId = CONSENT_ID)

private fun checkingState(canCheckAgain: Boolean = false): PaymentConsentState = PaymentConsentState(
    uiState = PaymentConsentUiState.Checking(canCheckAgain = canCheckAgain),
    consentId = CONSENT_ID,
)

private fun approvedState(): PaymentConsentState =
    PaymentConsentState(uiState = PaymentConsentUiState.Approved, consentId = CONSENT_ID)

private fun alreadySubmittedState(): PaymentConsentState =
    PaymentConsentState(uiState = PaymentConsentUiState.AlreadySubmitted, consentId = CONSENT_ID)

private fun submittingState(): PaymentConsentState =
    PaymentConsentState(uiState = PaymentConsentUiState.Submitting, consentId = CONSENT_ID)

private fun errorState(kind: PaymentConsentErrorKind): PaymentConsentState =
    PaymentConsentState(uiState = PaymentConsentUiState.Error(kind), consentId = CONSENT_ID)

/**
 * On-device mirror of [PaymentConsentScreenRobolectricTest], driving the same
 * [PaymentConsentTestTags] so a divergence between the JVM and device renderers is visible.
 */
@RunWith(AndroidJUnit4::class)
class PaymentConsentScreenInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<PaymentConsentAction>()

    private fun render(state: PaymentConsentState) {
        composeRule.setContent {
            PaymentConsentScreenContent(state = state, onAction = { actions.add(it) })
        }
    }

    /** The three return stages share one visual, so only their own tags distinguish them. */
    @Test
    fun validatingRendersOnlyItsOwnStage() {
        render(validatingState())

        composeRule.onNodeWithTag(PaymentConsentTestTags.VALIDATING_STATE).assertIsDisplayed()
        composeRule.onNodeWithTag(PaymentConsentTestTags.PROGRESS_DETAIL).assertIsDisplayed()
        composeRule.onNodeWithTag(PaymentConsentTestTags.EXCHANGING_STATE).assertDoesNotExist()
        composeRule.onNodeWithTag(PaymentConsentTestTags.CHECKING_STATE).assertDoesNotExist()
    }

    @Test
    fun exchangingRendersOnlyItsOwnStage() {
        render(exchangingState())

        composeRule.onNodeWithTag(PaymentConsentTestTags.EXCHANGING_STATE).assertIsDisplayed()
        composeRule.onNodeWithTag(PaymentConsentTestTags.VALIDATING_STATE).assertDoesNotExist()
    }

    @Test
    fun checkingOffersNothingUntilTheFirstPollComesBack() {
        render(checkingState())

        composeRule.onNodeWithTag(PaymentConsentTestTags.CHECKING_STATE).assertIsDisplayed()
        composeRule.onNodeWithTag(PaymentConsentTestTags.CHECK_AGAIN_BUTTON).assertDoesNotExist()
    }

    @Test
    fun aConsentStillAwaitingAuthorisationExplainsTheWaitAndRepolls() {
        render(checkingState(canCheckAgain = true))

        composeRule.onNodeWithTag(PaymentConsentTestTags.CHECKING_HINT).assertIsDisplayed()
        composeRule.onNodeWithTag(PaymentConsentTestTags.CHECK_AGAIN_BUTTON).performClick()

        assertEquals(listOf<PaymentConsentAction>(PaymentConsentAction.CheckAgain), actions)
    }

    @Test
    fun approvedRendersTheAuthorisedPanelAndNoExits() {
        render(approvedState())

        composeRule.onNodeWithTag(PaymentConsentTestTags.AUTHORISED_STATE).assertIsDisplayed()
        composeRule.onNodeWithTag(PaymentConsentTestTags.PROGRESS_INDICATOR).assertDoesNotExist()
        composeRule.onNodeWithTag(PaymentConsentTestTags.RESTART_BUTTON).assertDoesNotExist()
        assertTrue(actions.isEmpty())
    }

    /** The bank already created this payment: no reassurance, and no one-tap way to send another. */
    @Test
    fun alreadySubmittedNeitherReassuresNorOffersARerun() {
        render(alreadySubmittedState())

        composeRule.onNodeWithTag(PaymentConsentTestTags.ALREADY_SUBMITTED_STATE).assertIsDisplayed()
        composeRule.onNodeWithTag(PaymentConsentTestTags.NO_MONEY_MOVED).assertDoesNotExist()
        composeRule.onNodeWithTag(PaymentConsentTestTags.RESTART_BUTTON).assertDoesNotExist()
    }

    @Test
    fun submittingWarnsAgainstLeavingAndOffersNoExit() {
        render(submittingState())

        composeRule.onNodeWithTag(PaymentConsentTestTags.SUBMITTING_STATE).assertIsDisplayed()
        composeRule.onNodeWithTag(PaymentConsentTestTags.SUBMITTING_WARNING).assertIsDisplayed()
        composeRule.onNodeWithTag(PaymentConsentTestTags.ABANDON_BUTTON).assertDoesNotExist()
    }

    @Test
    fun aDeclinedPaymentGetsItsOwnPanel() {
        render(errorState(PaymentConsentErrorKind.ConsentRejected))

        composeRule.onNodeWithTag(PaymentConsentTestTags.DECLINED_STATE).assertIsDisplayed()
        composeRule.onNodeWithTag(PaymentConsentTestTags.NO_MONEY_MOVED).assertIsDisplayed()
        composeRule.onNodeWithTag(PaymentConsentTestTags.ERROR_STATE).assertDoesNotExist()
    }

    @Test
    fun aTimedOutAuthorisationRendersTheExpiredPanel() {
        render(errorState(PaymentConsentErrorKind.AuthorisationTimedOut))

        composeRule.onNodeWithTag(PaymentConsentTestTags.EXPIRED_STATE).assertIsDisplayed()
        composeRule.onNodeWithTag(PaymentConsentTestTags.NO_MONEY_MOVED).assertIsDisplayed()
    }

    @Test
    fun aConnectionFailureGetsItsOwnPanel() {
        render(errorState(PaymentConsentErrorKind.NetworkError))

        composeRule.onNodeWithTag(PaymentConsentTestTags.CONNECTION_FAILED_STATE).assertIsDisplayed()
        composeRule.onNodeWithTag(PaymentConsentTestTags.NO_MONEY_MOVED).assertIsDisplayed()
    }

    /**
     * Once the instruction is with the bank the app cannot know whether it was taken, so this is the
     * one outcome that asks the customer to check rather than promising nothing happened.
     */
    @Test
    fun aFailedSubmissionDoesNotPromiseThatNothingMoved() {
        render(errorState(PaymentConsentErrorKind.SubmissionFailed))

        composeRule.onNodeWithTag(PaymentConsentTestTags.OUTCOME_BODY).assertIsDisplayed()
        composeRule.onNodeWithTag(PaymentConsentTestTags.NO_MONEY_MOVED).assertDoesNotExist()
    }

    /**
     * The duplicate-payment guard: this outcome may already have taken the money, so it must not
     * offer a one-tap re-run under copy telling the customer to go and check.
     */
    @Test
    fun anUnconfirmedSubmissionNeverOffersAOneTapRerun() {
        render(errorState(PaymentConsentErrorKind.SubmissionFailed))

        composeRule.onNodeWithTag(PaymentConsentTestTags.RESTART_BUTTON).assertDoesNotExist()
        composeRule.onNodeWithTag(PaymentConsentTestTags.ABANDON_BUTTON).assertIsDisplayed()
    }

    @Test
    fun nothingPendingOffersOnlyAWayOut() {
        render(errorState(PaymentConsentErrorKind.NoPendingAuthorisation))

        composeRule.onNodeWithTag(PaymentConsentTestTags.RESTART_BUTTON).assertDoesNotExist()
        composeRule.onNodeWithTag(PaymentConsentTestTags.ABANDON_BUTTON).performClick()

        assertEquals(listOf<PaymentConsentAction>(PaymentConsentAction.AbandonPayment), actions)
    }

    @Test
    fun anOutcomeOffersBothRestartAndAbandon() {
        render(errorState(PaymentConsentErrorKind.ConsentRejected))

        composeRule.onNodeWithTag(PaymentConsentTestTags.RESTART_BUTTON).performClick()
        composeRule.onNodeWithTag(PaymentConsentTestTags.ABANDON_BUTTON).performClick()

        assertEquals(
            listOf<PaymentConsentAction>(
                PaymentConsentAction.RetryAuthorisation,
                PaymentConsentAction.AbandonPayment,
            ),
            actions,
        )
    }
}
