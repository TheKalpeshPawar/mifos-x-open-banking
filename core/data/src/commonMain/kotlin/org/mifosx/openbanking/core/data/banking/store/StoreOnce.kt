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

import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import template.core.base.store.screen.requireData

/**
 * Reads a single value from a [Store] and returns, mirroring the one-shot idiom in
 * [template.core.base.store.paging.loadPage].
 *
 * Store's `stream()` never completes, so this filters the non-terminal responses and takes the
 * first [StoreReadResponse.Data] or [StoreReadResponse.Error]. On error [requireData] throws, letting
 * the caller decide whether one failed read should fail its whole operation.
 *
 * @param key The store key to read.
 * @param refresh When true, always hit the fetcher via [StoreReadRequest.fresh] (the caller waits for
 *   the network). When false, serve the cached value if present and only fetch on a miss.
 */
suspend fun <K : Any, V : Any> Store<K, V>.getOnce(key: K, refresh: Boolean): V =
    stream(if (refresh) StoreReadRequest.fresh(key) else StoreReadRequest.cached(key, refresh = false))
        .filterNot {
            it is StoreReadResponse.Loading ||
                it is StoreReadResponse.NoNewData ||
                it is StoreReadResponse.Initial
        }
        .first()
        .requireData()
