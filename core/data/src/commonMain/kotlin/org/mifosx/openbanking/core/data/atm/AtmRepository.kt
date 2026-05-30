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
}
