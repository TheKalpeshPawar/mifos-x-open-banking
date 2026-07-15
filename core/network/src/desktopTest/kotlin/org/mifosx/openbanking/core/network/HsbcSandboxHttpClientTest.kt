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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HsbcSandboxHttpClientTest {

    private val signingKey: String =
        checkNotNull(HsbcSandboxHttpClientTest::class.java.getResourceAsStream("/test-signing-key.pem"))
            .readBytes().decodeToString()
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
            signingKeyPem = signingKey,
            redirectUri = "https://cb/callback/",
            refreshToken = "old-ref",
        )

        assertEquals("new-acc", tokens.accessToken)
        assertEquals("new-ref", tokens.refreshToken)
        assertEquals(HttpMethod.Post, request?.method)
        assertTrue(request?.url?.encodedPath?.contains("oauth2/token") == true)
    }

    @Test
    fun `refreshAccessToken maps a missing access_token to an empty string`() = runTest {
        val client = mockClient { respond("{}", headers = jsonHeaders) }
        val tokens = refreshAccessToken(client, tokenUrl, "c", "k", signingKey, "https://cb", "old-ref")
        assertEquals("", tokens.accessToken)
        assertNull(tokens.refreshToken)
    }

    @Test
    fun `saveTokens and loadTokens round-trip through Settings`() {
        val settings = Settings().apply { clear() }
        assertNull(loadTokens(settings))

        saveTokens(settings, AuthTokens(accessToken = "acc", refreshToken = "ref"))
        val loaded = loadTokens(settings)

        assertEquals("acc", loaded?.accessToken)
        assertEquals("ref", loaded?.refreshToken)
        settings.clear()
    }
}
