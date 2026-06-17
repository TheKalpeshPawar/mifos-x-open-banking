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
import org.mifosx.openbanking.core.model.obp.Bank
import org.mifosx.openbanking.core.network.api.BanksApi

/** Resolves OBP bank ids to directory entries, with an in-memory cache. */
interface BanksRepository {
    /**
     * Returns the bank's display name for [bankId], or [bankId] itself when the bank
     * cannot be resolved (e.g. a BIC code rather than an OBP bank id). Never throws.
     */
    suspend fun bankName(bankId: String): String

    /**
     * Returns the full directory entry for [bankId] (routings + attributes, used to derive
     * the bank's country), or null when the bank cannot be resolved. Never throws.
     */
    suspend fun bank(bankId: String): Bank?
}

class BanksRepositoryImpl(
    private val api: BanksApi,
) : BanksRepository {

    private val nameCache = mutableMapOf<String, String>()
    private val bankCache = mutableMapOf<String, Bank>()
    private val mutex = Mutex()

    override suspend fun bankName(bankId: String): String {
        if (bankId.isBlank()) return bankId
        return mutex.withLock { nameCache[bankId] } ?: resolveNameAndCache(bankId)
    }

    override suspend fun bank(bankId: String): Bank? {
        if (bankId.isBlank()) return null
        return mutex.withLock { bankCache[bankId] } ?: fetchAndCacheBank(bankId)
    }

    private suspend fun resolveNameAndCache(bankId: String): String {
        val resolved = bank(bankId)
            ?.let { it.fullName.ifBlank { it.shortName } }
            ?.ifBlank { bankId }
            ?: bankId
        mutex.withLock { nameCache[bankId] = resolved }
        return resolved
    }

    private suspend fun fetchAndCacheBank(bankId: String): Bank? {
        val bank = api.getBank(bankId).toResult().getOrNull()
        if (bank != null) mutex.withLock { bankCache[bankId] = bank }
        return bank
    }
}
