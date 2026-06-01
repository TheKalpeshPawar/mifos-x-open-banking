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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.mifosx.openbanking.core.datastore.UserPreferencesRepository

/**
 * Holds the active DirectLogin session token for outbound request auth.
 *
 * Phase 3 ships an in-memory implementation; Phase 7 replaces it with one backed by
 * the platform secure storage (core-base:security) so the token survives restarts.
 *
 * [sessionInvalidations] emits whenever the session becomes invalid (explicit logout or a
 * 401 from the server). The app layer observes it to drop the user back to the login screen.
 */
interface ObpTokenProvider {
    fun token(): String?
    fun setToken(token: String?)
    fun clear()

    /** Emits once per session-invalidation (logout or 401). */
    val sessionInvalidations: SharedFlow<Unit>

    /** Clears the token AND signals invalidation (used by the 401 handler). */
    fun invalidateSession()
}

/** In-memory token holder. Token is lost on process death (real persistence: Phase 7). */
class InMemoryObpTokenProvider : ObpTokenProvider {
    private var current: String? = null

    private val _sessionInvalidations = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val sessionInvalidations: SharedFlow<Unit> = _sessionInvalidations.asSharedFlow()

    override fun token(): String? = current
    override fun setToken(token: String?) {
        current = token
    }
    override fun clear() {
        current = null
    }

    override fun invalidateSession() {
        current = null
        _sessionInvalidations.tryEmit(Unit)
    }
}

/**
 * Token holder backed by encrypted secure storage ([UserPreferencesRepository]).
 *
 * The token is cached in memory for synchronous [token] reads and written through to
 * secure storage on every mutation, so the DirectLogin session survives process death.
 * On construction the cache is seeded from the persisted value (the repository loads it
 * in its own constructor), so a cold start restores the session without re-login.
 */
class PersistentObpTokenProvider(
    private val preferences: UserPreferencesRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : ObpTokenProvider {
    private var current: String? = preferences.authToken

    private val _sessionInvalidations = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val sessionInvalidations: SharedFlow<Unit> = _sessionInvalidations.asSharedFlow()

    override fun token(): String? = current

    override fun setToken(token: String?) {
        current = token
        scope.launch { preferences.setAuthToken(token) }
    }

    override fun clear() {
        current = null
        scope.launch { preferences.setAuthToken(null) }
    }

    override fun invalidateSession() {
        current = null
        scope.launch { preferences.setAuthToken(null) }
        _sessionInvalidations.tryEmit(Unit)
    }
}
