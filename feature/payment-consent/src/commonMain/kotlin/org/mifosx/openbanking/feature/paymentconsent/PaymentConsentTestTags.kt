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

/**
 * Stable tags the UI suites drive this screen by. Append-only.
 *
 * Every state carries a tag of its own as well as the shared [PROGRESS_INDICATOR] /
 * [PROGRESS_DETAIL] pair. The shared pair alone could not tell the three return-leg stages apart —
 * they render the same spinner over three different strings — so a screenshot suite asserting on it
 * would pass whichever of the three happened to be on screen.
 */
internal object PaymentConsentTestTags {
    const val PROGRESS_INDICATOR = "paymentConsent:progressIndicator"
    const val PROGRESS_DETAIL = "paymentConsent:progressDetail"
    const val CHECKING_HINT = "paymentConsent:checkingHint"
    const val CHECK_AGAIN_BUTTON = "paymentConsent:checkAgainButton"
    const val AUTHORISED_STATE = "paymentConsent:authorisedState"
    const val ERROR_STATE = "paymentConsent:errorState"
    const val RESTART_BUTTON = "paymentConsent:restartButton"
    const val ABANDON_BUTTON = "paymentConsent:abandonButton"
    const val SUBMITTING_WARNING = "paymentConsent:submittingWarning"

    // The three stages of the return leg, which share one visual.
    const val VALIDATING_STATE = "paymentConsent:validatingState"
    const val EXCHANGING_STATE = "paymentConsent:exchangingState"
    const val CHECKING_STATE = "paymentConsent:checkingState"

    const val CONFIRMING_FUNDS_STATE = "paymentConsent:confirmingFundsState"
    const val SUBMITTING_STATE = "paymentConsent:submittingState"

    // The three failures with a designed visual of their own.
    const val DECLINED_STATE = "paymentConsent:declinedState"
    const val EXPIRED_STATE = "paymentConsent:expiredState"
    const val CONNECTION_FAILED_STATE = "paymentConsent:connectionFailedState"

    /** The bank had already created the payment — a success, and never an error panel. */
    const val ALREADY_SUBMITTED_STATE = "paymentConsent:alreadySubmittedState"

    const val OUTCOME_TITLE = "paymentConsent:outcomeTitle"
    const val OUTCOME_BODY = "paymentConsent:outcomeBody"

    /** The "No money has been moved" reassurance. Load-bearing copy, so it is asserted directly. */
    const val NO_MONEY_MOVED = "paymentConsent:noMoneyMoved"
}
