/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.standingorders

/**
 * Stable `testTag` values for the standing-orders screen, shared by the Robolectric and instrumented
 * suites so both drive the same nodes.
 *
 * Append-only: a tag that an existing test references must not be renamed or removed without a
 * matching `uitest-tag-retire` note, or the suites silently stop asserting what they claim to.
 */
internal object StandingOrdersTestTags {

    const val LOADING_SKELETON = "standingOrders:loadingSkeleton"
    const val SKELETON_SUMMARY = "standingOrders:skeletonSummary"

    /** The scrolling order list itself — the node the suites scroll to reach later cards. */
    const val CONTENT = "standingOrders:content"
    const val SUMMARY_ROW = "standingOrders:summaryRow"

    const val EMPTY_STATE = "standingOrders:emptyState"
    const val EMPTY_TITLE = "standingOrders:emptyTitle"
    const val EMPTY_BODY = "standingOrders:emptyBody"

    const val ERROR_STATE = "standingOrders:errorState"
    const val ERROR_TITLE = "standingOrders:errorTitle"
    const val ERROR_BODY = "standingOrders:errorBody"
    const val RETRY_BUTTON = "standingOrders:retryButton"

    /** Tag for one order card, keyed by its OBIE `StandingOrderId`. */
    fun card(orderId: String): String = "standingOrders:card:$orderId"

    /** Tag for one order's status badge, keyed by its `StandingOrderId`. */
    fun statusBadge(orderId: String): String = "standingOrders:statusBadge:$orderId"

    /** Tag for one order's next-payment amount, keyed by its `StandingOrderId`. */
    fun amount(orderId: String): String = "standingOrders:amount:$orderId"

    /** Tag for one order's decoded frequency line, keyed by its `StandingOrderId`. */
    fun frequency(orderId: String): String = "standingOrders:frequency:$orderId"

    /** Tag for one order's next-payment-date line, keyed by its `StandingOrderId`. */
    fun nextDate(orderId: String): String = "standingOrders:nextDate:$orderId"

    /** Tag for one order's final-payment-date line, present only when the order ends. */
    fun finalDate(orderId: String): String = "standingOrders:finalDate:$orderId"

    /** Tag for one order's sort-code / account-number line, keyed by its `StandingOrderId`. */
    fun sortCode(orderId: String): String = "standingOrders:sortCode:$orderId"

    /** Tag for one order's payment-reference line, keyed by its `StandingOrderId`. */
    fun reference(orderId: String): String = "standingOrders:reference:$orderId"
}
