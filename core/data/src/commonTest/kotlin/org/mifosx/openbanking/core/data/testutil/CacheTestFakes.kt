/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.testutil

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import org.mifosx.openbanking.core.data.infra.NetworkMonitor
import org.mifosx.openbanking.core.data.infra.impl.NetworkMonitorImpl
import org.mifosx.openbanking.core.database.cache.ObpCacheDao
import org.mifosx.openbanking.core.database.cache.ObpCacheEntity
import template.core.base.store.infra.FetchedAtRepository
import kotlin.time.Instant

/** No-op timestamp store for tests. */
internal object NoopFetchedAt : FetchedAtRepository {
    override suspend fun read(storeKey: String): Instant? = null
    override suspend fun write(storeKey: String, instant: Instant) {}
}

/** In-memory [ObpCacheDao] for tests. */
internal class FakeObpCacheDao : ObpCacheDao {
    private val rows = MutableStateFlow<Map<String, String>>(emptyMap())
    override fun observe(storeKey: String): Flow<String?> = rows.map { it[storeKey] }
    override suspend fun upsert(entity: ObpCacheEntity) {
        rows.value = rows.value + (entity.storeKey to entity.payload)
    }
    override suspend fun delete(storeKey: String) {
        rows.value = rows.value - storeKey
    }
    override suspend fun clear() {
        rows.value = emptyMap()
    }
}

internal fun testJson(): Json = Json { ignoreUnknownKeys = true }

internal fun testNetworkMonitor(): NetworkMonitor = NetworkMonitorImpl()
