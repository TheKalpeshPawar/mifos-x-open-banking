/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.mifosx.openbanking.core.model.hsbcProduct.AccountEndpoint

/**
 * Remembers which `(account, endpoint)` pairs the bank has refused with `U000`.
 *
 * `HsbcProductCapability` predicts support from HSBC's documented product matrix, but that matrix
 * is transcribed prose and cannot know about a product HSBC adds later. This is the other half:
 * whatever the prediction said, a refusal observed at runtime is the truth, and it is remembered so
 * the app stops offering the feature.
 *
 * The observation is a [Flow], not a snapshot read. A user can deep-link straight to standing
 * orders, take the refusal there, and navigate back — the account-detail screen has to drop the
 * chip without being reloaded.
 */
interface AccountCapabilityRegistry {

    /** Endpoints observed to be unsupported for [accountId]; emits again whenever one is added. */
    fun unsupportedStream(accountId: String): Flow<Set<AccountEndpoint>>

    /** Records that the bank refused [endpoint] for [accountId]. Idempotent. */
    fun markUnsupported(accountId: String, endpoint: AccountEndpoint)

    /** Drops everything. Must run on logout so account ids cannot outlive their PSU session. */
    fun clear()
}

/**
 * In-memory implementation.
 *
 * Deliberately not persisted, matching the direct-debit and standing-order stores it observes:
 * those are memory-only because a cached copy could outlive the consent that permitted it, and a
 * record of *which accounts a user holds* deserves the same treatment. The cost is that process
 * death repeats one refused request per account.
 */
class InMemoryAccountCapabilityRegistry : AccountCapabilityRegistry {

    private val unsupported = MutableStateFlow<Map<String, Set<AccountEndpoint>>>(emptyMap())

    override fun unsupportedStream(accountId: String): Flow<Set<AccountEndpoint>> =
        unsupported.map { it[accountId].orEmpty() }

    override fun markUnsupported(accountId: String, endpoint: AccountEndpoint) {
        unsupported.update { current ->
            val existing = current[accountId].orEmpty()
            if (endpoint in existing) current else current + (accountId to existing + endpoint)
        }
    }

    override fun clear() {
        unsupported.value = emptyMap()
    }
}
