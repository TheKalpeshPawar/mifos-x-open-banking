/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.authorize

import io.ktor.http.URLBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.mifosx.openbanking.core.network.api.ConsentCreationScope
import org.mifosx.openbanking.core.network.signPs256
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val REQUEST_OBJECT_TTL_SECONDS = 1200L
private const val NBF_SKEW_SECONDS = 1L
private const val SCA_ACR = "urn:openbanking:psd2:sca"
private const val CA_ACR = "urn:openbanking:psd2:ca"

/**
 * The result of starting the FAPI authorization-code flow: the front-channel [authorizationUrl] the
 * app opens in a browser (Custom Tab / ASWebAuthenticationSession), plus the [state] and [nonce] the
 * caller MUST persist so the redirect callback can validate `state`/`nonce` before completing the
 * token exchange.
 */
data class ConsentAuthorization(
    val authorizationUrl: String,
    val state: String,
    val nonce: String,
)

/**
 * Builds the HSBC OBIE authorization URL for an already-created consent.
 *
 * This is a front-channel URL (not an HTTP call): it carries the OIDC query parameters plus a signed
 * FAPI **request object** JWT (PS256) whose claims bind the flow to [consentId] via
 * `openbanking_intent_id`. HSBC uses the OIDC **hybrid** flow (`response_type = "code id_token"` with
 * a `nonce`) and does NOT use PKCE. The app opens the returned URL; HSBC then redirects to
 * [redirectUri] with `#code&id_token&state` for the callback to exchange.
 *
 * Mirrors the sandbox reference `gen-request-jwt.js` exactly.
 *
 * @param audience the OIDC issuer used as the request object `aud` claim (the mTLS `secure.*` host).
 * @param authorizeUrl the ABSOLUTE front-channel authorize endpoint the browser opens — served from
 *   the OIDC authorize host, which is DISTINCT from [audience]'s mTLS host. Must be a full
 *   `https://<authorize-host>/obie/open-banking/v1.1/oauth2/authorize`; a relative value would build
 *   against `http://localhost` and the page would never load.
 * @param nowEpochSeconds current wall-clock seconds; drives the request object `exp`/`nbf`.
 */
@OptIn(ExperimentalUuidApi::class)
suspend fun generateConsentAuthorizationUrl(
    audience: String,
    authorizeUrl: String,
    scope: ConsentCreationScope,
    redirectUri: String,
    consentId: String,
    signingKeyPem: String,
    nowEpochSeconds: Long,
    responseType: String,
    clientId: String,
    kid: String,
): ConsentAuthorization {
    val state = Uuid.generateV4().toString()
    val nonce = Uuid.generateV4().toString()

    val header = buildJsonObject {
        put("alg", "PS256")
        put("kid", kid)
    }
    val payload = buildJsonObject {
        putJsonObject("claims") {
            putJsonObject("userinfo") {
                putJsonObject("openbanking_intent_id") {
                    put("value", consentId)
                    put("essential", true)
                }
            }
            putJsonObject("id_token") {
                putJsonObject("openbanking_intent_id") {
                    put("value", consentId)
                    put("essential", true)
                }
                putJsonObject("acr") {
                    put("essential", false)
                    putJsonArray("values") {
                        add(SCA_ACR)
                        add(CA_ACR)
                    }
                }
            }
        }
        put("iss", clientId)
        put("aud", audience)
        put("response_type", responseType)
        put("client_id", clientId)
        put("state", state)
        put("exp", nowEpochSeconds + REQUEST_OBJECT_TTL_SECONDS)
        put("nbf", nowEpochSeconds - NBF_SKEW_SECONDS)
        put("redirect_uri", redirectUri)
        put("nonce", nonce)
        put("scope", "openid ${scope.value}")
    }
    val requestJwt = signPs256(header, payload, signingKeyPem)

    val authorizationUrl = URLBuilder(authorizeUrl).apply {
        parameters.append("response_type", responseType)
        parameters.append("client_id", clientId)
        parameters.append("scope", "openid ${scope.value}")
        parameters.append("redirect_uri", redirectUri)
        parameters.append("state", state)
        parameters.append("nonce", nonce)
        parameters.append("request", requestJwt)
    }.buildString()

    return ConsentAuthorization(
        authorizationUrl = authorizationUrl,
        state = state,
        nonce = nonce,
    )
}
