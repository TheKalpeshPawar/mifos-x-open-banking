/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking.store

import template.core.base.store.infra.FetchedAtRepository
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * In-memory [FetchedAtRepository] for tests, mirroring the framework fixture. Records the store keys
 * it is asked to read and write so a test can assert the `cacheKeyFor` prefix the repository
 * computed (e.g. `home:balance:acc-1`).
 *
 * Recorded keys are held copy-on-write, so they stay safe to read while background stream
 * coroutines are still appending to them.
 */
@OptIn(ExperimentalTime::class)
class FakeFetchedAtRepository : FetchedAtRepository {

    /** Store keys passed to [read], oldest first. */
    var readKeys: List<String> = emptyList()
        private set

    /** Store keys passed to [write], oldest first. */
    var writeKeys: List<String> = emptyList()
        private set

    private val store = mutableMapOf<String, Instant>()

    override suspend fun read(storeKey: String): Instant? {
        readKeys = readKeys + storeKey
        return store[storeKey]
    }

    override suspend fun write(storeKey: String, instant: Instant) {
        writeKeys = writeKeys + storeKey
        store[storeKey] = instant
    }
}
