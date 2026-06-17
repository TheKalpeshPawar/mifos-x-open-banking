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

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Generic offline cache row: the JSON-serialized payload for an OBP read keyed by a
 * stable store key (e.g. "accounts", "transactions:{accountId}"). A single blob table
 * serves every cached read uniformly — no per-DTO entities/converters — which suits
 * cache-of-API-responses far better than normalized tables for deeply nested shapes.
 */
@Entity(tableName = "obp_cache")
data class ObpCacheEntity(
    @PrimaryKey val storeKey: String,
    val payload: String,
)
