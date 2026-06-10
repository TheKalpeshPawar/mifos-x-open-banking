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

    /** Store the session token. [bearer] = true for OIDC access tokens (sent as `Bearer`), false for DirectLogin. */
    fun setToken(token: String?, bearer: Boolean = false)

    /** The full `Authorization` header for outbound calls (`Bearer …` or `DirectLogin token="…"`), or null. */
    fun authHeader(): String?
    fun clear()

    /** Emits once per session-invalidation (logout or 401). */
    val sessionInvalidations: SharedFlow<Unit>

    /** Clears the token AND signals invalidation (used by the 401 handler). */
    fun invalidateSession()
}

/** In-memory token holder. Token is lost on process death (real persistence: Phase 7). */
class InMemoryObpTokenProvider : ObpTokenProvider {
    private var current: String? = null
    private var isBearer: Boolean = false

    private val _sessionInvalidations = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val sessionInvalidations: SharedFlow<Unit> = _sessionInvalidations.asSharedFlow()

    override fun token(): String? = current
    override fun setToken(token: String?, bearer: Boolean) {
        current = token
        isBearer = bearer
    }
    override fun authHeader(): String? =
        current?.let { if (isBearer) ObpAuth.bearerHeader(it) else ObpAuth.tokenHeader(it) }
    override fun clear() {
        current = null
        isBearer = false
    }

    override fun invalidateSession() {
        current = null
        isBearer = false
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
    private var isBearer: Boolean = false

    private val _sessionInvalidations = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val sessionInvalidations: SharedFlow<Unit> = _sessionInvalidations.asSharedFlow()

    override fun token(): String? = current

    override fun setToken(token: String?, bearer: Boolean) {
        current = token
        isBearer = bearer
        scope.launch { preferences.setAuthToken(token) }
    }

    override fun authHeader(): String? =
        current?.let { if (isBearer) ObpAuth.bearerHeader(it) else ObpAuth.tokenHeader(it) }

    override fun clear() {
        current = null
        isBearer = false
        scope.launch { preferences.setAuthToken(null) }
    }

    override fun invalidateSession() {
        current = null
        isBearer = false
        scope.launch { preferences.setAuthToken(null) }
        _sessionInvalidations.tryEmit(Unit)
    }
}
