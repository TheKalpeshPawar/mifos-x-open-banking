/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.directdebits

/**
 * Stable `testTag` values for the direct-debits screen, shared by the Robolectric and instrumented
 * suites so both drive the same nodes.
 *
 * Append-only: a tag that an existing test references must not be renamed or removed without a
 * matching `uitest-tag-retire` note, or the suites silently stop asserting what they claim to.
 */
internal object DirectDebitsTestTags {

    const val LOADING_SKELETON = "directDebits:loadingSkeleton"
    const val SKELETON_CHIP_ROW = "directDebits:skeletonChipRow"

    /** The scrolling mandate list itself — the node the suites scroll to reach later cards. */
    const val CONTENT = "directDebits:content"
    const val SUMMARY_CHIPS = "directDebits:summaryChips"
    const val ACTIVE_CHIP = "directDebits:activeChip"
    const val INACTIVE_CHIP = "directDebits:inactiveChip"

    const val EMPTY_STATE = "directDebits:emptyState"
    const val EMPTY_TITLE = "directDebits:emptyTitle"
    const val EMPTY_BODY = "directDebits:emptyBody"

    const val UNSUPPORTED_STATE = "directDebits:unsupportedState"
    const val UNSUPPORTED_TITLE = "directDebits:unsupportedTitle"
    const val UNSUPPORTED_BODY = "directDebits:unsupportedBody"

    const val ERROR_STATE = "directDebits:errorState"
    const val ERROR_TITLE = "directDebits:errorTitle"
    const val ERROR_BODY = "directDebits:errorBody"
    const val RETRY_BUTTON = "directDebits:retryButton"

    /** Tag for one mandate card, keyed by its OBIE `MandateIdentification`. */
    fun card(mandateId: String): String = "directDebits:card:$mandateId"

    /** Tag for one mandate's status badge, keyed by its `MandateIdentification`. */
    fun statusBadge(mandateId: String): String = "directDebits:statusBadge:$mandateId"

    /** Tag for one mandate's previous-payment amount, keyed by its `MandateIdentification`. */
    fun amount(mandateId: String): String = "directDebits:amount:$mandateId"

    /** Tag for one mandate's last-collected line, keyed by its `MandateIdentification`. */
    fun lastCollected(mandateId: String): String = "directDebits:lastCollected:$mandateId"

    /** Tag for one mandate's reference line, keyed by its `MandateIdentification`. */
    fun mandateReference(mandateId: String): String = "directDebits:mandateReference:$mandateId"
}
