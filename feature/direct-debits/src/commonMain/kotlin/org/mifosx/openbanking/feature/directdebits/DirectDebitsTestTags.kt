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
 */
internal object DirectDebitsTestTags {

    /** The scrolling mandate list itself — the node the suites scroll to reach later cards. */
    const val CONTENT = "directDebits:content"

    /** Tag for one mandate card, keyed by its OBIE `MandateIdentification`. */
    fun card(mandateId: String): String = "directDebits:card:$mandateId"

    /** Tag for one mandate's status badge, keyed by its `MandateIdentification`. */
    fun statusBadge(mandateId: String): String = "directDebits:statusBadge:$mandateId"

    /** Tag for one mandate's previous-payment amount, keyed by its `MandateIdentification`. */
    fun amount(mandateId: String): String = "directDebits:amount:$mandateId"

    /** Tag for one mandate's collection-time line, keyed by its `MandateIdentification`. */
    fun collectionTime(mandateId: String): String = "directDebits:collectionTime:$mandateId"
}
