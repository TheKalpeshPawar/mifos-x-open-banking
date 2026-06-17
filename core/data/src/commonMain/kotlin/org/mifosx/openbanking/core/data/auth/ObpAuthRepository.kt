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

/** OBP session lifecycle for both DirectLogin and OIDC authorization-code login. */
interface ObpAuthRepository {

    /** Exchange DirectLogin credentials for a session token and retain it for subsequent calls. */
    suspend fun login(username: String, password: String): Result<Unit>

    /**
     * Prepare the OIDC authorization-code flow: discover the provider, register a public client if
     * needed, generate the CSRF state + PKCE verifier (held until [completeOidc]), and return the
     * authorization URL the caller should open in a browser. Failure means discovery/registration broke.
     */
    suspend fun prepareOidcAuthorization(): Result<String>

    /**
     * Complete the OIDC flow after the redirect: validate [state] against the value from
     * [prepareOidcAuthorization], exchange [code] for tokens, and retain the access token (sent as
     * `Bearer`) for subsequent calls.
     */
    suspend fun completeOidc(code: String, state: String): Result<Unit>

    /** Drop the session token (DirectLogin or OIDC). */
    fun logout()

    /** True when a session token is currently held. */
    fun isLoggedIn(): Boolean
}
