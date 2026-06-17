/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.store.atm

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.mifosx.openbanking.core.database.cache.ObpCacheDao
import org.mifosx.openbanking.core.model.obp.Atm
import org.mifosx.openbanking.core.model.obp.ObpException
import org.mifosx.openbanking.core.network.api.AtmApi
import org.mifosx.openbanking.core.network.obp.ObpConfig
import org.mifosx.openbanking.core.store.infra.jsonCachedStore
import org.mobilenativefoundation.store.store5.Store
import template.core.base.network.NetworkResult

/**
 * Durable, offline-first Store5 store for the bank's ATM locations, keyed by [Unit].
 * Backed by the Room JSON cache.
 */
fun provideAtmStore(
    api: AtmApi,
    config: ObpConfig,
    dao: ObpCacheDao,
    json: Json,
): Store<Unit, List<Atm>> = jsonCachedStore(
    cacheKey = "atms",
    dao = dao,
    json = json,
    serializer = ListSerializer(Atm.serializer()),
) {
    when (val result = api.listAtms(config.bankId)) {
        is NetworkResult.Success -> result.data.atms
        is NetworkResult.Error -> throw ObpException(reason = result.error.name)
    }
}
