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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mifosx.openbanking.feature.paymentconsent.ui.PaymentConsentErrorKind
import org.mifosx.openbanking.feature.paymentconsent.ui.PaymentConsentState
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import template.core.base.designsystem.KptTheme

private const val ROBOLECTRIC_SDK = 34

/**
 * A phone-sized device rather than a Surface of that size: Roborazzi captures the composition root,
 * which Robolectric sizes from the device qualifiers, so constraining the Surface would change
 * nothing about what lands in the image.
 */
private const val PHONE = "w412dp-h892dp"

/**
 * Golden-image coverage for [PaymentConsentScreenContent], captured with Roborazzi under
 * Robolectric's native graphics (no device).
 *
 * Goldens are **not tracked** — `recordRoborazziDemoDebug` writes them under
 * `build/outputs/roborazzi/` for local review. So this suite fails on a composition crash and lets a
 * change be eyeballed, but `verifyRoborazziDemoDebug` can only detect drift against a baseline
 * recorded on the same machine.
 *
 * Every designed state gets its own golden, including all three stages of the return leg. They are
 * the same picture over three different strings, and the whole reason they now carry distinct tags
 * is that nothing else could prove which one was captured.
 *
 * These are also the only check on the outcome copy: the reassurance line is a rendering, and a
 * golden is what makes "does every failure still say no money has moved?" answerable by looking.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(manifest = Config.NONE, sdk = [ROBOLECTRIC_SDK], qualifiers = PHONE)
class PaymentConsentScreenScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    // The return leg — one visual, three stages.

    @Test
    fun validatingGolden() = capture("validating", PaymentConsentFixtures.validatingState())

    @Test
    fun exchangingGolden() = capture("exchanging", PaymentConsentFixtures.exchangingState())

    @Test
    fun checkingGolden() = capture("checking", PaymentConsentFixtures.checkingState())

    /** The bank has said "not yet": the hint and the way out both appear. */
    @Test
    fun checkingWaitingGolden() =
        capture("checking_waiting", PaymentConsentFixtures.checkingState(canCheckAgain = true))

    @Test
    fun approvedGolden() = capture("approved", PaymentConsentFixtures.approvedState())

    /**
     * The bank had already created the payment. Worth a golden of its own precisely because what
     * makes it correct is two things that are *absent* — the reassurance line and the retry button —
     * and absences are what a reviewer is least likely to notice without a picture.
     */
    @Test
    fun alreadySubmittedGolden() =
        capture("already_submitted", PaymentConsentFixtures.alreadySubmittedState())

    @Test
    fun confirmingFundsGolden() =
        capture("confirming_funds", PaymentConsentFixtures.confirmingFundsState())

    @Test
    fun submittingGolden() = capture("submitting", PaymentConsentFixtures.submittingState())

    // The outcomes.

    @Test
    fun declinedGolden() = captureOutcome("declined", PaymentConsentErrorKind.ConsentRejected)

    @Test
    fun expiredGolden() = captureOutcome("expired", PaymentConsentErrorKind.CodeExpired)

    @Test
    fun timedOutGolden() = captureOutcome("timed_out", PaymentConsentErrorKind.AuthorisationTimedOut)

    @Test
    fun connectionFailedGolden() = captureOutcome("connection_failed", PaymentConsentErrorKind.NetworkError)

    /** The benign one: a callback for a payment that already finished. */
    @Test
    fun nothingPendingGolden() =
        captureOutcome("nothing_pending", PaymentConsentErrorKind.NoPendingAuthorisation)

    @Test
    fun stateMismatchGolden() = captureOutcome("state_mismatch", PaymentConsentErrorKind.StateMismatch)

    @Test
    fun insufficientFundsGolden() =
        captureOutcome("insufficient_funds", PaymentConsentErrorKind.InsufficientFunds)

    /** A refusal the bank explained, which used to wear the connection-failure panel. */
    @Test
    fun requestRejectedGolden() =
        captureOutcome("request_rejected", PaymentConsentErrorKind.RequestRejected)

    /** A reply the app could not read, before anything was submitted. Still reassures. */
    @Test
    fun responseUnreadableGolden() =
        captureOutcome("response_unreadable", PaymentConsentErrorKind.ResponseUnreadable)

    /**
     * The one new panel that must NOT reassure — the bank accepted the payment and only the reply
     * was lost. A golden is the only place that absence is visible.
     */
    @Test
    fun submissionUnconfirmedGolden() =
        captureOutcome("submission_unconfirmed", PaymentConsentErrorKind.SubmissionUnconfirmed)

    /** The one panel with no reassurance line — worth being able to see that it is still absent. */
    @Test
    fun submissionFailedGolden() =
        captureOutcome("submission_failed", PaymentConsentErrorKind.SubmissionFailed)

    private fun captureOutcome(name: String, kind: PaymentConsentErrorKind) =
        capture(name, PaymentConsentFixtures.errorState(kind))

    private fun capture(state: String, screenState: PaymentConsentState) {
        composeRule.setContent {
            KptTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    PaymentConsentScreenContent(state = screenState, onAction = {})
                }
            }
        }
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/payment_consent_$state.png")
    }
}
