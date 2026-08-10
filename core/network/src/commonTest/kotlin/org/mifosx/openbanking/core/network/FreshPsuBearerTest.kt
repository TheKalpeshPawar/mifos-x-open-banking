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

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.mifosx.openbanking.core.network.model.oauth.PsuTokenResponse
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers [installFreshPsuBearer]: the AIS bearer is read fresh from storage on every request (no
 * in-memory cache to go stale — the first-login 403 U008 fix), excluded endpoints are never given
 * the PSU token, and a 401 refreshes the token and retries the request exactly once.
 */
private const val BANK_HOST = "bank.test"

class FreshPsuBearerTest {

    @Test
    fun `attaches the psu token read fresh from storage on every request`() = runTest {
        val settings = MapSettings()
        val seen = mutableListOf<String?>()
        val client = HttpClient(
            MockEngine { request ->
                seen += request.headers[HttpHeaders.Authorization]
                respond("", HttpStatusCode.OK)
            },
        )
        client.installFreshPsuBearer(settings, BANK_HOST) { null }

        // Persisted AFTER the client was built — the exact case the cached bearer plugin missed.
        savePsuTokens(settings, PsuTokenResponse(accesstoken = "access-1"))
        client.get("https://$BANK_HOST/aisp/accounts")

        // A later change is picked up immediately, proving there is no cached value.
        savePsuTokens(settings, PsuTokenResponse(accesstoken = "access-2"))
        client.get("https://$BANK_HOST/aisp/accounts")

        assertEquals(listOf<String?>("Bearer access-1", "Bearer access-2"), seen)
    }

    @Test
    fun `never attaches the psu token to the token or consent endpoints`() = runTest {
        val settings = MapSettings()
        savePsuTokens(settings, PsuTokenResponse(accesstoken = "access-1"))
        val seen = mutableListOf<String?>()
        val client = HttpClient(
            MockEngine { request ->
                seen += request.headers[HttpHeaders.Authorization]
                respond("", HttpStatusCode.OK)
            },
        )
        client.installFreshPsuBearer(settings, BANK_HOST) { null }

        client.get("https://$BANK_HOST/v1.1/oauth2/token")
        client.get("https://$BANK_HOST/aisp/account-access-consents/123")

        assertEquals(listOf<String?>(null, null), seen)
    }

    /**
     * Every PISP call supplies its own credential — client-credentials with `scope=payments` to
     * stage, the PSU payments token to submit — so the interceptor must leave the header alone.
     * Attaching the AIS bearer here would replace a correct token with one minted for a different
     * scope, and the resulting 401 would read as an auth fault rather than a scope fault.
     */
    @Test
    fun `never attaches the psu token to a pisp write`() = runTest {
        val settings = MapSettings()
        savePsuTokens(settings, PsuTokenResponse(accesstoken = "ais-access"))
        val seen = mutableListOf<String?>()
        val client = HttpClient(
            MockEngine { request ->
                seen += request.headers[HttpHeaders.Authorization]
                respond("", HttpStatusCode.OK)
            },
        )
        client.installFreshPsuBearer(settings, BANK_HOST) { null }

        client.get("https://$BANK_HOST/v4.0/pisp/domestic-payment-consents")
        client.get("https://$BANK_HOST/v4.0/pisp/domestic-payments")

        assertEquals(listOf<String?>(null, null), seen)
    }

    @Test
    fun `a 401 refreshes the token and retries the request once`() = runTest {
        val settings = MapSettings()
        savePsuTokens(settings, PsuTokenResponse(accesstoken = "stale", refreshtoken = "refresh-1"))
        var refreshCalls = 0
        val seen = mutableListOf<String?>()
        val client = HttpClient(
            MockEngine { request ->
                val auth = request.headers[HttpHeaders.Authorization]
                seen += auth
                if (auth == "Bearer stale") respond("", HttpStatusCode.Unauthorized) else respond("", HttpStatusCode.OK)
            },
        )
        client.installFreshPsuBearer(settings, BANK_HOST) { refreshToken ->
            refreshCalls++
            PsuTokenResponse(accesstoken = "fresh", refreshtoken = refreshToken)
        }

        val response = client.get("https://$BANK_HOST/aisp/accounts")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(1, refreshCalls)
        assertEquals(listOf<String?>("Bearer stale", "Bearer fresh"), seen)
        assertEquals("fresh", loadPsuTokens(settings)?.accesstoken)
    }

    @Test
    fun `a 401 with no refresh token is not retried`() = runTest {
        val settings = MapSettings()
        savePsuTokens(settings, PsuTokenResponse(accesstoken = "stale"))
        var refreshCalls = 0
        var engineCalls = 0
        val client = HttpClient(
            MockEngine {
                engineCalls++
                respond("", HttpStatusCode.Unauthorized)
            },
        )
        client.installFreshPsuBearer(settings, BANK_HOST) {
            refreshCalls++
            PsuTokenResponse(accesstoken = "fresh")
        }

        val response = client.get("https://$BANK_HOST/aisp/accounts")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(0, refreshCalls)
        assertEquals(1, engineCalls)
    }
}
