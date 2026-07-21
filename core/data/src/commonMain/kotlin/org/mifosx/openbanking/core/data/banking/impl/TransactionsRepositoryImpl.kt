/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import org.mifosx.openbanking.core.data.banking.TransactionsRepository
import org.mifosx.openbanking.core.data.banking.mapper.toTransactionsPage
import org.mifosx.openbanking.core.data.infra.NetworkMonitor
import org.mifosx.openbanking.core.model.banking.TransactionItem
import org.mifosx.openbanking.core.model.banking.TransactionsPage
import org.mifosx.openbanking.core.network.api.Aisp
import org.mifosx.openbanking.core.network.model.ais.transactions.TransactionsResponse
import org.mobilenativefoundation.store.store5.Store
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import template.core.base.store.infra.FetchedAtRepository
import template.core.base.store.screen.ScreenDataStream
import template.core.base.store.screen.asScreenStream

internal class TransactionsRepositoryImpl(
    private val store: Store<String, List<TransactionItem>>,
    private val aisp: Aisp,
    private val networkMonitor: NetworkMonitor,
    private val fetchedAtRepository: FetchedAtRepository,
) : TransactionsRepository {

    override fun transactionsStream(
        accountIdFlow: Flow<String>,
        scope: CoroutineScope,
    ): ScreenDataStream<List<TransactionItem>> =
        store.asScreenStream(
            keyFlow = accountIdFlow,
            networkMonitor = networkMonitor,
            fetchedAtRepository = fetchedAtRepository,
            cacheKeyFor = { accountId -> "$CACHE_KEY:$accountId" },
            scope = scope,
        )

    override suspend fun firstPage(accountId: String): NetworkResult<TransactionsPage, NetworkError> =
        aisp.getTransactions(accountId).toPage(accountId)

    override suspend fun nextPage(nextLink: String): NetworkResult<TransactionsPage, NetworkError> =
        aisp.getTransactionsPage(nextLink).toPage(accountId = "")

    private fun NetworkResult<TransactionsResponse, NetworkError>.toPage(
        accountId: String,
    ): NetworkResult<TransactionsPage, NetworkError> = when (this) {
        is NetworkResult.Success -> NetworkResult.Success(data.toTransactionsPage(accountId))
        is NetworkResult.Error -> this
    }

    private companion object {
        const val CACHE_KEY = "home:transactions"
    }
}
