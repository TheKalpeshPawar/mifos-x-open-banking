/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.api

import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.mifosx.openbanking.core.model.createConsent.CreateConsentTokenSuccess
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OAuthTest {

    private val signingKey: String =
        checkNotNull(OAuthTest::class.java.getResourceAsStream("/test-signing-key.pem"))
            .readBytes().decodeToString()
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    private val tokenUrl = "https://sandbox.test/obie/open-banking/v1.1/oauth2/token"

    private fun oauth(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        onRequest: (HttpRequestData) -> Unit = {},
    ): OAuth = OAuth(
        httpClient = mockClient { request -> onRequest(request); respond(body, status, jsonHeaders) },
        tokenUrl = tokenUrl,
        clientId = "client-1",
        kid = "kid-1",
        signingKeyPem = signingKey,
    )

    @Test
    fun `clientCredentialsToken posts to the token endpoint and decodes the token`() = runTest {
        var request: HttpRequestData? = null
        val result = oauth(
            body = """{"access_token":"tok","expires_in":300,"scope":"accounts","token_type":"Bearer"}""",
            onRequest = { request = it },
        ).clientCredentialsToken("accounts")

        assertIs<NetworkResult.Success<CreateConsentTokenSuccess>>(result)
        assertEquals("tok", result.data.accessToken)
        assertEquals(HttpMethod.Post, request?.method)
        assertTrue(request?.url?.encodedPath?.contains("oauth2/token") == true)
    }

    @Test
    fun `clientCredentialsToken maps 401 to Unauthorized`() = runTest {
        val result = oauth("invalid_client", HttpStatusCode.Unauthorized).clientCredentialsToken("accounts")
        assertIs<NetworkResult.Error<NetworkError>>(result)
        assertIs<NetworkError.Client.Unauthorized>(result.error)
    }

    @Test
    fun `exchangeAuthorizationCode decodes the PSU token`() = runTest {
        val result = oauth("{}").exchangeAuthorizationCode("code", "https://cb")
        assertIs<NetworkResult.Success<*>>(result)
    }

    @Test
    fun `refreshToken decodes the refreshed token`() = runTest {
        val result = oauth("{}").refreshToken("old-refresh")
        assertIs<NetworkResult.Success<*>>(result)
    }
}
