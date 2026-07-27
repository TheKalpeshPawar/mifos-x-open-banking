/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.consentlist

/**
 * Stable `testTag` values for the consent-list screen, shared by the Compose, Robolectric and
 * instrumented suites so all drive the same nodes.
 *
 * Append-only: a tag an existing test references must not be renamed or removed without a matching
 * `uitest-tag-retire` note, or the suites silently stop asserting what they claim to.
 */
internal object ConsentListTestTags {

    const val LOADING = "consentList:loading"

    const val CONTENT_LIST = "consentList:contentList"

    const val RECONFIRM_BANNER = "consentList:reconfirmBanner"
    const val ACTIVE_SECTION_LABEL = "consentList:activeSectionLabel"
    // uitest-tag-retire: consent history dropped — the list shows only the current consent, so there
    // is no History section label or between-section divider to tag.

    const val EMPTY_STATE = "consentList:emptyState"
    const val EMPTY_TITLE = "consentList:emptyTitle"
    const val EMPTY_BODY = "consentList:emptyBody"
    const val CONNECT_BUTTON = "consentList:connectButton"

    const val ERROR_STATE = "consentList:errorState"
    const val ERROR_TITLE = "consentList:errorTitle"
    const val ERROR_BODY = "consentList:errorBody"
    const val RETRY_BUTTON = "consentList:retryButton"

    const val AUTH_ERROR_STATE = "consentList:authErrorState"
    const val AUTH_ERROR_TITLE = "consentList:authErrorTitle"
    const val AUTH_ERROR_BODY = "consentList:authErrorBody"
    const val REAUTH_BUTTON = "consentList:reauthButton"

    /** Tag for one consent card, keyed by its OBIE `ConsentId`. */
    fun card(consentId: String): String = "consentList:card:$consentId"

    /** Tag for one card's status chip, keyed by its `ConsentId`. */
    fun statusChip(consentId: String): String = "consentList:statusChip:$consentId"

    /** Tag for one card's reconfirm-urgency chip, keyed by its `ConsentId`. */
    fun urgencyChip(consentId: String): String = "consentList:urgencyChip:$consentId"
}
