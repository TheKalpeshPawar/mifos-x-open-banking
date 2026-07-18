/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.store.cards

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.mifosx.openbanking.core.database.cache.ObpCacheDao
import org.mifosx.openbanking.core.model.obp.Card
import org.mifosx.openbanking.core.model.obp.ObpException
import org.mifosx.openbanking.core.network.api.CardsApi
import org.mifosx.openbanking.core.store.infra.jsonCachedStore
import org.mobilenativefoundation.store.store5.Store
import template.core.base.network.NetworkResult

/**
 * Durable, offline-first Store5 store for the current user's full card list, keyed by
 * [Unit] (one cached payload for the signed-in user). Backed by [GET /obp/v7.0.0/cards]
 * via [CardsApi.getCardsForCurrentUser] — distinct from the per-account [provideCardsStore],
 * which keys by accountId. This is the "My Cards" carousel source.
 */
fun provideUserCardsStore(
    api: CardsApi,
    dao: ObpCacheDao,
    json: Json,
): Store<Unit, List<Card>> = jsonCachedStore(
    cacheKey = "user-cards",
    dao = dao,
    json = json,
    serializer = ListSerializer(Card.serializer()),
) {
    when (val result = api.getCardsForCurrentUser()) {
        is NetworkResult.Success -> result.data.cards
        is NetworkResult.Error -> throw ObpException(reason = result.error.name)
    }
}
