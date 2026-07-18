/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Bridges the OAuth redirect from the platform layer into the common login flow. The OIDC provider
 * redirects to the app's custom-scheme URI, which the platform (Android `MainActivity.onNewIntent`,
 * iOS URL handler) receives and forwards via [emit]; the login ViewModel collects [callbacks] and
 * resumes the token exchange. A singleton so both the platform entry point and the ViewModel share it.
 */
class OidcCallbackBus {

    private val _callbacks = MutableSharedFlow<Callback>(extraBufferCapacity = 1)
    val callbacks: SharedFlow<Callback> = _callbacks.asSharedFlow()

    /** Forward an OAuth callback's authorization [code] + CSRF [state] from a received redirect URI. */
    fun emit(code: String, state: String) {
        _callbacks.tryEmit(Callback(code, state))
    }

    /** An OAuth authorization-code redirect parsed from the callback URI. */
    data class Callback(val code: String, val state: String)
}
