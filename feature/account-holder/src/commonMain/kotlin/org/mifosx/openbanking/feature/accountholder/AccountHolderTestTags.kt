/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accountholder

/**
 * Stable `testTag` values for the account-holder screen, shared by the headless, Robolectric and
 * instrumented suites so all three drive the same nodes.
 *
 * Append-only: a tag that an existing test references must not be renamed or removed without a
 * matching `uitest-tag-retire` note, or the suites silently stop asserting what they claim to.
 */
internal object AccountHolderTestTags {

    /** The scrolling account-holder body — the node the suites scroll to reach later sections. */
    const val CONTENT = "accountHolder:content"

    const val IDENTITY_CARD = "accountHolder:identityCard"
    const val AVATAR = "accountHolder:avatar"
    const val DISPLAY_NAME = "accountHolder:displayName"
    const val ROLE_LABEL = "accountHolder:roleLabel"

    const val IDENTITY_SECTION = "accountHolder:identitySection"
    const val EMAIL_ROW = "accountHolder:emailRow"
    const val MOBILE_ROW = "accountHolder:mobileRow"
    const val ADDRESS_ROW = "accountHolder:addressRow"
}
