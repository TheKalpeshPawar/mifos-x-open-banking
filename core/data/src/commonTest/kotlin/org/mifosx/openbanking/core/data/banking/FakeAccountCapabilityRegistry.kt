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
 * Records what was marked unsupported, so a test can assert the store noticed a refusal.
 *
 * Behaves like the real in-memory registry rather than only spying, so it can also stand in for it
 * when a collaborator actually reads the stream back.
 */
class FakeAccountCapabilityRegistry : AccountCapabilityRegistry {

    private val state = MutableStateFlow<Map<String, Set<AccountEndpoint>>>(emptyMap())

    /** Every `(accountId, endpoint)` marked, in call order, including repeats. */
    var marked: List<Pair<String, AccountEndpoint>> = emptyList()
        private set

    var clearCount: Int = 0
        private set

    override fun unsupportedStream(accountId: String): Flow<Set<AccountEndpoint>> =
        state.map { it[accountId].orEmpty() }

    override fun unsupportedStream(): Flow<Map<String, Set<AccountEndpoint>>> = state

    override fun markUnsupported(accountId: String, endpoint: AccountEndpoint) {
        marked = marked + (accountId to endpoint)
        state.update { current ->
            current + (accountId to current[accountId].orEmpty() + endpoint)
        }
    }

    override fun clear() {
        clearCount++
        marked = emptyList()
        state.value = emptyMap()
    }
}
