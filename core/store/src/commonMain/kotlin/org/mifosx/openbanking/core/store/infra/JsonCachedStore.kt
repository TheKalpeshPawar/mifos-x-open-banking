/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.store.infra

import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.mifosx.openbanking.core.database.cache.ObpCacheDao
import org.mifosx.openbanking.core.database.cache.ObpCacheEntity
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import template.core.base.store.infra.StoreFactory

/**
 * Durable Store5 store backed by the generic [ObpCacheEntity] JSON-blob table.
 *
 * Reads stream from Room (data survives process death → true offline-first); the
 * fetcher refreshes from the network and the source-of-truth writer persists the
 * serialized payload. Keyed by [Unit] — one cached list per [cacheKey]. The fetcher
 * must throw on failure so Store5 surfaces an error read-response.
 */
fun <T : Any> jsonCachedStore(
    cacheKey: String,
    dao: ObpCacheDao,
    json: Json,
    serializer: KSerializer<List<T>>,
    fetch: suspend () -> List<T>,
): Store<Unit, List<T>> = keyedJsonCachedStore<Unit, T>(
    dao = dao,
    json = json,
    serializer = serializer,
    cacheKey = { cacheKey },
    fetch = { fetch() },
)

/**
 * Keyed variant of [jsonCachedStore] for per-resource caches (e.g. transactions or
 * cards per account). [cacheKey] derives a stable row key from the Store key so each
 * key gets its own cached payload.
 */
fun <K : Any, T : Any> keyedJsonCachedStore(
    dao: ObpCacheDao,
    json: Json,
    serializer: KSerializer<List<T>>,
    cacheKey: (K) -> String,
    fetch: suspend (K) -> List<T>,
): Store<K, List<T>> = StoreFactory.createStore(
    fetcher = Fetcher.of { key: K -> fetch(key) },
    sourceOfTruth = SourceOfTruth.of(
        reader = { key: K ->
            dao.observe(cacheKey(key)).map { payload ->
                payload?.let { json.decodeFromString(serializer, it) }
            }
        },
        writer = { key: K, value: List<T> ->
            dao.upsert(
                ObpCacheEntity(storeKey = cacheKey(key), payload = json.encodeToString(serializer, value)),
            )
        },
    ),
)
