/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.scheduledpayments

/**
 * Stable `testTag` values for the scheduled-payments screen, shared by the Compose, Robolectric and
 * instrumented suites so all drive the same nodes.
 */
internal object ScheduledPaymentsTestTags {

    const val CONTENT_LIST = "scheduledPayments:contentList"

    /** Tag for one payment card, keyed by its OBIE `ScheduledPaymentId`. */
    fun card(scheduledPaymentId: String): String = "scheduledPayments:card:$scheduledPaymentId"
}
