/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network

import com.russhwolf.settings.Settings
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.mifosx.openbanking.core.network.api.mockClient
import org.mifosx.openbanking.core.network.model.oauth.PsuTokenResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HsbcSandboxHttpClientTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    private val tokenUrl = "https://sandbox.test/obie/open-banking/v1.1/oauth2/token"

    @Test
    fun `refreshAccessToken posts the refresh_token grant and maps the new tokens`() = runTest {
        var request: HttpRequestData? = null
        val client = mockClient { req ->
            request = req
            respond(
                """{"access_token":"new-acc","refresh_token":"new-ref","expires_in":300,"token_type":"Bearer"}""",
                headers = jsonHeaders,
            )
        }

        val tokens = refreshAccessToken(
            httpClient = client,
            tokenUrl = tokenUrl,
            clientId = "client-1",
            kid = "kid-1",
            signingKeyPem = TestSigningKey.pem(),
            redirectUri = "https://cb/callback/",
            refreshToken = "old-ref",
        )

        assertEquals("new-acc", tokens.accesstoken)
        assertEquals("new-ref", tokens.refreshtoken)
        assertEquals(HttpMethod.Post, request?.method)
        assertTrue(request?.url?.encodedPath?.contains("oauth2/token") == true)
    }

    @Test
    fun `refreshAccessToken maps a missing access_token to null and reuses the sent refresh token`() = runTest {
        val client = mockClient { respond("{}", headers = jsonHeaders) }
        val tokens = refreshAccessToken(client, tokenUrl, "c", "k", TestSigningKey.pem(), "https://cb", "old-ref")
        assertNull(tokens.accesstoken)
        assertEquals("old-ref", tokens.refreshtoken)
    }

    @Test
    fun `savePsuTokens and loadPsuTokens round-trip through Settings`() {
        val settings = Settings().apply { clear() }
        assertNull(loadPsuTokens(settings))

        savePsuTokens(settings, PsuTokenResponse(accesstoken = "acc", refreshtoken = "ref"))
        val loaded = loadPsuTokens(settings)

        assertEquals("acc", loaded?.accesstoken)
        assertEquals("ref", loaded?.refreshtoken)
        settings.clear()
    }
}
