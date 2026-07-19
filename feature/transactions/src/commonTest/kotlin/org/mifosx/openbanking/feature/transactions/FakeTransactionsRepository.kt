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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.mifosx.openbanking.core.data.banking.TransactionsRepository
import org.mifosx.openbanking.core.model.banking.TransactionItem
import org.mifosx.openbanking.core.model.banking.TransactionsPage
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

/**
 * In-memory [TransactionsRepository] for ViewModel tests. Only the cursor pager ([firstPage] /
 * [nextPage]) is exercised; the store-backed stream is unused here.
 */
class FakeTransactionsRepository : TransactionsRepository {

    var firstPageResult: NetworkResult<TransactionsPage, NetworkError> =
        NetworkResult.Success(TransactionsPage(emptyList(), nextLink = null, totalPages = null))

    /** Queue of results returned by successive [nextPage] calls. */
    val nextPageResults: ArrayDeque<NetworkResult<TransactionsPage, NetworkError>> = ArrayDeque()

    var firstPageCount: Int = 0
        private set
    var nextPageCount: Int = 0
        private set
    var lastNextLink: String? = null
        private set

    override fun transactionsState(
        accountIdFlow: Flow<String>,
        scope: CoroutineScope,
    ): Flow<ScreenState<List<TransactionItem>>> = emptyFlow()

    override fun refresh() = Unit

    override suspend fun firstPage(accountId: String): NetworkResult<TransactionsPage, NetworkError> {
        firstPageCount++
        return firstPageResult
    }

    override suspend fun nextPage(nextLink: String): NetworkResult<TransactionsPage, NetworkError> {
        nextPageCount++
        lastNextLink = nextLink
        return nextPageResults.removeFirstOrNull()
            ?: NetworkResult.Success(TransactionsPage(emptyList(), nextLink = null, totalPages = null))
    }
}
