/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.store.standingorders

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.mifosx.openbanking.core.database.cache.ObpCacheDao
import org.mifosx.openbanking.core.model.obp.ObpException
import org.mifosx.openbanking.core.model.obp.StandingOrder
import org.mifosx.openbanking.core.network.api.StandingOrdersApi
import org.mifosx.openbanking.core.network.obp.ObpConfig
import org.mifosx.openbanking.core.store.infra.keyedJsonCachedStore
import org.mobilenativefoundation.store.store5.Store
import template.core.base.network.NetworkResult

/**
 * Durable, offline-first Store5 store for an account's standing orders, keyed by
 * accountId. Backed by the Room JSON cache.
 */
fun provideStandingOrdersStore(
    api: StandingOrdersApi,
    config: ObpConfig,
    dao: ObpCacheDao,
    json: Json,
): Store<String, List<StandingOrder>> = keyedJsonCachedStore(
    dao = dao,
    json = json,
    serializer = ListSerializer(StandingOrder.serializer()),
    cacheKey = { accountId -> "standing-orders:$accountId" },
) { accountId ->
    when (val result = api.listStandingOrders(config.bankId, accountId)) {
        is NetworkResult.Success -> result.data.standingOrders
        is NetworkResult.Error -> throw ObpException(reason = result.error.name)
    }
}
