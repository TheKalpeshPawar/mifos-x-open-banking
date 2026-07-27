/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.consentdetail

/**
 * Stable `testTag` values for the consent-detail screen, shared by the Compose, Robolectric and
 * instrumented suites so all drive the same nodes.
 *
 * Append-only: a tag an existing test references must not be renamed or removed without a matching
 * `uitest-tag-retire` note, or the suites silently stop asserting what they claim to.
 */
internal object ConsentDetailTestTags {

    const val LOADING = "consentDetail:loading"

    const val CONTENT_LIST = "consentDetail:contentList"

    const val STATUS_CARD = "consentDetail:statusCard"
    const val STATUS_CHIP = "consentDetail:statusChip"
    const val CONSENT_ID = "consentDetail:consentId"
    const val EXPIRY_BANNER = "consentDetail:expiryBanner"

    const val DATES_HEADER = "consentDetail:datesHeader"
    const val DATES_LIST = "consentDetail:datesList"
    const val PERMISSIONS_HEADER = "consentDetail:permissionsHeader"
    const val PERMISSIONS_LIST = "consentDetail:permissionsList"

    const val RECONFIRM_BUTTON = "consentDetail:reconfirmButton"
    const val REVOKE_BUTTON = "consentDetail:revokeButton"

    const val REVOKE_DIALOG = "consentDetail:revokeDialog"
    const val REVOKE_DIALOG_TITLE = "consentDetail:revokeDialogTitle"
    const val REVOKE_DIALOG_BODY = "consentDetail:revokeDialogBody"
    const val REVOKE_DIALOG_CANCEL = "consentDetail:revokeDialogCancel"
    const val REVOKE_DIALOG_CONFIRM = "consentDetail:revokeDialogConfirm"

    const val REVOKING_PROGRESS = "consentDetail:revokingProgress"
    const val REVOKING_LABEL = "consentDetail:revokingLabel"

    const val EMPTY_STATE = "consentDetail:emptyState"
    const val EMPTY_TITLE = "consentDetail:emptyTitle"
    const val EMPTY_BODY = "consentDetail:emptyBody"
    const val GO_BACK_BUTTON = "consentDetail:goBackButton"

    const val ERROR_STATE = "consentDetail:errorState"
    const val ERROR_TITLE = "consentDetail:errorTitle"
    const val ERROR_BODY = "consentDetail:errorBody"
    const val RETRY_BUTTON = "consentDetail:retryButton"

    /** Tag for one permission row, keyed by its OBIE permission code. */
    fun permissionRow(code: String): String = "consentDetail:permission:$code"

    /** Tag for one date row, keyed by its slot. */
    fun dateRow(slot: String): String = "consentDetail:date:$slot"
}
