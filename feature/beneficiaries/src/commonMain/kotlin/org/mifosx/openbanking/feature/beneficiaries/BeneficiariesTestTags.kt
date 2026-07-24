/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.beneficiaries

/**
 * Stable `testTag` values for the beneficiaries screen, shared by the Compose, Robolectric and
 * instrumented suites so all drive the same nodes.
 *
 * Append-only: a tag an existing test references must not be renamed or removed without a matching
 * `uitest-tag-retire` note, or the suites silently stop asserting what they claim to.
 */
internal object BeneficiariesTestTags {

    const val LOADING = "beneficiaries:loading"

    const val SEARCH_FIELD = "beneficiaries:searchField"

    const val CONTENT_LIST = "beneficiaries:contentList"

    const val SEARCH_EMPTY_STATE = "beneficiaries:searchEmptyState"
    const val SEARCH_EMPTY_TITLE = "beneficiaries:searchEmptyTitle"
    const val SEARCH_EMPTY_BODY = "beneficiaries:searchEmptyBody"

    const val EMPTY_STATE = "beneficiaries:emptyState"
    const val EMPTY_TITLE = "beneficiaries:emptyTitle"
    const val EMPTY_BODY = "beneficiaries:emptyBody"

    const val ERROR_STATE = "beneficiaries:errorState"
    const val ERROR_TITLE = "beneficiaries:errorTitle"
    const val ERROR_BODY = "beneficiaries:errorBody"
    const val RETRY_BUTTON = "beneficiaries:retryButton"
    const val VIEW_CONSENTS_BUTTON = "beneficiaries:viewConsentsButton"

    /** Tag for one payee row, keyed by its OBIE `BeneficiaryId`. */
    fun row(beneficiaryId: String): String = "beneficiaries:row:$beneficiaryId"

    /** Tag for one payee row's initials avatar, keyed by its `BeneficiaryId`. */
    fun avatar(beneficiaryId: String): String = "beneficiaries:avatar:$beneficiaryId"
}
