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

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.parameters
import org.mifosx.openbanking.core.model.obp.OidcClientRegistrationRequest
import org.mifosx.openbanking.core.model.obp.OidcClientRegistrationResponse
import org.mifosx.openbanking.core.model.obp.OidcConfiguration
import org.mifosx.openbanking.core.model.obp.OidcProviderList
import org.mifosx.openbanking.core.model.obp.OidcToken

/**
 * OIDC provider client. Unlike the OBP API (one base host + DirectLogin auth plugin), the OIDC
 * flow spans two hosts (OBP for discovery, the OIDC provider for auth/token/registration) and must
 * NOT carry the session token, so this uses a plain [client] with absolute URLs. Calls throw on
 * transport/HTTP error; the repository wraps them in `runCatching`.
 *
 * @param client a bare JSON Ktor client with no OBP auth plugin.
 * @param obpBaseUrl the OBP API base (e.g. `https://apisandbox.openbankproject.com/obp/`), used to
 *   reach the `well-known` provider-discovery endpoint.
 */
class OidcApi(
    private val client: HttpClient,
    private val obpBaseUrl: String,
) {

    /** Discover the first OIDC provider via OBP `well-known`, then fetch its OpenID configuration. */
    suspend fun discover(): OidcConfiguration {
        val providers: OidcProviderList = client.get("${obpBaseUrl}v5.1.0/well-known").body()
        val wellKnownUrl = providers.wellKnownUris.firstOrNull()?.url
            ?: error("No OIDC provider advertised by OBP well-known")
        return client.get(wellKnownUrl).body()
    }

    /** Dynamically register this app as a native public client (RFC 7591); returns its `client_id`. */
    suspend fun register(registrationEndpoint: String, redirectUri: String): OidcClientRegistrationResponse =
        client.post(registrationEndpoint) {
            contentType(ContentType.Application.Json)
            setBody(
                OidcClientRegistrationRequest(
                    clientName = "Mifos X Open Banking",
                    redirectUris = listOf(redirectUri),
                ),
            )
        }.body()

    /** Exchange an authorization [code] (+ PKCE [codeVerifier]) for tokens at a public client's token endpoint. */
    suspend fun exchangeToken(
        tokenEndpoint: String,
        code: String,
        redirectUri: String,
        clientId: String,
        codeVerifier: String,
    ): OidcToken =
        client.submitForm(
            url = tokenEndpoint,
            formParameters = parameters {
                append("grant_type", "authorization_code")
                append("code", code)
                append("redirect_uri", redirectUri)
                append("client_id", clientId)
                append("code_verifier", codeVerifier)
            },
        ).body()
}
