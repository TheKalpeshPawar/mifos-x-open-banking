/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.network.obp

/**
 * Holds the active DirectLogin session token for outbound request auth.
 *
 * Phase 3 ships an in-memory implementation; Phase 7 replaces it with one backed by
 * the platform secure storage (core-base:security) so the token survives restarts.
 */
interface ObpTokenProvider {
    fun token(): String?
    fun setToken(token: String?)
    fun clear()
}

/** In-memory token holder. Token is lost on process death (real persistence: Phase 7). */
class InMemoryObpTokenProvider : ObpTokenProvider {
    private var current: String? = null
    override fun token(): String? = current
    override fun setToken(token: String?) {
        current = token
    }
    override fun clear() {
        current = null
    }
}
