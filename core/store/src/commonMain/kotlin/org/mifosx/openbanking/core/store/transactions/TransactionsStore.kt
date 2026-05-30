/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.store.transactions

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.mifosx.openbanking.core.database.cache.ObpCacheDao
import org.mifosx.openbanking.core.model.obp.ObpException
import org.mifosx.openbanking.core.model.obp.Transaction
import org.mifosx.openbanking.core.network.api.TransactionsApi
import org.mifosx.openbanking.core.network.obp.ObpConfig
import org.mifosx.openbanking.core.store.infra.keyedJsonCachedStore
import org.mobilenativefoundation.store.store5.Store
import template.core.base.network.NetworkResult

/**
 * Durable, offline-first Store5 store for an account's transactions, keyed by
 * accountId (one cached payload per account). Backed by the Room JSON cache.
 */
fun provideTransactionsStore(
    api: TransactionsApi,
    config: ObpConfig,
    dao: ObpCacheDao,
    json: Json,
): Store<String, List<Transaction>> = keyedJsonCachedStore(
    dao = dao,
    json = json,
    serializer = ListSerializer(Transaction.serializer()),
    cacheKey = { accountId -> "transactions:$accountId" },
) { accountId ->
    when (val result = api.listTransactions(config.bankId, accountId, limit = null, offset = null)) {
        is NetworkResult.Success -> result.data.transactions
        is NetworkResult.Error -> throw ObpException(reason = result.error.name)
    }
}
