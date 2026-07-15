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

import io.ktor.http.Url
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalEncodingApi::class)
class ConsentAuthorizationTest {

    private val signingKey: String =
        checkNotNull(ConsentAuthorizationTest::class.java.getResourceAsStream("/test-signing-key.pem"))
            .readBytes().decodeToString()

    private val base64Url = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

    private suspend fun authorize() = initiateConsentAuthorization(
        authorizeUrl = "https://sandbox.test/obie/open-banking/v1.1/oauth2/authorize",
        audience = "https://secure.sandbox.test",
        clientId = "client-1",
        kid = "kid-1",
        scope = "openid accounts",
        responseType = "code id_token",
        redirectUri = "https://cb/callback/",
        consentId = "consent-123",
        signingKeyPem = signingKey,
        nowEpochSeconds = 1_000_000L,
    )

    @Test
    fun `authorize URL carries the OIDC hybrid params, state and nonce, and no PKCE`() = runTest {
        val result = authorize()
        val params = Url(result.authorizationUrl).parameters

        assertEquals("code id_token", params["response_type"])
        assertEquals("client-1", params["client_id"])
        assertEquals("openid accounts", params["scope"])
        assertEquals("https://cb/callback/", params["redirect_uri"])
        assertEquals(result.state, params["state"])
        assertEquals(result.nonce, params["nonce"])
        assertNotNull(params["request"])
        // HSBC does not use PKCE — these must never be emitted.
        assertNull(params["code_challenge"])
        assertNull(params["code_challenge_method"])
    }

    @Test
    fun `request object JWT is PS256, binds the consent, and carries exp nbf without PKCE`() = runTest {
        val result = authorize()
        val jwt = Url(result.authorizationUrl).parameters["request"]!!
        val parts = jwt.split(".")
        assertEquals(3, parts.size)

        val json = Json { ignoreUnknownKeys = true }
        val header = json.parseToJsonElement(base64Url.decode(parts[0]).decodeToString()).jsonObject
        val payload = json.parseToJsonElement(base64Url.decode(parts[1]).decodeToString()).jsonObject

        // Header: {alg, kid} only — no typ (matches gen-request-jwt.js).
        assertEquals("PS256", header["alg"]?.jsonPrimitive?.content)
        assertEquals("kid-1", header["kid"]?.jsonPrimitive?.content)
        assertNull(header["typ"])

        assertEquals("client-1", payload["iss"]?.jsonPrimitive?.content)
        assertEquals("https://secure.sandbox.test", payload["aud"]?.jsonPrimitive?.content)
        assertEquals("code id_token", payload["response_type"]?.jsonPrimitive?.content)
        assertEquals("client-1", payload["client_id"]?.jsonPrimitive?.content)
        assertEquals(result.state, payload["state"]?.jsonPrimitive?.content)
        assertEquals(result.nonce, payload["nonce"]?.jsonPrimitive?.content)
        assertEquals("openid accounts", payload["scope"]?.jsonPrimitive?.content)
        assertEquals("https://cb/callback/", payload["redirect_uri"]?.jsonPrimitive?.content)
        assertEquals("1001200", payload["exp"]?.jsonPrimitive?.content)
        assertEquals("999999", payload["nbf"]?.jsonPrimitive?.content)
        assertNull(payload["code_challenge"])
        assertNull(payload["max_age"])

        val intentId = payload["claims"]?.jsonObject
            ?.get("userinfo")?.jsonObject
            ?.get("openbanking_intent_id")?.jsonObject
            ?.get("value")?.jsonPrimitive?.content
        assertEquals("consent-123", intentId)
    }
}
