/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.atm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.mifosx.openbanking.core.data.infra.NetworkMonitor
import org.mifosx.openbanking.core.data.obp.toResult
import org.mifosx.openbanking.core.model.obp.Atm
import org.mifosx.openbanking.core.network.api.AtmApi
import org.mifosx.openbanking.core.network.obp.ObpConfig
import org.mobilenativefoundation.store.store5.Store
import template.core.base.store.infra.FetchedAtRepository
import template.core.base.store.screen.ScreenDataStream
import template.core.base.store.screen.asScreenStream

/** Read access to OBP ATM locations. */
interface AtmRepository {
    /** Durable, offline-first stream of the bank's ATM locations (cache-then-network). */
    fun atmsStream(scope: CoroutineScope): ScreenDataStream<List<Atm>>

    suspend fun listAtms(): Result<List<Atm>>

    /**
     * ATMs across every bank in [bankIds], fetched in parallel and flattened. A bank whose
     * fetch fails contributes nothing rather than failing the whole call; the result fails
     * only when [bankIds] is empty or every bank fetch fails.
     */
    suspend fun atmsForBanks(bankIds: List<String>): Result<List<Atm>>
}

class AtmRepositoryImpl(
    private val api: AtmApi,
    private val config: ObpConfig,
    private val atmsStore: Store<Unit, List<Atm>>,
    private val networkMonitor: NetworkMonitor,
    private val fetchedAtRepository: FetchedAtRepository,
) : AtmRepository {

    override fun atmsStream(scope: CoroutineScope): ScreenDataStream<List<Atm>> =
        atmsStore.asScreenStream(
            key = Unit,
            networkMonitor = networkMonitor,
            fetchedAtRepository = fetchedAtRepository,
            cacheKey = "atms",
            scope = scope,
            isEmpty = { it.isEmpty() },
        )

    override suspend fun listAtms(): Result<List<Atm>> =
        api.listAtms(config.bankId).toResult().map { it.atms }

    override suspend fun atmsForBanks(bankIds: List<String>): Result<List<Atm>> {
        val banks = bankIds.filter { it.isNotBlank() }.distinct()
        if (banks.isEmpty()) return Result.failure(IllegalArgumentException("No banks to query for ATMs"))
        val results = coroutineScope {
            banks.map { bankId -> async { api.listAtms(bankId).toResult().map { it.atms } } }.awaitAll()
        }
        return if (results.all { it.isFailure }) {
            Result.failure(results.firstNotNullOf { it.exceptionOrNull() })
        } else {
            Result.success(results.flatMap { it.getOrDefaultList() }.distinctBy { it.id })
        }
    }

    private fun Result<List<Atm>>.getOrDefaultList(): List<Atm> = getOrElse { emptyList() }
}
