/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.paymentstatus

/** Stable tags the UI suites drive this screen by. Append-only. */
internal object PaymentStatusTestTags {
    const val SKELETON = "paymentStatus:skeleton"
    const val SUMMARY_CARD = "paymentStatus:summaryCard"
    const val AMOUNT = "paymentStatus:amount"
    const val STATUS_CHIP = "paymentStatus:statusChip"
    const val IN_PROGRESS_NOTE = "paymentStatus:inProgressNote"
    const val DETAILS_LIST = "paymentStatus:detailsList"
    const val DETAIL_REFERENCE = "paymentStatus:detailReference"
    const val DETAIL_FROM = "paymentStatus:detailFrom"
    const val DETAIL_SUBMITTED = "paymentStatus:detailSubmitted"
    const val DETAIL_PAYMENT_ID = "paymentStatus:detailPaymentId"
    const val STATUS_DETAIL = "paymentStatus:statusDetail"
    const val DETAIL_SETTLED = "paymentStatus:detailSettled"
    const val DETAIL_STATUS_CHANGED = "paymentStatus:detailStatusChanged"
    const val DETAIL_FEE = "paymentStatus:detailFee"
    const val LAST_CHECKED = "paymentStatus:lastChecked"
    const val REFRESH_BUTTON = "paymentStatus:refreshButton"
    const val NEW_PAYMENT_BUTTON = "paymentStatus:newPaymentButton"
    const val ERROR_STATE = "paymentStatus:errorState"
    const val RETRY_BUTTON = "paymentStatus:retryButton"
}
