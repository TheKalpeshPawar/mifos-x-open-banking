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

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.mifosx.openbanking.core.model.banking.payment.ConsentType
import org.mifosx.openbanking.feature.paymentconsent.ui.PaymentConsentAction
import org.mifosx.openbanking.feature.paymentconsent.ui.PaymentConsentErrorKind
import org.mifosx.openbanking.feature.paymentconsent.ui.PaymentConsentState
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class PaymentConsentScreenUiTest {

    private fun captureActions(state: PaymentConsentState, tag: String): List<PaymentConsentAction> {
        val actions = mutableListOf<PaymentConsentAction>()
        runComposeUiTest {
            setContent {
                PaymentConsentScreenContent(state = state, onAction = { actions += it })
            }
            onNodeWithTag(tag).performClick()
        }
        return actions
    }

    @Test
    fun validatingRendersProgressWithItsOwnDetail() = runComposeUiTest {
        setContent { PaymentConsentScreenContent(PaymentConsentFixtures.validatingState(), {}) }
        onNodeWithTag(PaymentConsentTestTags.PROGRESS_INDICATOR).assertIsDisplayed()
        onNodeWithTag(PaymentConsentTestTags.PROGRESS_DETAIL).assertIsDisplayed()
    }

    @Test
    fun exchangingRendersProgress() = runComposeUiTest {
        setContent { PaymentConsentScreenContent(PaymentConsentFixtures.exchangingState(), {}) }
        onNodeWithTag(PaymentConsentTestTags.PROGRESS_INDICATOR).assertIsDisplayed()
    }

    /**
     * The three return-leg stages share one visual, so the shared progress tags cannot tell them
     * apart — a suite asserting only on those would pass against whichever stage happened to be on
     * screen. Each therefore carries a tag no other stage does.
     */
    @Test
    fun eachReturnStageIsIdentifiableOnItsOwn() = runComposeUiTest {
        setContent { PaymentConsentScreenContent(PaymentConsentFixtures.validatingState(), {}) }
        onNodeWithTag(PaymentConsentTestTags.VALIDATING_STATE).assertIsDisplayed()
        onNodeWithTag(PaymentConsentTestTags.EXCHANGING_STATE).assertDoesNotExist()
        onNodeWithTag(PaymentConsentTestTags.CHECKING_STATE).assertDoesNotExist()
    }

    @Test
    fun exchangingCarriesItsOwnStateTag() = runComposeUiTest {
        setContent { PaymentConsentScreenContent(PaymentConsentFixtures.exchangingState(), {}) }
        onNodeWithTag(PaymentConsentTestTags.EXCHANGING_STATE).assertIsDisplayed()
        onNodeWithTag(PaymentConsentTestTags.VALIDATING_STATE).assertDoesNotExist()
    }

    @Test
    fun checkingCarriesItsOwnStateTag() = runComposeUiTest {
        setContent { PaymentConsentScreenContent(PaymentConsentFixtures.checkingState(), {}) }
        onNodeWithTag(PaymentConsentTestTags.CHECKING_STATE).assertIsDisplayed()
        onNodeWithTag(PaymentConsentTestTags.EXCHANGING_STATE).assertDoesNotExist()
    }

    /** The tag was declared long before anything emitted it. This is the state that finally does. */
    @Test
    fun approvedRendersTheAuthorisedPanel() = runComposeUiTest {
        setContent { PaymentConsentScreenContent(PaymentConsentFixtures.approvedState(), {}) }
        onNodeWithTag(PaymentConsentTestTags.AUTHORISED_STATE).assertIsDisplayed()
        onNodeWithTag(PaymentConsentTestTags.OUTCOME_TITLE).assertIsDisplayed()
        onNodeWithTag(PaymentConsentTestTags.PROGRESS_INDICATOR).assertDoesNotExist()
    }

    /** Approval is not an ending — there is nothing to restart or abandon from here. */
    @Test
    fun approvedOffersNoExits() = runComposeUiTest {
        setContent { PaymentConsentScreenContent(PaymentConsentFixtures.approvedState(), {}) }
        onNodeWithTag(PaymentConsentTestTags.RESTART_BUTTON).assertDoesNotExist()
        onNodeWithTag(PaymentConsentTestTags.ABANDON_BUTTON).assertDoesNotExist()
    }

    /**
     * The strongest duplicate-payment case on the screen: the app *knows* a payment exists. It must
     * neither claim the money is safe nor offer a one-tap way to send another.
     */
    @Test
    fun alreadySubmittedNeitherReassuresNorOffersARerun() = runComposeUiTest {
        setContent { PaymentConsentScreenContent(PaymentConsentFixtures.alreadySubmittedState(), {}) }
        onNodeWithTag(PaymentConsentTestTags.ALREADY_SUBMITTED_STATE).assertIsDisplayed()
        onNodeWithTag(PaymentConsentTestTags.NO_MONEY_MOVED).assertDoesNotExist()
        onNodeWithTag(PaymentConsentTestTags.RESTART_BUTTON).assertDoesNotExist()
        onNodeWithTag(PaymentConsentTestTags.ERROR_STATE).assertDoesNotExist()
    }

    /** It is not the approved panel either — that one is mid-flight, this one is finished. */
    @Test
    fun alreadySubmittedIsNotTheApprovedPanel() = runComposeUiTest {
        setContent { PaymentConsentScreenContent(PaymentConsentFixtures.alreadySubmittedState(), {}) }
        onNodeWithTag(PaymentConsentTestTags.AUTHORISED_STATE).assertDoesNotExist()
        onNodeWithTag(PaymentConsentTestTags.PROGRESS_INDICATOR).assertDoesNotExist()
    }

    @Test
    fun alreadySubmittedLeavesByItsSingleExit() {
        val actions = captureActions(
            PaymentConsentFixtures.alreadySubmittedState(),
            PaymentConsentTestTags.ABANDON_BUTTON,
        )

        assertEquals<List<PaymentConsentAction>>(listOf(PaymentConsentAction.AbandonPayment), actions)
    }

    @Test
    fun aDeclinedPaymentGetsItsOwnPanel() = runComposeUiTest {
        setContent {
            PaymentConsentScreenContent(
                PaymentConsentFixtures.errorState(PaymentConsentErrorKind.ConsentRejected),
                {},
            )
        }
        onNodeWithTag(PaymentConsentTestTags.DECLINED_STATE).assertIsDisplayed()
        onNodeWithTag(PaymentConsentTestTags.ERROR_STATE).assertDoesNotExist()
    }

    /** Expiry has two producers — a spent code and a bank that never confirmed — and one panel. */
    @Test
    fun bothWaysAnAuthorisationCanExpireShareTheExpiredPanel() = runComposeUiTest {
        setContent {
            PaymentConsentScreenContent(
                PaymentConsentFixtures.errorState(PaymentConsentErrorKind.CodeExpired),
                {},
            )
        }
        onNodeWithTag(PaymentConsentTestTags.EXPIRED_STATE).assertIsDisplayed()
    }

    @Test
    fun aTimedOutAuthorisationAlsoRendersTheExpiredPanel() = runComposeUiTest {
        setContent {
            PaymentConsentScreenContent(
                PaymentConsentFixtures.errorState(PaymentConsentErrorKind.AuthorisationTimedOut),
                {},
            )
        }
        onNodeWithTag(PaymentConsentTestTags.EXPIRED_STATE).assertIsDisplayed()
    }

    @Test
    fun aConnectionFailureGetsItsOwnPanel() = runComposeUiTest {
        setContent {
            PaymentConsentScreenContent(
                PaymentConsentFixtures.errorState(PaymentConsentErrorKind.NetworkError),
                {},
            )
        }
        onNodeWithTag(PaymentConsentTestTags.CONNECTION_FAILED_STATE).assertIsDisplayed()
        onNodeWithTag(PaymentConsentTestTags.ERROR_STATE).assertDoesNotExist()
    }

    /**
     * Load-bearing copy: someone who has just been bounced out of a payment needs to know their
     * money is where they left it before they will read anything else.
     */
    @Test
    fun everyOutcomeThatCanPromiseItSaysNoMoneyHasMoved() {
        val reassuring = PaymentConsentErrorKind.entries -
            setOf(
                PaymentConsentErrorKind.SubmissionFailed,
                PaymentConsentErrorKind.SubmissionUnconfirmed,
            )
        reassuring.forEach { kind ->
            runComposeUiTest {
                setContent {
                    PaymentConsentScreenContent(PaymentConsentFixtures.errorState(kind), {})
                }
                onNodeWithTag(PaymentConsentTestTags.NO_MONEY_MOVED).assertIsDisplayed()
            }
        }
    }

    /**
     * The second outcome that must not make the promise, and the less obvious one.
     *
     * Here the bank returned success and only the reply was unreadable, so the payment almost
     * certainly exists. It is the case most likely to be "tidied up" into the reassuring branch by
     * someone reading the kind's name and assuming an unreadable response means nothing happened.
     */
    @Test
    fun anUnconfirmedSubmissionDoesNotPromiseThatNothingMoved() = runComposeUiTest {
        setContent {
            PaymentConsentScreenContent(
                PaymentConsentFixtures.errorState(PaymentConsentErrorKind.SubmissionUnconfirmed),
                {},
            )
        }
        onNodeWithTag(PaymentConsentTestTags.OUTCOME_BODY).assertIsDisplayed()
        onNodeWithTag(PaymentConsentTestTags.NO_MONEY_MOVED).assertDoesNotExist()
    }

    /**
     * The one outcome that must not make the promise. Once the instruction is with the bank the app
     * cannot know whether it was taken, so this asks the customer to check rather than reassuring.
     */
    @Test
    fun aFailedSubmissionDoesNotPromiseThatNothingMoved() = runComposeUiTest {
        setContent {
            PaymentConsentScreenContent(
                PaymentConsentFixtures.errorState(PaymentConsentErrorKind.SubmissionFailed),
                {},
            )
        }
        onNodeWithTag(PaymentConsentTestTags.OUTCOME_BODY).assertIsDisplayed()
        onNodeWithTag(PaymentConsentTestTags.NO_MONEY_MOVED).assertDoesNotExist()
    }

    /** While the first poll is in flight there is nothing useful to offer yet. */
    @Test
    fun checkingOffersNothingUntilTheFirstPollComesBack() = runComposeUiTest {
        setContent { PaymentConsentScreenContent(PaymentConsentFixtures.checkingState(), {}) }
        onNodeWithTag(PaymentConsentTestTags.PROGRESS_INDICATOR).assertIsDisplayed()
        onNodeWithTag(PaymentConsentTestTags.CHECK_AGAIN_BUTTON).assertDoesNotExist()
    }

    /** Once the bank has said "not yet", explain the wait and offer another look. */
    @Test
    fun aConsentStillAwaitingAuthorisationExplainsTheWait() = runComposeUiTest {
        setContent {
            PaymentConsentScreenContent(PaymentConsentFixtures.checkingState(canCheckAgain = true), {})
        }
        onNodeWithTag(PaymentConsentTestTags.CHECKING_HINT).assertIsDisplayed()
        onNodeWithTag(PaymentConsentTestTags.CHECK_AGAIN_BUTTON).assertIsDisplayed()
    }

    @Test
    fun tappingCheckAgainRepolls() {
        val actions = captureActions(
            PaymentConsentFixtures.checkingState(canCheckAgain = true),
            PaymentConsentTestTags.CHECK_AGAIN_BUTTON,
        )

        assertEquals<List<PaymentConsentAction>>(listOf(PaymentConsentAction.CheckAgain), actions)
    }

    @Test
    fun confirmingFundsNamesTheStageItHasReached() = runComposeUiTest {
        setContent { PaymentConsentScreenContent(PaymentConsentFixtures.confirmingFundsState(), {}) }
        onNodeWithTag(PaymentConsentTestTags.PROGRESS_INDICATOR).assertIsDisplayed()
        onNodeWithTag(PaymentConsentTestTags.PROGRESS_DETAIL).assertIsDisplayed()
    }

    /**
     * The instruction is with the bank at this point, so the screen tells the customer to wait and
     * offers nothing to tap — there is no exit that would undo anything.
     */
    @Test
    fun submittingWarnsAgainstLeavingAndOffersNoExit() = runComposeUiTest {
        setContent { PaymentConsentScreenContent(PaymentConsentFixtures.submittingState(), {}) }
        onNodeWithTag(PaymentConsentTestTags.PROGRESS_INDICATOR).assertIsDisplayed()
        onNodeWithTag(PaymentConsentTestTags.SUBMITTING_WARNING).assertIsDisplayed()
        onNodeWithTag(PaymentConsentTestTags.ABANDON_BUTTON).assertDoesNotExist()
        onNodeWithTag(PaymentConsentTestTags.CHECK_AGAIN_BUTTON).assertDoesNotExist()
    }

    /** Both exits are present where restarting and abandoning are equally legitimate. */
    @Test
    fun errorOffersBothRestartAndAbandon() = runComposeUiTest {
        setContent { PaymentConsentScreenContent(PaymentConsentFixtures.errorState(), {}) }
        onNodeWithTag(PaymentConsentTestTags.ERROR_STATE).assertIsDisplayed()
        onNodeWithTag(PaymentConsentTestTags.RESTART_BUTTON).assertIsDisplayed()
        onNodeWithTag(PaymentConsentTestTags.ABANDON_BUTTON).assertIsDisplayed()
    }

    /**
     * The duplicate-payment guard, and the reason it matters most here.
     *
     * This outcome's own copy tells the customer their money may already be gone and to go and
     * check. Putting "Authorise again" underneath that as the primary control invites exactly the
     * second payment the copy is warning about — on a rail where a consent cannot be cancelled.
     */
    @Test
    fun anUnconfirmedSubmissionNeverOffersAOneTapRerun() = runComposeUiTest {
        setContent {
            PaymentConsentScreenContent(
                PaymentConsentFixtures.errorState(PaymentConsentErrorKind.SubmissionFailed),
                {},
            )
        }
        onNodeWithTag(PaymentConsentTestTags.RESTART_BUTTON).assertDoesNotExist()
        onNodeWithTag(PaymentConsentTestTags.ABANDON_BUTTON).assertIsDisplayed()
    }

    /** Nothing was in flight, so there is nothing to authorise again — only a way out. */
    @Test
    fun nothingPendingOffersOnlyAWayOut() = runComposeUiTest {
        setContent {
            PaymentConsentScreenContent(
                PaymentConsentFixtures.errorState(PaymentConsentErrorKind.NoPendingAuthorisation),
                {},
            )
        }
        onNodeWithTag(PaymentConsentTestTags.RESTART_BUTTON).assertDoesNotExist()
        onNodeWithTag(PaymentConsentTestTags.ABANDON_BUTTON).assertIsDisplayed()
    }

    /** The single exit still reports the payment as abandoned, so the host still routes away. */
    @Test
    fun theSingleExitStillAbandonsThePayment() {
        val actions = captureActions(
            PaymentConsentFixtures.errorState(PaymentConsentErrorKind.SubmissionFailed),
            PaymentConsentTestTags.ABANDON_BUTTON,
        )

        assertEquals<List<PaymentConsentAction>>(listOf(PaymentConsentAction.AbandonPayment), actions)
    }

    /**
     * Every other outcome keeps its retry. Gating is a targeted exception, not a new default — a
     * declined or expired authorisation is exactly the case where starting again is the right thing.
     */
    @Test
    fun everyOtherOutcomeStillOffersAuthoriseAgain() {
        val retryable = PaymentConsentErrorKind.entries -
            setOf(
                PaymentConsentErrorKind.SubmissionFailed,
                PaymentConsentErrorKind.NoPendingAuthorisation,
                PaymentConsentErrorKind.RequestRejected,
                PaymentConsentErrorKind.ResponseUnreadable,
                PaymentConsentErrorKind.SubmissionUnconfirmed,
            )
        retryable.forEach { kind ->
            runComposeUiTest {
                setContent {
                    PaymentConsentScreenContent(PaymentConsentFixtures.errorState(kind), {})
                }
                onNodeWithTag(PaymentConsentTestTags.RESTART_BUTTON).assertIsDisplayed()
            }
        }
    }

    @Test
    fun tappingAuthoriseAgainRestarts() {
        val actions = captureActions(
            PaymentConsentFixtures.errorState(),
            PaymentConsentTestTags.RESTART_BUTTON,
        )

        assertEquals<List<PaymentConsentAction>>(
            listOf(PaymentConsentAction.RetryAuthorisation),
            actions,
        )
    }

    @Test
    fun tappingCancelAbandonsThePayment() {
        val actions = captureActions(
            PaymentConsentFixtures.errorState(),
            PaymentConsentTestTags.ABANDON_BUTTON,
        )

        assertEquals<List<PaymentConsentAction>>(listOf(PaymentConsentAction.AbandonPayment), actions)
    }

    /**
     * A standing order is set up and a scheduled payment is scheduled; neither is sent. Every rail
     * shared the immediate payment's wording before the consent type reached this screen, and no
     * assertion here would have caught it.
     */
    @Test
    fun submittingSaysTheStandingOrderIsBeingSetUp() = runComposeUiTest {
        setContent {
            PaymentConsentScreenContent(
                PaymentConsentFixtures.submittingState(ConsentType.DomesticStandingOrder),
                {},
            )
        }
        onNodeWithTag(PaymentConsentTestTags.PROGRESS_DETAIL)
            .assertTextEquals("Setting up your standing order…")
    }

    @Test
    fun submittingSaysTheScheduledPaymentIsBeingScheduled() = runComposeUiTest {
        setContent {
            PaymentConsentScreenContent(
                PaymentConsentFixtures.submittingState(ConsentType.InternationalScheduledPayment),
                {},
            )
        }
        onNodeWithTag(PaymentConsentTestTags.PROGRESS_DETAIL)
            .assertTextEquals("Scheduling your payment…")
    }

    @Test
    fun submittingStillSaysSendingForAnImmediatePayment() = runComposeUiTest {
        setContent {
            PaymentConsentScreenContent(
                PaymentConsentFixtures.submittingState(ConsentType.DomesticSinglePayment),
                {},
            )
        }
        onNodeWithTag(PaymentConsentTestTags.PROGRESS_DETAIL)
            .assertTextEquals("Sending your payment…")
    }

    /** The fallback: a rail this screen cannot name must not claim an instruction was set up. */
    @Test
    fun anUnnamedRailKeepsTheImmediatePaymentWording() = runComposeUiTest {
        setContent { PaymentConsentScreenContent(PaymentConsentFixtures.submittingState(), {}) }
        onNodeWithTag(PaymentConsentTestTags.PROGRESS_DETAIL)
            .assertTextEquals("Sending your payment…")
    }

    @Test
    fun theApprovedPanelNamesTheInstruction() = runComposeUiTest {
        setContent {
            PaymentConsentScreenContent(
                PaymentConsentFixtures.approvedState(ConsentType.DomesticScheduledPayment),
                {},
            )
        }
        onNodeWithTag(PaymentConsentTestTags.OUTCOME_BODY)
            .assertTextEquals("Finishing your scheduled payment…")
    }

    @Test
    fun aDeclinedStandingOrderIsNotCalledAPayment() = runComposeUiTest {
        setContent {
            PaymentConsentScreenContent(
                PaymentConsentFixtures.errorState(
                    kind = PaymentConsentErrorKind.ConsentRejected,
                    consentType = ConsentType.DomesticStandingOrder,
                ),
                {},
            )
        }
        onNodeWithTag(PaymentConsentTestTags.OUTCOME_TITLE)
            .assertTextEquals("Standing order declined")
    }

    /**
     * The reassurance answers the question the rail actually raises. For a mandate that is whether
     * one was created, not whether money moved — money was never going to move today either way.
     */
    @Test
    fun theReassuranceNamesWhatWasNotCreated() = runComposeUiTest {
        setContent {
            PaymentConsentScreenContent(
                PaymentConsentFixtures.errorState(
                    kind = PaymentConsentErrorKind.ConsentRejected,
                    consentType = ConsentType.DomesticStandingOrder,
                ),
                {},
            )
        }
        onNodeWithTag(PaymentConsentTestTags.NO_MONEY_MOVED)
            .assertTextEquals("No standing order has been set up.")
    }
}
