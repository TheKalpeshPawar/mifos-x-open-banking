/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.database.cache

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ObpCacheDao {

    /** Observe the cached payload for [storeKey]; emits null until first write. */
    @Query("SELECT payload FROM obp_cache WHERE storeKey = :storeKey")
    fun observe(storeKey: String): Flow<String?>

    @Upsert
    suspend fun upsert(entity: ObpCacheEntity)

    @Query("DELETE FROM obp_cache WHERE storeKey = :storeKey")
    suspend fun delete(storeKey: String)

    @Query("DELETE FROM obp_cache")
    suspend fun clear()
}
