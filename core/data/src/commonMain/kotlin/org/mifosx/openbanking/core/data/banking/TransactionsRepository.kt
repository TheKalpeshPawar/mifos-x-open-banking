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
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

/**
 * Streams the transactions for whichever account [accountIdFlow] currently selects, re-fetching when
 * the selection changes. Backed by a Room-persisted Store so the recent list and the month-to-date
 * spending aggregate survive process death and render offline.
 *
 * Also exposes a cursor-based pager ([firstPage] / [nextPage]) for the full Transactions screen, which
 * follows the OBIE `Links.Next` URL to page transparently and accumulates rows in memory.
 */
interface TransactionsRepository {

    /** Streams the selected account's transactions as [ScreenState], keyed by the emitted account id. */
    fun transactionsState(
        accountIdFlow: Flow<String>,
        scope: CoroutineScope,
    ): Flow<ScreenState<List<TransactionItem>>>

    /** Triggers a network refresh for the current account. */
    fun refresh()

    /** Fetches the first transactions page for [accountId], including the next-page cursor. */
    suspend fun firstPage(accountId: String): NetworkResult<TransactionsPage, NetworkError>

    /** Follows an OBIE `Links.Next` cursor URL to fetch the next transactions page. */
    suspend fun nextPage(nextLink: String): NetworkResult<TransactionsPage, NetworkError>
}
