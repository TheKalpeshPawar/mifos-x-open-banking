/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import org.mifosx.openbanking.core.model.banking.TransactionItem
import org.mifosx.openbanking.core.model.banking.TransactionsPage
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import template.core.base.store.screen.ScreenDataStream

/**
 * Streams the transactions for whichever account [accountIdFlow] currently selects, re-fetching when
 * the selection changes. Backed by a Room-persisted Store so the recent list and the month-to-date
 * spending aggregate survive process death and render offline.
 *
 * Also exposes a cursor-based pager ([firstPage] / [nextPage]) for the full Transactions screen, which
 * follows the OBIE `Links.Next` URL to page transparently and accumulates rows in memory.
 */
interface TransactionsRepository {

    /**
     * Opens the selected account's transaction stream, re-keyed as [accountIdFlow] emits.
     *
     * The id is a flow because home switches account from its chip row without navigating. Each
     * call builds a stream bound to [scope], which the caller owns for its screen's lifetime.
     */
    fun transactionsStream(
        accountIdFlow: Flow<String>,
        scope: CoroutineScope,
    ): ScreenDataStream<List<TransactionItem>>

    /** Fetches the first transactions page for [accountId], including the next-page cursor. */
    suspend fun firstPage(accountId: String): NetworkResult<TransactionsPage, NetworkError>

    /** Follows an OBIE `Links.Next` cursor URL to fetch the next transactions page. */
    suspend fun nextPage(nextLink: String): NetworkResult<TransactionsPage, NetworkError>
}
