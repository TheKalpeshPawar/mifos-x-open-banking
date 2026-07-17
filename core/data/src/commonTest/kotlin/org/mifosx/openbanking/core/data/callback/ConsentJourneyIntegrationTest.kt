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

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.mifosx.openbanking.core.data.TestSigningKey
import org.mifosx.openbanking.core.data.callback.impl.ConsentCallbackRepositoryImpl
import org.mifosx.openbanking.core.data.login.ConsentResult
import org.mifosx.openbanking.core.data.login.impl.LoginRepositoryImpl
import org.mifosx.openbanking.core.model.callback.ConsentStatus
import org.mifosx.openbanking.core.network.api.Aisp
import org.mifosx.openbanking.core.network.api.OAuth
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkResult
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/**
 * Drives the whole consent journey across the real seam, with only the network faked.
 *
 * The unit tests each pin one component against a fake neighbour. This one wires the real
 * [LoginRepositoryImpl], real [SettingsPendingAuthStore] and real [ConsentCallbackRepositoryImpl]
 * together and feeds the callback the `state`/`nonce` that login actually minted — the join no unit
 * test can check: whether the values one side produces are the values the other side accepts.
 */
@OptIn(ExperimentalEncodingApi::class)
class ConsentJourneyIntegrationTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private val settings = MapSettings()
    private var now = 10_000L
    private val pendingAuthStore = SettingsPendingAuthStore(settings) { now }

    private fun client(consentStatus: String = "AUTH") = HttpClient(
        MockEngine { request ->
            when {
                request.url.encodedPath.contains("oauth2/token") ->
                    respond(TOKEN_JSON, HttpStatusCode.OK, jsonHeaders)

                request.url.encodedPath.contains("account-access-consents") ->
                    respond(consentJson(consentStatus), HttpStatusCode.Created, jsonHeaders)

                else -> respond("{}", HttpStatusCode.OK, jsonHeaders)
            }
        },
    ) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    /** Assembled from parts so no single line exceeds the 120-char limit. */
    private fun consentJson(status: String) =
        """{"Data":{"ConsentId":"$CONSENT_ID","Status":"$status",""" +
            """"CreationDateTime":"","ExpirationDateTime":"","Permissions":[],""" +
            """"StatusUpdateDateTime":"","TransactionFromDateTime":"","TransactionToDateTime":""},""" +
            """"Links":{"Self":""},"Meta":{"TotalPages":1},"Risk":{}}"""

    private suspend fun loginRepository(client: HttpClient) = LoginRepositoryImpl(
        oauth = OAuth(client, TOKEN_URL, "test-client", "test-kid", TestSigningKey.pem()),
        aisp = Aisp(client),
        signingKeyPem = TestSigningKey.pem(),
        clientId = "test-client",
        kid = "test-kid",
        bankHost = "sandbox.test",
        authorizeHost = "authorize.sandbox.test",
        redirectUri = REDIRECT_URI,
    )

    private suspend fun callbackRepository(client: HttpClient) = ConsentCallbackRepositoryImpl(
        oauth = OAuth(client, TOKEN_URL, "test-client", "test-kid", TestSigningKey.pem()),
        aisp = Aisp(client),
        pendingAuthStore = pendingAuthStore,
    )

    private suspend fun mintConsent(client: HttpClient): ConsentResult {
        val result = loginRepository(client).createConsentAndBuildAuthorizationUrl()
        return assertIs<NetworkResult.Success<ConsentResult>>(result).data
    }

    /** Models what HSBC sends back: an unpadded base64url JWT carrying the nonce it was given. */
    private fun idTokenFor(nonce: String): String {
        val payload = Base64.UrlSafe.encode("""{"nonce":"$nonce"}""".encodeToByteArray()).trimEnd('=')
        return "header.$payload.signature"
    }

    /** The redirect the bridge page relays into the app, rewritten fragment → query. */
    private fun redirectFor(state: String, nonce: String) =
        "org.mifosx.openbanking://callback?code=$AUTH_CODE&id_token=${idTokenFor(nonce)}&state=$state"

    @Test
    fun `login through callback completes end to end`() = runTest {
        val client = client(consentStatus = "AUTH")
        val minted = mintConsent(client)

        pendingAuthStore.save(minted.state, minted.nonce, minted.consentId)

        val callback = callbackRepository(client)
        val validation = callback.validateCallback(redirectFor(minted.state, minted.nonce))

        val valid = assertIs<ValidationResult.Valid>(
            validation,
            "the state/nonce login minted must be exactly what the callback accepts",
        )
        assertEquals(AUTH_CODE, valid.code)
        assertEquals(CONSENT_ID, valid.consentId)

        assertIs<ScreenState.Content<*>>(callback.exchangeCode(valid.code, REDIRECT_URI))

        val polled = assertIs<ScreenState.Content<*>>(callback.pollConsentStatus(valid.consentId))
        assertEquals(ConsentStatus.Authorised, polled.data)
    }

    @Test
    fun `a redirect replayed after a completed journey is refused`() = runTest {
        val client = client()
        val minted = mintConsent(client)
        pendingAuthStore.save(minted.state, minted.nonce, minted.consentId)
        val callback = callbackRepository(client)
        val redirect = redirectFor(minted.state, minted.nonce)

        assertIs<ValidationResult.Valid>(callback.validateCallback(redirect))

        assertIs<ValidationResult.SecurityError>(
            callback.validateCallback(redirect),
            "the pending auth is single-use, so a captured redirect cannot be re-submitted",
        )
    }

    @Test
    fun `a redirect carrying another session's state is refused`() = runTest {
        val client = client()
        val minted = mintConsent(client)
        pendingAuthStore.save(minted.state, minted.nonce, minted.consentId)

        val validation = callbackRepository(client)
            .validateCallback(redirectFor("attacker-state", minted.nonce))

        assertIs<ValidationResult.SecurityError>(validation)
    }

    @Test
    fun `a redirect arriving after the request object expired is refused`() = runTest {
        val client = client()
        val minted = mintConsent(client)
        pendingAuthStore.save(minted.state, minted.nonce, minted.consentId)

        now += SettingsPendingAuthStore.TTL_SECONDS + 1
        val validation = callbackRepository(client)
            .validateCallback(redirectFor(minted.state, minted.nonce))

        assertIs<ValidationResult.SecurityError>(validation)
        assertNull(pendingAuthStore.consume())
    }

    @Test
    fun `each login mints a fresh state and nonce`() = runTest {
        val client = client()

        val first = mintConsent(client)
        val second = mintConsent(client)

        assertNotEquals(first.state, second.state, "a reused state would defeat CSRF protection")
        assertNotEquals(first.nonce, second.nonce, "a reused nonce would defeat replay protection")
    }

    @Test
    fun `an awaiting consent surfaces as AwaitingAuthorisation rather than completing`() = runTest {
        val client = client(consentStatus = "AWAU")
        val minted = mintConsent(client)
        pendingAuthStore.save(minted.state, minted.nonce, minted.consentId)
        val callback = callbackRepository(client)
        val valid = assertIs<ValidationResult.Valid>(
            callback.validateCallback(redirectFor(minted.state, minted.nonce)),
        )

        val polled = assertIs<ScreenState.Content<*>>(callback.pollConsentStatus(valid.consentId))

        assertEquals(ConsentStatus.AwaitingAuthorisation, polled.data)
    }

    private companion object {
        const val TOKEN_URL = "https://sandbox.test/obie/open-banking/v1.1/oauth2/token"
        const val REDIRECT_URI = "https://thekalpeshpawar.github.io/obp-callback/callback/"
        const val CONSENT_ID = "consent-123"
        const val AUTH_CODE = "auth-code-xyz"
        const val TOKEN_JSON =
            """{"access_token":"tok","expires_in":300,"scope":"accounts","token_type":"Bearer"}"""
    }
}
