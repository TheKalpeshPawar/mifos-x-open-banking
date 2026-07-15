/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.result

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@Serializable
private data class Dummy(val id: String)

private fun clientReturning(status: HttpStatusCode, body: String): HttpClient =
    HttpClient(
        MockEngine {
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        },
    ) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

class ToNetworkResultTest {

    @Test
    fun `2xx decodes the body into Success`() = runTest {
        val client = clientReturning(HttpStatusCode.OK, """{"id":"abc"}""")
        val result = client.get("/x").toNetworkResult<Dummy>()
        assertIs<NetworkResult.Success<Dummy>>(result)
        assertEquals("abc", result.data.id)
    }

    @Test
    fun `2xx with undecodable body maps to Serialization`() = runTest {
        val client = clientReturning(HttpStatusCode.OK, "not-json")
        val result = client.get("/x").toNetworkResult<Dummy>()
        assertIs<NetworkResult.Error<NetworkError>>(result)
        assertIs<NetworkError.Serialization>(result.error)
    }

    @Test
    fun `401 maps to Unauthorized and captures the raw body`() = runTest {
        val client = clientReturning(HttpStatusCode.Unauthorized, """{"Code":"invalid_token"}""")
        val result = client.get("/x").toNetworkResult<Dummy>()
        assertIs<NetworkResult.Error<NetworkError>>(result)
        val error = result.error
        assertIs<NetworkError.Client.Unauthorized>(error)
        assertEquals("""{"Code":"invalid_token"}""", error.body)
    }

    @Test
    fun `403 maps to Forbidden`() = runTest {
        val client = clientReturning(HttpStatusCode.Forbidden, "consent revoked")
        val result = client.get("/x").toNetworkResult<Dummy>()
        assertIs<NetworkResult.Error<NetworkError>>(result)
        assertIs<NetworkError.Client.Forbidden>(result.error)
    }

    @Test
    fun `500 maps to Server`() = runTest {
        val client = clientReturning(HttpStatusCode.InternalServerError, "boom")
        val result = client.get("/x").toNetworkResult<Dummy>()
        assertIs<NetworkResult.Error<NetworkError>>(result)
        assertIs<NetworkError.Server>(result.error)
    }
}
