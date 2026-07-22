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
 *
 * Append-only: a tag an existing test references must not be renamed or removed without a matching
 * `uitest-tag-retire` note, or the suites silently stop asserting what they claim to.
 */
internal object ScheduledPaymentsTestTags {

    const val LOADING = "scheduledPayments:loading"

    const val CONTENT_LIST = "scheduledPayments:contentList"

    const val EMPTY_STATE = "scheduledPayments:emptyState"
    const val EMPTY_TITLE = "scheduledPayments:emptyTitle"
    const val EMPTY_BODY = "scheduledPayments:emptyBody"

    const val ERROR_STATE = "scheduledPayments:errorState"
    const val ERROR_TITLE = "scheduledPayments:errorTitle"
    const val ERROR_BODY = "scheduledPayments:errorBody"
    const val RETRY_BUTTON = "scheduledPayments:retryButton"

    /** Tag for one payment card, keyed by its OBIE `ScheduledPaymentId`. */
    fun card(scheduledPaymentId: String): String = "scheduledPayments:card:$scheduledPaymentId"

    /** Tag for one payment's ScheduledType chip, keyed by its `ScheduledPaymentId`. */
    fun typeChip(scheduledPaymentId: String): String = "scheduledPayments:typeChip:$scheduledPaymentId"
}
