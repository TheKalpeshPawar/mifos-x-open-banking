/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.consentcallback

/**
 * The tag vocabulary the step composables already emit, named in one place so Compose UI tests and
 * Maestro flows pin to a constant instead of a repeated string literal.
 *
 * Values mirror the tags in `steps/` verbatim — changing one here without changing the composable
 * silently breaks every test pinned to it. Append-only: renaming or removing a tag breaks tests
 * already relying on it.
 */
object ConsentCallbackTestTags {
    const val LOADING_STEP = "callback_loading"
    const val SUCCESS_STEP = "callback_content"
    const val AWAITING_STEP = "callback_awaiting"
    const val ERROR_STEP = "callback_error"
    const val ACCESS_DENIED_STEP = "callback_access_denied"
    const val SECURITY_ERROR_STEP = "callback_security_error"

    /** Awaiting → re-poll the consent (`PollConsentStatus`). */
    const val CHECK_AGAIN_BUTTON = "callback_check_again"

    /** Error → back to login (`NavigateRetry`). */
    const val ERROR_RETRY_BUTTON = "callback_error_retry"

    /** Access denied → back to login (`NavigateRetry`). */
    const val START_OVER_BUTTON = "callback_start_over"

    /** Security error → clear the session and re-authenticate (`NavigateLogin`). */
    const val START_AGAIN_BUTTON = "callback_start_again"
}
