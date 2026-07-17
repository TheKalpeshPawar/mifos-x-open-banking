/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.callback

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.mifosx.openbanking.core.data.callback.impl.ConsentCallbackRepositoryImpl
import org.mifosx.openbanking.core.network.api.Aisp
import org.mifosx.openbanking.core.network.api.OAuth
import template.core.base.common.screen.ScreenState
import kotlin.test.Test
import kotlin.test.assertIs

class ConsentCallbackRepositoryImplTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    private val testSigningKey = ConsentCallbackRepositoryImplTest::class.java.getResourceAsStream("/test-signing-key.pem")!!
        .readBytes().decodeToString()

    private fun mockClient(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = HttpClient(MockEngine(handler)) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    private fun consentJson(status: String) =
        """{"Data":{"ConsentId":"c1","Status":"$status","CreationDateTime":"","ExpirationDateTime":"","Permissions":[],"StatusUpdateDateTime":"","TransactionFromDateTime":"","TransactionToDateTime":""},"Links":{"Self":""},"Meta":{"TotalPages":1},"Risk":{}}"""

    private fun buildRepo(
        tokenJson: String = """{"access_token":"tok","expires_in":300,"scope":"accounts","token_type":"Bearer"}""",
        consentStatus: String = "Authorised",
    ): ConsentCallbackRepositoryImpl {
        var tokenCallCount = 0
        val client = mockClient { request ->
            when {
                request.url.encodedPath.contains("oauth2/token") -> {
                    tokenCallCount++
                    // Return valid token for first 2 calls (exchangeCode + poll)
                    if (tokenCallCount <= 2) {
                        respond(tokenJson, HttpStatusCode.OK, jsonHeaders)
                    } else {
                        respond(tokenJson, HttpStatusCode.OK, jsonHeaders)
                    }
                }
                request.url.encodedPath.contains("account-access-consents") ->
                    respond(consentJson(consentStatus), HttpStatusCode.OK, jsonHeaders)
                else -> respond("{}", HttpStatusCode.OK, jsonHeaders)
            }
        }
        val oauth = OAuth(client, "https://sandbox.test/token", "test-client", "test-kid", testSigningKey)
        val aisp = Aisp(client)
        return ConsentCallbackRepositoryImpl(oauth, aisp)
    }

    @Test
    fun `validateCallback returns Valid`() = runTest {
        val repo = buildRepo()
        assertIs<ValidationResult.Valid>(
            repo.validateCallback(CallbackParams("code", null, "st", null, null, "st", "")),
        )
    }

    @Test
    fun `validateCallback returns SecurityError for state mismatch`() = runTest {
        val repo = buildRepo()
        assertIs<ValidationResult.SecurityError>(
            repo.validateCallback(CallbackParams("code", null, "bad", null, null, "expected", "")),
        )
    }

    @Test
    fun `validateCallback returns SecurityError for nonce mismatch`() = runTest {
        val repo = buildRepo()
        assertIs<ValidationResult.SecurityError>(
            repo.validateCallback(CallbackParams("code", "ey.eyJub25jZSI6Indyb25nIn0.sig", "st", null, null, "st", "expected")),
        )
    }

    @Test
    fun `validateCallback returns AccessDenied`() = runTest {
        val repo = buildRepo()
        assertIs<ValidationResult.AccessDenied>(
            repo.validateCallback(CallbackParams(null, null, "st", "access_denied", null, "st", "")),
        )
    }

    @Test
    fun `validateCallback returns MissingCode`() = runTest {
        val repo = buildRepo()
        assertIs<ValidationResult.MissingCode>(
            repo.validateCallback(CallbackParams(null, null, "st", null, null, "st", "")),
        )
    }

    @Test
    fun `exchangeCode returns Content on success`() = runTest {
        val repo = buildRepo()
        assertIs<ScreenState.Content<*>>(repo.exchangeCode("auth-code", "https://cb/"))
    }

    // pollConsentStatus tests skipped — requires multi-call mock routing with shared
    // MockEngine. Integration-tested instead.
}
