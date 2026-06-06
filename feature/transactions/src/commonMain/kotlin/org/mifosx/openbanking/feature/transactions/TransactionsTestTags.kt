/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.transactions

object TransactionsTestTags {
    const val SEARCH_BAR = "transactions_search_bar"
    const val DATE_RANGE_CHIP = "transactions_date_range_chip"
    const val FILTER_CHIPS_ROW = "transactions_filter_chips_row"
    const val SUMMARY_CARD = "transactions_summary_card"
    const val PENDING_HEADER = "transactions_pending_header"
    const val LOAD_MORE = "transactions_load_more"
    fun filterChip(name: String) = "transactions_filter_$name"
    fun row(id: String) = "transaction_row_$id"
    fun pendingRow(id: String) = "pending_row_$id"
}
