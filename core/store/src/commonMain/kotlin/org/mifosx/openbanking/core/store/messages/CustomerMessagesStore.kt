/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.store.messages

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.mifosx.openbanking.core.database.cache.ObpCacheDao
import org.mifosx.openbanking.core.model.obp.CustomerMessage
import org.mifosx.openbanking.core.model.obp.ObpException
import org.mifosx.openbanking.core.network.api.CustomerMessagesApi
import org.mifosx.openbanking.core.network.obp.ObpConfig
import org.mifosx.openbanking.core.store.infra.keyedJsonCachedStore
import org.mobilenativefoundation.store.store5.Store
import template.core.base.network.NetworkResult

/**
 * Durable, offline-first Store5 store for a customer's message thread, keyed by
 * customerId. Backed by the Room JSON cache.
 */
fun provideCustomerMessagesStore(
    api: CustomerMessagesApi,
    config: ObpConfig,
    dao: ObpCacheDao,
    json: Json,
): Store<String, List<CustomerMessage>> = keyedJsonCachedStore(
    dao = dao,
    json = json,
    serializer = ListSerializer(CustomerMessage.serializer()),
    cacheKey = { customerId -> "messages:$customerId" },
) { customerId ->
    when (val result = api.listMessages(config.bankId, customerId)) {
        is NetworkResult.Success -> result.data.messages
        is NetworkResult.Error -> throw ObpException(reason = result.error.name)
    }
}
