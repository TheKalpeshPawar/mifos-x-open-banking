/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.login

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
import org.mifosx.openbanking.core.data.login.impl.LoginRepositoryImpl
import org.mifosx.openbanking.core.network.api.Aisp
import org.mifosx.openbanking.core.network.api.OAuth
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LoginRepositoryImplTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    private val testSigningKey = LoginRepositoryImplTest::class.java.getResourceAsStream("/test-signing-key.pem")!!
        .readBytes().decodeToString()

    private fun mockClient(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = HttpClient(MockEngine(handler)) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    private fun buildRepo(
        tokenJson: String = """{"access_token":"tok","expires_in":300,"scope":"accounts","token_type":"Bearer"}""",
        consentJson: String = """{"Data":{"ConsentId":"consent-123","Status":"AWAU","CreationDateTime":"","ExpirationDateTime":"","Permissions":[],"StatusUpdateDateTime":"","TransactionFromDateTime":"","TransactionToDateTime":""},"Links":{"Self":""},"Meta":{"TotalPages":1},"Risk":{}}""",
    ): LoginRepositoryImpl {
        val client = mockClient { request ->
            when {
                request.url.encodedPath.contains("oauth2/token") -> respond(tokenJson, HttpStatusCode.OK, jsonHeaders)
                request.url.encodedPath.contains("account-access-consents") -> respond(consentJson, HttpStatusCode.Created, jsonHeaders)
                else -> respond("{}", HttpStatusCode.OK, jsonHeaders)
            }
        }
        val oauth = OAuth(client, "https://sandbox.test/obie/open-banking/v1.1/oauth2/token", "test-client", "test-kid", testSigningKey)
        val aisp = Aisp(client)
        return LoginRepositoryImpl(oauth, aisp, testSigningKey, "test-client", "test-kid", "sandbox.test", "https://cb/")
    }

    @Test
    fun `getPermissions returns 21 items`() = runTest {
        assertEquals(21, buildRepo().getPermissions().size)
    }

    @Test
    fun `createConsentAndBuildAuthorizationUrl returns Success`() = runTest {
        val result = buildRepo().createConsentAndBuildAuthorizationUrl()
        assertIs<NetworkResult.Success<ConsentResult>>(result)
        assertEquals("consent-123", result.data.consentId)
        assertTrue(result.data.authorizationUrl.contains("authorize"))
    }

    @Test
    fun `createConsentAndBuildAuthorizationUrl returns Error on token failure`() = runTest {
        val repo = buildRepo(tokenJson = """{"error":"invalid_client"}""")
        assertIs<NetworkResult.Error<NetworkError>>(repo.createConsentAndBuildAuthorizationUrl())
    }

    @Test
    fun `createConsentAndBuildAuthorizationUrl returns Error on consent failure`() = runTest {
        val repo = buildRepo(consentJson = """{"Code":"400"}""")
        assertIs<NetworkResult.Error<NetworkError>>(repo.createConsentAndBuildAuthorizationUrl())
    }
}
