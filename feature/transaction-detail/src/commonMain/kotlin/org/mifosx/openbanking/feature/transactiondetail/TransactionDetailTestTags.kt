/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.transactiondetail

/**
 * Stable `testTag` values for the transaction-detail screen, shared by the Compose, Robolectric and
 * instrumented suites so all drive the same nodes.
 *
 * Append-only: a tag an existing test references must not be renamed or removed without a matching
 * `uitest-tag-retire` note, or the suites silently stop asserting what they claim to.
 */
internal object TransactionDetailTestTags {

    const val LOADING_INDICATOR = "transactionDetail:loading"

    const val CONTENT_ROOT = "transactionDetail:content"
    const val AMOUNT = "transactionDetail:amount"
    const val CURRENCY = "transactionDetail:currency"
    const val MERCHANT = "transactionDetail:merchant"
    const val STATUS_CHIP = "transactionDetail:statusChip"
    const val DETAIL_CARD = "transactionDetail:detailCard"

    const val BOOKING_DATE_ROW = "transactionDetail:bookingDateRow"
    const val VALUE_DATE_ROW = "transactionDetail:valueDateRow"
    const val CATEGORY_ROW = "transactionDetail:categoryRow"
    const val MCC_ROW = "transactionDetail:mccRow"
    const val BALANCE_AFTER_ROW = "transactionDetail:balanceAfterRow"
    const val REFERENCE_ROW = "transactionDetail:referenceRow"
    const val REFERENCE_COPY_ICON = "transactionDetail:referenceCopyIcon"
    const val BANK_CODE_ROW = "transactionDetail:bankCodeRow"

    const val ERROR_STATE = "transactionDetail:errorState"
    const val ERROR_TITLE = "transactionDetail:errorTitle"
    const val ERROR_BODY = "transactionDetail:errorBody"
    const val RETRY_BUTTON = "transactionDetail:retryButton"
    const val GO_BACK_BUTTON = "transactionDetail:goBackButton"

    const val EMPTY_STATE = "transactionDetail:emptyState"
    const val EMPTY_TITLE = "transactionDetail:emptyTitle"
    const val EMPTY_BODY = "transactionDetail:emptyBody"
    const val EMPTY_BACK_BUTTON = "transactionDetail:emptyBackButton"
}
