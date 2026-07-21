/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accountdetail

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.mifosx.openbanking.core.data.banking.AccountCapabilityRegistry
import org.mifosx.openbanking.core.model.hsbcProduct.AccountEndpoint

/**
 * Test double for the capability registry.
 *
 * Behaves like the real in-memory one rather than only recording calls, because the view model
 * actually collects [unsupportedStream] — a pure spy would emit nothing and the chip set would
 * never update.
 *
 * A near-identical fake exists in `core/data`'s test source set; test fixtures are not shared
 * across modules, so each consumer keeps its own.
 */
class FakeAccountCapabilityRegistry : AccountCapabilityRegistry {

    private val state = MutableStateFlow<Map<String, Set<AccountEndpoint>>>(emptyMap())

    override fun unsupportedStream(accountId: String): Flow<Set<AccountEndpoint>> =
        state.map { it[accountId].orEmpty() }

    override fun markUnsupported(accountId: String, endpoint: AccountEndpoint) {
        state.update { current ->
            current + (accountId to current[accountId].orEmpty() + endpoint)
        }
    }

    override fun clear() {
        state.value = emptyMap()
    }
}
