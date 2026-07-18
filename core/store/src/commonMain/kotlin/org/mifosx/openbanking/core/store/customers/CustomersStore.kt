/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.store.customers

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.mifosx.openbanking.core.database.cache.ObpCacheDao
import org.mifosx.openbanking.core.model.obp.Customer
import org.mifosx.openbanking.core.model.obp.ObpException
import org.mifosx.openbanking.core.network.api.CustomersApi
import org.mifosx.openbanking.core.network.obp.ObpConfig
import org.mifosx.openbanking.core.store.infra.jsonCachedStore
import org.mobilenativefoundation.store.store5.Store
import template.core.base.network.NetworkResult

/**
 * Durable, offline-first Store5 store for the bank's customers (field-officer
 * surface), keyed by [Unit]. Backed by the Room JSON cache.
 */
fun provideCustomersStore(
    api: CustomersApi,
    config: ObpConfig,
    dao: ObpCacheDao,
    json: Json,
): Store<Unit, List<Customer>> = jsonCachedStore(
    cacheKey = "customers",
    dao = dao,
    json = json,
    serializer = ListSerializer(Customer.serializer()),
) {
    when (val result = api.listCustomers(config.bankId)) {
        is NetworkResult.Success -> result.data.customers
        is NetworkResult.Error -> throw ObpException(reason = result.error.name)
    }
}
