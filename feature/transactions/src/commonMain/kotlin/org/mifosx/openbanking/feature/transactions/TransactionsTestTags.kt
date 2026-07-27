/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.transactions

import org.mifosx.openbanking.feature.transactions.ui.TransactionFilter

/**
 * Stable Compose `testTag` identifiers for the transactions screen. Shared by the Robolectric and
 * on-device instrumented UI tests so both drive the screen through the same vocabulary.
 */
internal object TransactionsTestTags {
    const val CONTENT = "transactions_content"
    const val LIST = "transactions_list"
    const val SKELETON = "transactions_skeleton"

    const val PERIOD_SUMMARY = "transactions_period_summary"
    const val MONEY_IN = "transactions_money_in"
    const val MONEY_OUT = "transactions_money_out"

    const val FILTER_ROW = "transactions_filter_row"
    const val FILTER_DATE_RANGE = "transactions_filter_date_range"

    const val SEARCH_FIELD = "transactions_search_field"
    const val SEARCH_CLEAR = "transactions_search_clear"

    const val LOAD_MORE = "transactions_load_more"
    const val PAGINATION_LOADER = "transactions_pagination_loader"

    const val EMPTY = "transactions_empty"
    const val CLEAR_FILTERS = "transactions_clear_filters"

    const val ERROR = "transactions_error"
    const val ERROR_STATUS = "transactions_error_status"
    const val ERROR_RETRY = "transactions_error_retry"

    private const val ROW_PREFIX = "transactions_row_"
    private const val FILTER_PREFIX = "transactions_filter_"
    private const val PENDING_PREFIX = "transactions_pending_"

    /** Per-row tag, e.g. `transactions_row_t1`. */
    fun row(key: String): String = ROW_PREFIX + key

    /** Per-row pending-badge tag, e.g. `transactions_pending_t1`. */
    fun pendingBadge(key: String): String = PENDING_PREFIX + key

    /** Per-chip tag for a money-direction filter, e.g. `transactions_filter_money_in`. */
    fun filterChip(filter: TransactionFilter): String = FILTER_PREFIX + filter.name.lowercase()
}
