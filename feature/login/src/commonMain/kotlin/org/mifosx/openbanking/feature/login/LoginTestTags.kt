/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.login

/**
 * Stable Compose `testTag` identifiers for the login screen states. Shared by the Robolectric and
 * on-device instrumented UI tests so both drive the screen through the same vocabulary.
 */
internal object LoginTestTags {
    const val LOADING_PROGRESS = "login_loading_progress"
    const val EXPLAINER_CARD = "hsbc_explainer_card"
    const val CONTINUE_HSBC = "login_continue_hsbc"
    const val CANCEL = "login_cancel"
    const val AUTHORISING = "login_authorising"
    const val AUTHORISING_SPINNER = "authorising_spinner"
    const val ERROR = "login_error"
    const val ERROR_RETRY = "login_error_retry"
    const val EMPTY = "login_empty"
    const val EMPTY_GO_BACK = "login_empty_go_back"
}
