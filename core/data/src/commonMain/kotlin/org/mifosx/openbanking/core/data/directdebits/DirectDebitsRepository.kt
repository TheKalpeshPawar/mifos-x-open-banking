/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.directdebits

import kotlinx.coroutines.CoroutineScope
import org.mifosx.openbanking.core.data.infra.NetworkMonitor
import org.mifosx.openbanking.core.data.obp.toResult
import org.mifosx.openbanking.core.model.obp.DirectDebit
import org.mifosx.openbanking.core.network.api.DirectDebitsApi
import org.mifosx.openbanking.core.network.obp.ObpConfig
import org.mobilenativefoundation.store.store5.Store
import template.core.base.store.infra.FetchedAtRepository
import template.core.base.store.screen.ScreenDataStream
import template.core.base.store.screen.asScreenStream

/** Read + cancel access to an account's OBP direct-debit mandates. */
interface DirectDebitsRepository {
    /** Durable, offline-first stream of an account's direct debits (cache-then-network). */
    fun directDebitsStream(accountId: String, scope: CoroutineScope): ScreenDataStream<List<DirectDebit>>

    suspend fun list(accountId: String): Result<List<DirectDebit>>
    suspend fun cancel(accountId: String, directDebitId: String): Result<DirectDebit>
}

class DirectDebitsRepositoryImpl(
    private val api: DirectDebitsApi,
    private val config: ObpConfig,
    private val directDebitsStore: Store<String, List<DirectDebit>>,
    private val networkMonitor: NetworkMonitor,
    private val fetchedAtRepository: FetchedAtRepository,
) : DirectDebitsRepository {

    override fun directDebitsStream(
        accountId: String,
        scope: CoroutineScope,
    ): ScreenDataStream<List<DirectDebit>> =
        directDebitsStore.asScreenStream(
            key = accountId,
            networkMonitor = networkMonitor,
            fetchedAtRepository = fetchedAtRepository,
            cacheKey = "direct-debits:$accountId",
            scope = scope,
            isEmpty = { it.isEmpty() },
        )

    override suspend fun list(accountId: String): Result<List<DirectDebit>> =
        api.listDirectDebits(config.bankId, accountId).toResult().map { it.directDebits }

    override suspend fun cancel(accountId: String, directDebitId: String): Result<DirectDebit> =
        api.cancelDirectDebit(config.bankId, accountId, directDebitId).toResult()
}
