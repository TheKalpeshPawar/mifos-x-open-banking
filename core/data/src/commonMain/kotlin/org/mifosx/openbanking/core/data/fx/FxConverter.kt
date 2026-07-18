/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.fx

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Session-cached FX rate lookup. Identity pairs return 1.0 without a network call. */
class FxConverter(
    private val fxRepository: FxRepository,
) {
    private val mutex = Mutex()
    private val rates = mutableMapOf<Pair<String, String>, Double>()

    /**
     * Multiplier converting [from] amounts into [to], or null when no rate is available.
     * The network fetch runs outside the lock so concurrent lookups for different pairs
     * never serialize; a duplicate fetch for the same pair is an idempotent cache write.
     */
    suspend fun rate(from: String, to: String): Double? = when {
        from.isBlank() || to.isBlank() || from == to -> 1.0
        else -> mutex.withLock { rates[from to to] } ?: fetchAndCache(from, to)
    }

    private suspend fun fetchAndCache(from: String, to: String): Double? {
        val fetched = fxRepository.getRate(from, to).getOrNull()
            ?.conversionValue?.takeIf { it > 0 } ?: return null
        return mutex.withLock { rates.getOrPut(from to to) { fetched } }
    }
}
