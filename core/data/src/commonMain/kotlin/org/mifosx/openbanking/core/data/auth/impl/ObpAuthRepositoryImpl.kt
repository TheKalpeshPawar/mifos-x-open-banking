/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.auth.impl

import org.mifosx.openbanking.core.data.auth.ObpAuthRepository
import org.mifosx.openbanking.core.data.obp.toResult
import org.mifosx.openbanking.core.model.obp.OidcConfiguration
import org.mifosx.openbanking.core.network.api.AuthApi
import org.mifosx.openbanking.core.network.obp.ObpAuth
import org.mifosx.openbanking.core.network.obp.ObpConfig
import org.mifosx.openbanking.core.network.obp.ObpTokenProvider
import org.mifosx.openbanking.core.network.obp.OidcApi
import org.mifosx.openbanking.core.network.obp.Pkce

/**
 * Auth repository for both DirectLogin and OIDC.
 *
 * DirectLogin posts credentials in the `Authorization` header and stores the returned token.
 *
 * OIDC ([prepareOidcAuthorization] + [completeOidc]) drives an authorization-code + PKCE (S256)
 * flow against the discovered OBP-OIDC provider: it discovers endpoints via OBP `well-known`,
 * dynamically registers a native public client (so the custom-scheme redirect is whitelisted —
 * otherwise the provider rejects it with `invalid_client`), and exchanges the code for an access
 * token stored as a `Bearer` credential. Discovery, the registered `client_id`, and the per-flow
 * CSRF state + PKCE verifier are cached in memory for the process lifetime.
 */
class ObpAuthRepositoryImpl(
    private val authApi: AuthApi,
    private val oidcApi: OidcApi,
    private val config: ObpConfig,
    private val tokenProvider: ObpTokenProvider,
) : ObpAuthRepository {

    private var discovered: OidcConfiguration? = null
    private var clientId: String? = null
    private var pendingState: String? = null
    private var pendingVerifier: String? = null

    override suspend fun login(username: String, password: String): Result<Unit> {
        val header = ObpAuth.loginHeader(username, password, config.consumerKey)
        return authApi.directLogin(header)
            .toResult()
            .map { response -> tokenProvider.setToken(response.token) }
    }

    override suspend fun prepareOidcAuthorization(): Result<String> = runCatching {
        val oidc = discovered ?: oidcApi.discover().also { discovered = it }
        val id = clientId ?: oidcApi.register(oidc.registrationEndpoint, REDIRECT_URI).clientId.also { clientId = it }
        require(id.isNotBlank()) { "OIDC client registration returned no client_id" }
        val verifier = Pkce.generateVerifier()
        pendingVerifier = verifier
        pendingState = Pkce.randomState()
        authorizeUrl(oidc.authorizationEndpoint, id, pendingState!!, Pkce.challengeS256(verifier))
    }

    override suspend fun completeOidc(code: String, state: String): Result<Unit> = runCatching {
        val oidc = requireNotNull(discovered) { "OIDC provider not discovered" }
        val id = requireNotNull(clientId) { "OIDC client not registered" }
        val verifier = requireNotNull(pendingVerifier) { "Missing PKCE verifier" }
        require(pendingState != null && pendingState == state) { "OIDC state mismatch" }
        val token = oidcApi.exchangeToken(
            tokenEndpoint = oidc.tokenEndpoint,
            code = code,
            redirectUri = REDIRECT_URI,
            clientId = id,
            codeVerifier = verifier,
        )
        tokenProvider.setToken(token.accessToken, bearer = true)
        pendingState = null
        pendingVerifier = null
    }

    override fun logout() = tokenProvider.clear()

    override fun isLoggedIn(): Boolean = tokenProvider.token() != null

    /**
     * Build the authorize URL. Only [redirectUri][REDIRECT_URI] and the scope need percent-encoding;
     * [state]/[challenge]/[clientId] use URL-safe alphabets already (unreserved / base64url / token id).
     */
    private fun authorizeUrl(
        authorizationEndpoint: String,
        clientId: String,
        state: String,
        challenge: String,
    ): String =
        "$authorizationEndpoint" +
            "?response_type=code" +
            "&client_id=$clientId" +
            "&redirect_uri=$REDIRECT_URI_ENCODED" +
            "&scope=$SCOPE_ENCODED" +
            "&state=$state" +
            "&code_challenge=$challenge" +
            "&code_challenge_method=S256"

    private companion object {
        const val REDIRECT_URI = "org.mifosx.openbanking://oauth/callback"
        const val REDIRECT_URI_ENCODED = "org.mifosx.openbanking%3A%2F%2Foauth%2Fcallback"
        const val SCOPE_ENCODED = "openid%20profile%20email"
    }
}
