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

/** Stable tags the UI suites drive this screen by. Append-only. */
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
}
