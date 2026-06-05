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
import org.mobilenativefoundation.store.store5.Store
import template.core.base.store.infra.FetchedAtRepository
import template.core.base.store.screen.ScreenDataStream
import template.core.base.store.screen.asScreenStream

/**
 * Read access to an account's OBP transactions. Every call carries the account's own
 * [bankId] — accounts come from `/my/accounts` (cross-bank), so transactions must hit
 * the bank that actually holds the account, not a single global config bank.
 */
interface TransactionsRepository {
    /** Durable, offline-first stream of an account's transactions (cache-then-network). */
    fun transactionsStream(
        bankId: String,
        accountId: String,
        scope: CoroutineScope,
    ): ScreenDataStream<List<Transaction>>

    suspend fun listTransactions(bankId: String, accountId: String, limit: Int? = null): Result<List<Transaction>>

    /**
     * Transactions with their attributes inline (OBP v6) — carries CARD_ID / TXN_TYPE so the
     * caller can filter by card and label by type without per-transaction attribute fetches.
     */
    suspend fun listTransactionsWithAttributes(
        bankId: String,
        accountId: String,
        limit: Int? = null,
    ): Result<List<Transaction>>

    suspend fun getTransaction(bankId: String, accountId: String, transactionId: String): Result<Transaction>
}

class TransactionsRepositoryImpl(
    private val api: TransactionsApi,
    private val transactionsStore: Store<String, List<Transaction>>,
    private val networkMonitor: NetworkMonitor,
    private val fetchedAtRepository: FetchedAtRepository,
) : TransactionsRepository {

    override fun transactionsStream(
        bankId: String,
        accountId: String,
        scope: CoroutineScope,
    ): ScreenDataStream<List<Transaction>> {
        // Composite Store key — must match provideTransactionsStore's "$bankId/$accountId".
        val key = "$bankId/$accountId"
        return transactionsStore.asScreenStream(
            key = key,
            networkMonitor = networkMonitor,
            fetchedAtRepository = fetchedAtRepository,
            cacheKey = "transactions:$key",
            scope = scope,
            isEmpty = { it.isEmpty() },
        )
    }

    override suspend fun listTransactions(bankId: String, accountId: String, limit: Int?): Result<List<Transaction>> =
        api.listTransactions(bankId, accountId, limit).toResult().map { it.transactions }

    override suspend fun listTransactionsWithAttributes(
        bankId: String,
        accountId: String,
        limit: Int?,
    ): Result<List<Transaction>> =
        api.listTransactionsWithAttributes(bankId, accountId, limit).toResult().map { it.transactions }

    override suspend fun getTransaction(bankId: String, accountId: String, transactionId: String): Result<Transaction> =
        api.getTransaction(bankId, accountId, transactionId).toResult()
}
