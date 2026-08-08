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

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.feature.paymentconsent.ui.PaymentConsentAction
import org.mifosx.openbanking.feature.paymentconsent.ui.PaymentConsentErrorKind
import org.mifosx.openbanking.feature.paymentconsent.ui.PaymentConsentState
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val ROBOLECTRIC_SDK = 34
private const val PHONE = "w412dp-h892dp"

/**
 * Renders [PaymentConsentScreenContent] across its states under Robolectric (JVM, no device) and
 * drives it through the shared [PaymentConsentTestTags]. A verbatim on-device mirror lives in
 * [PaymentConsentScreenInstrumentedTest].
 *
 * Named `…RobolectricTest` rather than `…ScreenUiTest` deliberately: the module's `Test` task filter
 * excludes `*ScreenUiTest` from JVM unit-test runs, so a class named that way would be silently
 * skipped here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [ROBOLECTRIC_SDK], qualifiers = PHONE)
class PaymentConsentScreenRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<PaymentConsentAction>()

    private fun render(state: PaymentConsentState) {
        composeRule.setContent {
            PaymentConsentScreenContent(state = state, onAction = { actions.add(it) })
        }
    }

    // region — the return leg's three stages

    /**
     * The point of the distinct tags. All three stages draw the same spinner over the same layout,
     * so the shared progress tags cannot tell a suite which one it captured.
     */
    @Test
    fun validatingRendersOnlyItsOwnStage() {
        render(PaymentConsentFixtures.validatingState())

        composeRule.onNodeWithTag(PaymentConsentTestTags.VALIDATING_STATE).assertIsDisplayed()
        composeRule.onNodeWithTag(PaymentConsentTestTags.PROGRESS_DETAIL).assertIsDisplayed()
        composeRule.onNodeWithTag(PaymentConsentTestTags.EXCHANGING_STATE).assertDoesNotExist()
        composeRule.onNodeWithTag(PaymentConsentTestTags.CHECKING_STATE).assertDoesNotExist()
    }

    @Test
    fun exchangingRendersOnlyItsOwnStage() {
        render(PaymentConsentFixtures.exchangingState())

        composeRule.onNodeWithTag(PaymentConsentTestTags.EXCHANGING_STATE).assertIsDisplayed()
        composeRule.onNodeWithTag(PaymentConsentTestTags.VALIDATING_STATE).assertDoesNotExist()
    }

    /** While the first poll is in flight there is nothing useful to offer yet. */
    @Test
    fun checkingOffersNothingUntilTheFirstPollComesBack() {
        render(PaymentConsentFixtures.checkingState())

        composeRule.onNodeWithTag(PaymentConsentTestTags.CHECKING_STATE).assertIsDisplayed()
        composeRule.onNodeWithTag(PaymentConsentTestTags.CHECK_AGAIN_BUTTON).assertDoesNotExist()
        composeRule.onNodeWithTag(PaymentConsentTestTags.CHECKING_HINT).assertDoesNotExist()
    }

    @Test
    fun aConsentStillAwaitingAuthorisationExplainsTheWaitAndRepolls() {
        render(PaymentConsentFixtures.checkingState(canCheckAgain = true))

        composeRule.onNodeWithTag(PaymentConsentTestTags.CHECKING_HINT).assertIsDisplayed()
        composeRule.onNodeWithTag(PaymentConsentTestTags.CHECK_AGAIN_BUTTON).performClick()

        assertEquals(listOf<PaymentConsentAction>(PaymentConsentAction.CheckAgain), actions)
    }

    @Test
    fun confirmingFundsNamesTheStageItHasReached() {
        render(PaymentConsentFixtures.confirmingFundsState())

        composeRule.onNodeWithTag(PaymentConsentTestTags.CONFIRMING_FUNDS_STATE).assertIsDisplayed()
        composeRule.onNodeWithTag(PaymentConsentTestTags.PROGRESS_DETAIL).assertIsDisplayed()
    }

    /**
     * The instruction is with the bank at this point, so the screen tells the customer to wait and
     * offers nothing to tap — there is no exit that would undo anything.
     */
    @Test
    fun submittingWarnsAgainstLeavingAndOffersNoExit() {
        render(PaymentConsentFixtures.submittingState())

        composeRule.onNodeWithTag(PaymentConsentTestTags.SUBMITTING_STATE).assertIsDisplayed()
        composeRule.onNodeWithTag(PaymentConsentTestTags.SUBMITTING_WARNING).assertIsDisplayed()
        composeRule.onNodeWithTag(PaymentConsentTestTags.ABANDON_BUTTON).assertDoesNotExist()
        composeRule.onNodeWithTag(PaymentConsentTestTags.CHECK_AGAIN_BUTTON).assertDoesNotExist()
    }

    // endregion

    // region — approved

    /** The tag existed long before anything emitted it. This is the state that finally does. */
    @Test
    fun approvedRendersTheAuthorisedPanelWithoutASpinner() {
        render(PaymentConsentFixtures.approvedState())

        composeRule.onNodeWithTag(PaymentConsentTestTags.AUTHORISED_STATE).assertIsDisplayed()
        composeRule.onNodeWithTag(PaymentConsentTestTags.OUTCOME_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(PaymentConsentTestTags.PROGRESS_INDICATOR).assertDoesNotExist()
    }

    /** Approval is not an ending: there is nothing here to restart or abandon. */
    @Test
    fun approvedOffersNoExits() {
        render(PaymentConsentFixtures.approvedState())

        composeRule.onNodeWithTag(PaymentConsentTestTags.RESTART_BUTTON).assertDoesNotExist()
        composeRule.onNodeWithTag(PaymentConsentTestTags.ABANDON_BUTTON).assertDoesNotExist()
        assertTrue(actions.isEmpty())
    }

    /**
     * The strongest duplicate-payment case: the app knows a payment exists. It must not claim the
     * money is safe, and must not offer a one-tap way to send another.
     */
    @Test
    fun alreadySubmittedNeitherReassuresNorOffersARerun() {
        render(PaymentConsentFixtures.alreadySubmittedState())

        composeRule.onNodeWithTag(PaymentConsentTestTags.ALREADY_SUBMITTED_STATE).assertIsDisplayed()
        composeRule.onNodeWithTag(PaymentConsentTestTags.NO_MONEY_MOVED).assertDoesNotExist()
        composeRule.onNodeWithTag(PaymentConsentTestTags.RESTART_BUTTON).assertDoesNotExist()
        composeRule.onNodeWithTag(PaymentConsentTestTags.AUTHORISED_STATE).assertDoesNotExist()
    }

    // endregion

    // region — the three failures with a panel of their own

    @Test
    fun aDeclinedPaymentGetsItsOwnPanel() {
        render(PaymentConsentFixtures.errorState(PaymentConsentErrorKind.ConsentRejected))

        composeRule.onNodeWithTag(PaymentConsentTestTags.DECLINED_STATE).assertIsDisplayed()
        composeRule.onNodeWithTag(PaymentConsentTestTags.ERROR_STATE).assertDoesNotExist()
    }

    /** Expiry has two producers — a spent code and a bank that never confirmed — and one panel. */
    @Test
    fun aSpentCodeRendersTheExpiredPanel() {
        render(PaymentConsentFixtures.errorState(PaymentConsentErrorKind.CodeExpired))

        composeRule.onNodeWithTag(PaymentConsentTestTags.EXPIRED_STATE).assertIsDisplayed()
    }

    @Test
    fun aTimedOutAuthorisationRendersTheSameExpiredPanel() {
        render(PaymentConsentFixtures.errorState(PaymentConsentErrorKind.AuthorisationTimedOut))

        composeRule.onNodeWithTag(PaymentConsentTestTags.EXPIRED_STATE).assertIsDisplayed()
    }

    @Test
    fun aConnectionFailureGetsItsOwnPanel() {
        render(PaymentConsentFixtures.errorState(PaymentConsentErrorKind.NetworkError))

        composeRule.onNodeWithTag(PaymentConsentTestTags.CONNECTION_FAILED_STATE).assertIsDisplayed()
        composeRule.onNodeWithTag(PaymentConsentTestTags.ERROR_STATE).assertDoesNotExist()
    }

    // endregion

    // region — the reassurance, and the one place it must not appear

    /**
     * Swept through one composition rather than one per kind, because the rule may only be given
     * content once. Every kind bar the submission failure has to make the promise.
     */
    @Test
    fun everyOutcomeThatCanPromiseItSaysNoMoneyHasMoved() {
        val kind = mutableStateOf(PaymentConsentErrorKind.StateMismatch)
        composeRule.setContent {
            PaymentConsentScreenContent(
                state = PaymentConsentFixtures.errorState(kind.value),
                onAction = {},
            )
        }

        (PaymentConsentErrorKind.entries - PaymentConsentErrorKind.SubmissionFailed).forEach {
            kind.value = it
            composeRule.waitForIdle()
            composeRule.onNodeWithTag(PaymentConsentTestTags.NO_MONEY_MOVED).assertIsDisplayed()
        }
    }

    /**
     * Once the instruction is with the bank the app cannot know whether it was taken, so this
     * outcome asks the customer to check their transactions instead of promising nothing happened.
     */
    @Test
    fun aFailedSubmissionDoesNotPromiseThatNothingMoved() {
        render(PaymentConsentFixtures.errorState(PaymentConsentErrorKind.SubmissionFailed))

        composeRule.onNodeWithTag(PaymentConsentTestTags.OUTCOME_BODY).assertIsDisplayed()
        composeRule.onNodeWithTag(PaymentConsentTestTags.NO_MONEY_MOVED).assertDoesNotExist()
    }

    // endregion

    // region — the two exits

    /**
     * The duplicate-payment guard. This outcome's copy says the money may already be gone and to go
     * and check; a primary "Authorise again" underneath that invites the second payment the copy is
     * warning about, on a rail where a consent cannot be cancelled.
     */
    @Test
    fun anUnconfirmedSubmissionNeverOffersAOneTapRerun() {
        render(PaymentConsentFixtures.errorState(PaymentConsentErrorKind.SubmissionFailed))

        composeRule.onNodeWithTag(PaymentConsentTestTags.RESTART_BUTTON).assertDoesNotExist()
        composeRule.onNodeWithTag(PaymentConsentTestTags.ABANDON_BUTTON).assertIsDisplayed()
    }

    /** Nothing was in flight, so there is nothing to authorise again — only a way out. */
    @Test
    fun nothingPendingOffersOnlyAWayOut() {
        render(PaymentConsentFixtures.errorState(PaymentConsentErrorKind.NoPendingAuthorisation))

        composeRule.onNodeWithTag(PaymentConsentTestTags.RESTART_BUTTON).assertDoesNotExist()
        composeRule.onNodeWithTag(PaymentConsentTestTags.ABANDON_BUTTON).performClick()

        assertEquals(listOf<PaymentConsentAction>(PaymentConsentAction.AbandonPayment), actions)
    }

    /** Both exits are on the failures where restarting and abandoning are equally legitimate. */
    @Test
    fun anOutcomeOffersBothRestartAndAbandon() {
        render(PaymentConsentFixtures.errorState(PaymentConsentErrorKind.ConsentRejected))

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

    // endregion
}
