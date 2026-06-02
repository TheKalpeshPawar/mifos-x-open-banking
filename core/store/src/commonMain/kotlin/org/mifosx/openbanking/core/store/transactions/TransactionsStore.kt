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
import org.mifosx.openbanking.core.store.infra.keyedJsonCachedStore
import org.mobilenativefoundation.store.store5.Store
import template.core.base.network.NetworkResult

/** Separator joining bankId + accountId into the transactions Store key. */
private const val KEY_SEP = "/"

/**
 * Build the composite Store key for an account's transactions. Each account is
 * fetched from its OWN bank (cross-bank safe — `/my/accounts` returns accounts at
 * multiple banks), so the bankId is part of the cache identity, not a global config.
 */
fun transactionsStoreKey(bankId: String, accountId: String): String = "$bankId$KEY_SEP$accountId"

/**
 * Durable, offline-first Store5 store for an account's transactions, keyed by
 * `"$bankId/$accountId"` (one cached payload per bank+account). Backed by the Room
 * JSON cache. The bankId travels in the key so each account hits its own bank.
 */
fun provideTransactionsStore(
    api: TransactionsApi,
    dao: ObpCacheDao,
    json: Json,
): Store<String, List<Transaction>> = keyedJsonCachedStore(
    dao = dao,
    json = json,
    serializer = ListSerializer(Transaction.serializer()),
    cacheKey = { key -> "transactions:$key" },
) { key ->
    val bankId = key.substringBefore(KEY_SEP)
    val accountId = key.substringAfter(KEY_SEP)
    when (val result = api.listTransactions(bankId, accountId, limit = null, offset = null)) {
        is NetworkResult.Success -> result.data.transactions
        is NetworkResult.Error -> throw ObpException(reason = result.error.name)
    }
}
