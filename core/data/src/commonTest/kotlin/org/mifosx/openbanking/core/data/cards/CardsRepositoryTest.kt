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

import kotlinx.coroutines.test.runTest
import org.mifosx.openbanking.core.data.testutil.FakeObpCacheDao
import org.mifosx.openbanking.core.data.testutil.NoopFetchedAt
import org.mifosx.openbanking.core.data.testutil.testJson
import org.mifosx.openbanking.core.data.testutil.testNetworkMonitor
import org.mifosx.openbanking.core.model.obp.Card
import org.mifosx.openbanking.core.model.obp.CardsResponse
import org.mifosx.openbanking.core.network.api.CardsApi
import org.mifosx.openbanking.core.network.obp.ObpConfig
import org.mifosx.openbanking.core.store.cards.provideCardsStore
import org.mifosx.openbanking.core.store.cards.provideUserCardsStore
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeCardsApi(
    var listResult: NetworkResult<CardsResponse, NetworkError> = NetworkResult.Success(CardsResponse()),
) : CardsApi {
    override suspend fun getCardsForCurrentUser() = listResult
    override suspend fun listCards(bankId: String, accountId: String) = listResult
    override suspend fun getCard(bankId: String, accountId: String, cardId: String) =
        NetworkResult.Success(Card())
}

private fun cardsRepo(api: CardsApi): CardsRepositoryImpl {
    val config = ObpConfig()
    return CardsRepositoryImpl(
        api,
        config,
        provideCardsStore(api, config, FakeObpCacheDao(), testJson()),
        provideUserCardsStore(api, FakeObpCacheDao(), testJson()),
        testNetworkMonitor(),
        NoopFetchedAt,
    )
}

class CardsRepositoryTest {

    @Test
    fun listCards_unwrapsList() = runTest {
        val repo = cardsRepo(
            FakeCardsApi(NetworkResult.Success(CardsResponse(cards = listOf(Card(id = "c1"))))),
        )
        val result = repo.listCards("acc-1")
        assertTrue(result.isSuccess)
        assertEquals("c1", result.getOrNull()?.firstOrNull()?.id)
    }

    @Test
    fun listCards_error_isFailure() = runTest {
        val repo = cardsRepo(FakeCardsApi(NetworkResult.Error(NetworkError.UNAUTHORIZED)))
        assertTrue(repo.listCards("acc-1").isFailure)
    }
}
