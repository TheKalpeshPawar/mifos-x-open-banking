/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.payments

import kotlinx.coroutines.CoroutineScope
import org.mifosx.openbanking.core.data.infra.NetworkMonitor
import org.mifosx.openbanking.core.data.obp.toResult
import org.mifosx.openbanking.core.model.obp.Counterparty
import org.mifosx.openbanking.core.network.api.PaymentsApi
import org.mifosx.openbanking.core.network.obp.ObpConfig
import org.mobilenativefoundation.store.store5.Store
import template.core.base.store.infra.FetchedAtRepository
import template.core.base.store.screen.ScreenDataStream
import template.core.base.store.screen.asScreenStream

/** Read access to an account's OBP counterparties (payees / beneficiaries). */
interface PaymentsRepository {
    /** Durable, offline-first stream of an account's beneficiaries (cache-then-network). */
    fun beneficiariesStream(accountId: String, scope: CoroutineScope): ScreenDataStream<List<Counterparty>>

    suspend fun listBeneficiaries(accountId: String): Result<List<Counterparty>>
}

class PaymentsRepositoryImpl(
    private val api: PaymentsApi,
    private val config: ObpConfig,
    private val counterpartiesStore: Store<String, List<Counterparty>>,
    private val networkMonitor: NetworkMonitor,
    private val fetchedAtRepository: FetchedAtRepository,
) : PaymentsRepository {

    override fun beneficiariesStream(
        accountId: String,
        scope: CoroutineScope,
    ): ScreenDataStream<List<Counterparty>> =
        counterpartiesStore.asScreenStream(
            key = accountId,
            networkMonitor = networkMonitor,
            fetchedAtRepository = fetchedAtRepository,
            cacheKey = "counterparties:$accountId",
            scope = scope,
            isEmpty = { it.isEmpty() },
        )

    override suspend fun listBeneficiaries(accountId: String): Result<List<Counterparty>> =
        api.listCounterparties(config.bankId, accountId).toResult().map { it.counterparties }
}
