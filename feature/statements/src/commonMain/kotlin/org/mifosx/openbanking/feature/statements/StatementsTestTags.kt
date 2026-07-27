/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.statements

/**
 * Stable `testTag` values for the statements screen, shared by the Robolectric and instrumented
 * suites so both drive the same nodes.
 *
 * Append-only: a tag that an existing test references must not be renamed or removed without a
 * matching `uitest-tag-retire` note, or the suites silently stop asserting what they claim to.
 */
internal object StatementsTestTags {

    const val LOADING_SKELETON = "statements:loadingSkeleton"

    /** The scrolling statement list itself — the node the suites scroll to reach later rows. */
    const val CONTENT_LIST = "statements:contentList"

    const val EMPTY_STATE = "statements:emptyState"
    const val EMPTY_TITLE = "statements:emptyTitle"
    const val EMPTY_BODY = "statements:emptyBody"

    const val ERROR_STATE = "statements:errorState"
    const val ERROR_TITLE = "statements:errorTitle"
    const val ERROR_BODY = "statements:errorBody"
    const val RETRY_BUTTON = "statements:retryButton"

    /** Tag for one statement row, keyed by its OBIE `StatementId`. */
    fun row(id: String): String = "statements_row_$id"

    /** Tag for one row's download button, keyed by its `StatementId`. */
    fun downloadButton(id: String): String = "statements_download_$id"

    /** Tag for one row's in-flight download spinner, keyed by its `StatementId`. */
    fun downloadSpinner(id: String): String = "statements_download_spinner_$id"
}
