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

import kotlinx.coroutines.test.runTest
import org.mobilenativefoundation.store.store5.Fetcher
import template.core.base.store.infra.StoreFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Verifies [getOnce] returns the first terminal Store response for both cache-first and forced-fresh
 * reads, and rethrows the fetcher's failure so the caller can decide how to handle it.
 */
class StoreOnceTest {

    private fun valueStore(value: String) =
        StoreFactory.createMemoryStore(fetcher = Fetcher.of { _: String -> value })

    @Test
    fun getOnceReturnsDataForACacheFirstRead() = runTest {
        assertEquals("v", valueStore("v").getOnce("k", refresh = false))
    }

    @Test
    fun getOnceReturnsDataForAForcedFreshRead() = runTest {
        assertEquals("v", valueStore("v").getOnce("k", refresh = true))
    }

    @Test
    fun getOnceRethrowsTheFetcherFailure() = runTest {
        val store = StoreFactory.createMemoryStore(
            fetcher = Fetcher.of { _: String -> error("balance unavailable") },
        )

        assertFailsWith<IllegalStateException> { store.getOnce("k", refresh = true) }
    }
}
