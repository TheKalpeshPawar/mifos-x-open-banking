/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.cards

import kotlinx.coroutines.CoroutineScope
import org.mifosx.openbanking.core.data.infra.NetworkMonitor
import org.mifosx.openbanking.core.data.obp.toResult
import org.mifosx.openbanking.core.model.obp.Card
import org.mifosx.openbanking.core.network.api.CardsApi
import org.mifosx.openbanking.core.network.obp.ObpConfig
import org.mobilenativefoundation.store.store5.Store
import template.core.base.store.infra.FetchedAtRepository
import template.core.base.store.screen.ScreenDataStream
import template.core.base.store.screen.asScreenStream

/** Read access to an account's OBP cards. */
interface CardsRepository {
    /** Durable, offline-first stream of an account's cards (cache-then-network). */
    fun cardsStream(accountId: String, scope: CoroutineScope): ScreenDataStream<List<Card>>

    suspend fun listCards(accountId: String): Result<List<Card>>
    suspend fun getCard(accountId: String, cardId: String): Result<Card>
}

class CardsRepositoryImpl(
    private val api: CardsApi,
    private val config: ObpConfig,
    private val cardsStore: Store<String, List<Card>>,
    private val networkMonitor: NetworkMonitor,
    private val fetchedAtRepository: FetchedAtRepository,
) : CardsRepository {

    override fun cardsStream(
        accountId: String,
        scope: CoroutineScope,
    ): ScreenDataStream<List<Card>> =
        cardsStore.asScreenStream(
            key = accountId,
            networkMonitor = networkMonitor,
            fetchedAtRepository = fetchedAtRepository,
            cacheKey = "cards:$accountId",
            scope = scope,
            isEmpty = { it.isEmpty() },
        )

    override suspend fun listCards(accountId: String): Result<List<Card>> =
        api.listCards(config.bankId, accountId).toResult().map { it.cards }

    override suspend fun getCard(accountId: String, cardId: String): Result<Card> =
        api.getCard(config.bankId, accountId, cardId).toResult()
}
