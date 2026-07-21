/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.profile

import org.mifosx.openbanking.core.model.hsbcPermission.PermissionId

/**
 * Stable `testTag` values for the profile screen, shared by the headless, Robolectric and
 * instrumented suites so all three drive the same nodes.
 *
 * Append-only: a tag that an existing test references must not be renamed or removed without a
 * matching `uitest-tag-retire` note, or the suites silently stop asserting what they claim to.
 */
internal object ProfileTestTags {

    const val LOADING = "profile:loading"
    const val LOADING_CAPTION = "profile:loadingCaption"

    /** The scrolling profile body — the node the suites scroll to reach later sections. */
    const val CONTENT = "profile:content"

    const val IDENTITY_CARD = "profile:identityCard"
    const val AVATAR = "profile:avatar"
    const val DISPLAY_NAME = "profile:displayName"
    const val ROLE_LABEL = "profile:roleLabel"

    const val IDENTITY_SECTION = "profile:identitySection"
    const val EMAIL_ROW = "profile:emailRow"
    const val MOBILE_ROW = "profile:mobileRow"
    const val ADDRESS_ROW = "profile:addressRow"

    const val EXPIRY_BANNER = "profile:expiryBanner"
    const val EXPIRY_BANNER_BODY = "profile:expiryBannerBody"
    const val RENEW_CONSENT_BUTTON = "profile:renewConsentButton"

    const val CONNECTION_HEADER = "profile:connectionHeader"
    const val CONNECTION_CARD = "profile:connectionCard"
    const val CONNECTION_BANK_ROW = "profile:connectionBankRow"
    const val STATUS_ROW = "profile:statusRow"
    const val STATUS_VALUE = "profile:statusValue"
    const val EXPIRY_ROW = "profile:expiryRow"
    const val EXPIRY_VALUE = "profile:expiryValue"

    const val PERMISSIONS_HEADER = "profile:permissionsHeader"
    const val PERMISSIONS_TOGGLE = "profile:permissionsToggle"
    const val PERMISSIONS_LIST = "profile:permissionsList"
    const val MANAGE_CONSENT_BUTTON = "profile:manageConsentButton"

    const val SIGN_OUT_BUTTON = "profile:signOutButton"

    const val SIGN_OUT_DIALOG = "profile:signOutDialog"
    const val SIGN_OUT_CONFIRM_BUTTON = "profile:signOutConfirmButton"
    const val SIGN_OUT_CANCEL_BUTTON = "profile:signOutCancelButton"

    const val EMPTY_STATE = "profile:emptyState"
    const val EMPTY_TITLE = "profile:emptyTitle"
    const val EMPTY_BODY = "profile:emptyBody"
    const val REAUTHORISE_BUTTON = "profile:reauthoriseButton"

    const val ERROR_STATE = "profile:errorState"
    const val ERROR_TITLE = "profile:errorTitle"
    const val ERROR_BODY = "profile:errorBody"
    const val RETRY_BUTTON = "profile:retryButton"

    /** Tag for one permission row, keyed by its OBIE scope. */
    fun permissionRow(id: PermissionId): String = "profile:permission:${id.name}"
}
