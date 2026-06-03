/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.banks

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.mifosx.openbanking.core.data.obp.toResult
import org.mifosx.openbanking.core.network.api.BanksApi

/** Resolves OBP bank ids to display names, with an in-memory cache. */
interface BanksRepository {
    /**
     * Returns the bank's display name for [bankId], or [bankId] itself when the bank
     * cannot be resolved (e.g. a BIC code rather than an OBP bank id). Never throws.
     */
    suspend fun bankName(bankId: String): String
}

class BanksRepositoryImpl(
    private val api: BanksApi,
) : BanksRepository {

    private val cache = mutableMapOf<String, String>()
    private val mutex = Mutex()

    override suspend fun bankName(bankId: String): String {
        if (bankId.isBlank()) return bankId
        return mutex.withLock { cache[bankId] } ?: resolveAndCache(bankId)
    }

    private suspend fun resolveAndCache(bankId: String): String {
        val resolved = api.getBank(bankId).toResult().fold(
            onSuccess = { it.fullName.ifBlank { it.shortName.ifBlank { bankId } } },
            onFailure = { bankId },
        )
        mutex.withLock { cache[bankId] = resolved }
        return resolved
    }
}
