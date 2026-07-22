/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.statementdetail

/**
 * Stable `testTag` values for the statement-detail screen, shared by the Compose, Robolectric and
 * instrumented suites so all drive the same nodes.
 *
 * Append-only: a tag an existing test references must not be renamed or removed without a matching
 * `uitest-tag-retire` note, or the suites silently stop asserting what they claim to.
 */
internal object StatementDetailTestTags {

    const val LOADING_SKELETON = "statementDetail:loadingSkeleton"

    /** The scrolling content list root. */
    const val CONTENT_LIST = "statementDetail:contentList"

    /** The scrolling empty-state list root (header + balances + empty-transactions block). */
    const val EMPTY_LIST = "statementDetail:emptyList"

    const val HEADER_CARD = "statementDetail:headerCard"
    const val BALANCES_SECTION = "statementDetail:balancesSection"
    const val FEES_SECTION = "statementDetail:feesSection"
    const val INTEREST_SECTION = "statementDetail:interestSection"
    const val TRANSACTIONS_SECTION = "statementDetail:transactionsSection"

    const val EMPTY_TRANSACTIONS = "statementDetail:emptyTransactions"
    const val EMPTY_TRANSACTIONS_TITLE = "statementDetail:emptyTransactionsTitle"
    const val EMPTY_TRANSACTIONS_BODY = "statementDetail:emptyTransactionsBody"

    const val DOWNLOAD_BUTTON = "statementDetail:downloadButton"
    const val DOWNLOAD_PROGRESS = "statementDetail:downloadProgress"

    const val ERROR_STATE = "statementDetail:errorState"
    const val ERROR_TITLE = "statementDetail:errorTitle"
    const val ERROR_BODY = "statementDetail:errorBody"
    const val RETRY_BUTTON = "statementDetail:retryButton"

    /** Tag for one transaction row, keyed by its OBIE `TransactionId`. */
    fun txnRow(id: String): String = "statementDetail_txn_$id"
}
