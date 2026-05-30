/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.transactions

import kotlinx.coroutines.CoroutineScope
import org.mifosx.openbanking.core.data.infra.NetworkMonitor
import org.mifosx.openbanking.core.data.obp.toResult
import org.mifosx.openbanking.core.model.obp.Transaction
import org.mifosx.openbanking.core.network.api.TransactionsApi
import org.mifosx.openbanking.core.network.obp.ObpConfig
import org.mobilenativefoundation.store.store5.Store
import template.core.base.store.infra.FetchedAtRepository
import template.core.base.store.screen.ScreenDataStream
import template.core.base.store.screen.asScreenStream

/** Read access to an account's OBP transactions. */
interface TransactionsRepository {
    /** Durable, offline-first stream of an account's transactions (cache-then-network). */
    fun transactionsStream(accountId: String, scope: CoroutineScope): ScreenDataStream<List<Transaction>>

    suspend fun listTransactions(accountId: String, limit: Int? = null): Result<List<Transaction>>
    suspend fun getTransaction(accountId: String, transactionId: String): Result<Transaction>
}

class TransactionsRepositoryImpl(
    private val api: TransactionsApi,
    private val config: ObpConfig,
    private val transactionsStore: Store<String, List<Transaction>>,
    private val networkMonitor: NetworkMonitor,
    private val fetchedAtRepository: FetchedAtRepository,
) : TransactionsRepository {

    override fun transactionsStream(
        accountId: String,
        scope: CoroutineScope,
    ): ScreenDataStream<List<Transaction>> =
        transactionsStore.asScreenStream(
            key = accountId,
            networkMonitor = networkMonitor,
            fetchedAtRepository = fetchedAtRepository,
            cacheKey = "transactions:$accountId",
            scope = scope,
            isEmpty = { it.isEmpty() },
        )

    override suspend fun listTransactions(accountId: String, limit: Int?): Result<List<Transaction>> =
        api.listTransactions(config.bankId, accountId, limit).toResult().map { it.transactions }

    override suspend fun getTransaction(accountId: String, transactionId: String): Result<Transaction> =
        api.getTransaction(config.bankId, accountId, transactionId).toResult()
}
