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
import org.mifosx.openbanking.core.model.callback.ConsentStatus
import org.mifosx.openbanking.core.network.api.Aisp
import org.mifosx.openbanking.core.network.api.OAuth
import template.core.base.common.screen.ScreenState
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers [ConsentCallbackRepositoryImpl] — chiefly `validateCallback`, the security gate on HSBC's
 * redirect.
 *
 * `exchangeCode` / `pollConsentStatus` reach `clientAssertion()` and so need a signing key; that key
 * is generated per run by [TestSigningKey] rather than read from a fixture, which is what lets these
 * paths be covered at all without putting signing material in the repository.
 */
@OptIn(ExperimentalEncodingApi::class)
class ConsentCallbackRepositoryImplTest {

    private val pendingAuthStore = FakePendingAuthStore()
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    /**
     * `validateCallback` never signs anything, so the key is irrelevant to most of this class — but
     * `exchangeCode` / `pollConsentStatus` do reach `clientAssertion()`, hence a real generated one.
     */
    private suspend fun buildRepo(
        tokenJson: String = TOKEN_JSON,
        tokenStatus: HttpStatusCode = HttpStatusCode.OK,
        consentStatus: String = "Authorised",
        consentHttpStatus: HttpStatusCode = HttpStatusCode.OK,
    ): ConsentCallbackRepositoryImpl {
        val client = HttpClient(
            MockEngine { request ->
                when {
                    request.url.encodedPath.contains("oauth2/token") ->
                        respond(tokenJson, tokenStatus, jsonHeaders)

                    request.url.encodedPath.contains("account-access-consents") ->
                        respond(consentJson(consentStatus), consentHttpStatus, jsonHeaders)

                    else -> respond("{}", HttpStatusCode.OK, jsonHeaders)
                }
            },
        ) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return ConsentCallbackRepositoryImpl(
            oauth = OAuth(client, TOKEN_URL, "test-client", "test-kid", TestSigningKey.pem()),
            aisp = Aisp(client),
            pendingAuthStore = pendingAuthStore,
        )
    }

    /** Assembled from parts so no single line exceeds the 120-char limit. */
    private fun consentJson(status: String) =
        """{"Data":{"ConsentId":"cn-1","Status":"$status",""" +
            """"CreationDateTime":"","ExpirationDateTime":"","Permissions":[],""" +
            """"StatusUpdateDateTime":"","TransactionFromDateTime":"","TransactionToDateTime":""},""" +
            """"Links":{"Self":""},"Meta":{"TotalPages":1},"Risk":{}}"""

    /**
     * Builds an unsigned id_token whose payload carries [nonce]; only the payload is inspected.
     *
     * Padding is stripped on purpose — that is what a real JWT looks like (RFC 7515 §2), and the
     * strict [Base64.UrlSafe] decoder throws on it.
     */
    private fun idTokenWithNonce(nonce: String): String {
        val payload = Base64.UrlSafe.encode("""{"nonce":"$nonce"}""".encodeToByteArray()).trimEnd('=')
        return "header.$payload.signature"
    }

    private fun savePending(
        state: String = "st-1",
        nonce: String = "no-1",
        consentId: String = "cn-1",
    ) = pendingAuthStore.save(state, nonce, consentId)

    private fun callbackUrl(
        code: String? = "auth-code",
        state: String? = "st-1",
        idToken: String? = null,
        error: String? = null,
        errorDescription: String? = null,
    ): String {
        val parts = buildList {
            code?.let { add("code=$it") }
            state?.let { add("state=$it") }
            idToken?.let { add("id_token=$it") }
            error?.let { add("error=$it") }
            errorDescription?.let { add("error_description=$it") }
        }
        return "org.mifosx.openbanking://callback?" + parts.joinToString("&")
    }

    @Test
    fun `validateCallback returns Valid with the code and the stored consent id`() = runTest {
        savePending(nonce = "no-1", consentId = "cn-1")

        val result = buildRepo().validateCallback(
            callbackUrl(code = "auth-code", idToken = idTokenWithNonce("no-1")),
        )

        val valid = assertIs<ValidationResult.Valid>(result)
        assertEquals("auth-code", valid.code)
        assertEquals("cn-1", valid.consentId)
    }

    @Test
    fun `validateCallback returns SecurityError when no pending auth was stored`() = runTest {
        assertIs<ValidationResult.SecurityError>(buildRepo().validateCallback(callbackUrl()))
    }

    @Test
    fun `validateCallback returns SecurityError for state mismatch`() = runTest {
        savePending(state = "st-expected")

        assertIs<ValidationResult.SecurityError>(
            buildRepo().validateCallback(callbackUrl(state = "st-attacker")),
        )
    }

    @Test
    fun `validateCallback returns SecurityError for nonce mismatch`() = runTest {
        savePending(nonce = "no-expected")

        assertIs<ValidationResult.SecurityError>(
            buildRepo().validateCallback(callbackUrl(idToken = idTokenWithNonce("no-attacker"))),
        )
    }

    @Test
    fun `validateCallback returns SecurityError when the id_token is absent entirely`() = runTest {
        savePending()

        assertIs<ValidationResult.SecurityError>(buildRepo().validateCallback(callbackUrl(idToken = null)))
    }

    @Test
    fun `validateCallback returns SecurityError for a malformed id_token`() = runTest {
        savePending()

        assertIs<ValidationResult.SecurityError>(
            buildRepo().validateCallback(callbackUrl(idToken = "not-a-jwt")),
        )
    }

    @Test
    fun `validateCallback returns SecurityError for an id_token with undecodable payload`() = runTest {
        savePending()

        assertIs<ValidationResult.SecurityError>(
            buildRepo().validateCallback(callbackUrl(idToken = "header.!!!not-base64!!!.sig")),
        )
    }

    @Test
    fun `validateCallback returns AccessDenied when the psu declines`() = runTest {
        savePending()

        assertIs<ValidationResult.AccessDenied>(
            buildRepo().validateCallback(callbackUrl(code = null, error = "access_denied")),
        )
    }

    @Test
    fun `AccessDenied is reported even though a declined consent carries no id_token`() = runTest {
        savePending()

        val result = buildRepo().validateCallback(
            callbackUrl(code = null, idToken = null, error = "access_denied"),
        )

        assertIs<ValidationResult.AccessDenied>(
            result,
            "errors must be classified before the nonce check, or a plain refusal surfaces as a " +
                "SecurityError",
        )
    }

    @Test
    fun `validateCallback surfaces the error description for a non access_denied error`() = runTest {
        savePending()

        val result = buildRepo().validateCallback(
            callbackUrl(code = null, error = "server_error", errorDescription = "HSBC%20is%20down"),
        )

        assertEquals("HSBC is down", assertIs<ValidationResult.Error>(result).message)
    }

    @Test
    fun `validateCallback falls back to the error code when no description is given`() = runTest {
        savePending()

        val result = buildRepo().validateCallback(callbackUrl(code = null, error = "server_error"))

        assertTrue(assertIs<ValidationResult.Error>(result).message.contains("server_error"))
    }

    @Test
    fun `validateCallback returns MissingCode when the response has no code`() = runTest {
        savePending(nonce = "no-1")

        assertIs<ValidationResult.MissingCode>(
            buildRepo().validateCallback(callbackUrl(code = null, idToken = idTokenWithNonce("no-1"))),
        )
    }

    @Test
    fun `validateCallback consumes the pending auth so a replay is refused`() = runTest {
        savePending(nonce = "no-1")
        val repo = buildRepo()
        val url = callbackUrl(idToken = idTokenWithNonce("no-1"))

        assertIs<ValidationResult.Valid>(repo.validateCallback(url))
        assertIs<ValidationResult.SecurityError>(
            repo.validateCallback(url),
            "the same redirect must not validate twice",
        )
    }

    @Test
    fun `validateCallback consumes the pending auth even when validation fails`() = runTest {
        savePending(state = "st-expected")

        buildRepo().validateCallback(callbackUrl(state = "st-attacker"))

        assertNull(pendingAuthStore.consume(), "a failed attempt must not leave a reusable entry")
    }

    @Test
    fun `validateCallback reads the fragment form delivered by the mobile custom scheme`() = runTest {
        savePending(nonce = "no-1")

        val result = buildRepo().validateCallback(
            "org.mifosx.openbanking://callback#code=auth-code&state=st-1&id_token=${idTokenWithNonce("no-1")}",
        )

        assertIs<ValidationResult.Valid>(result)
    }

    @Test
    fun `an unpadded id_token validates, as every real jwt is unpadded base64url`() = runTest {
        savePending(nonce = "no-1")
        val unpadded = idTokenWithNonce("no-1")
        assertTrue(!unpadded.contains('='), "fixture must model a real JWT")

        val result = buildRepo().validateCallback(callbackUrl(idToken = unpadded))

        assertIs<ValidationResult.Valid>(
            result,
            "Base64.UrlSafe rejects unpadded input by default, which failed every genuine consent",
        )
    }

    @Test
    fun `a padded id_token still validates`() = runTest {
        savePending(nonce = "no-1")
        val padded = "header.${Base64.UrlSafe.encode("""{"nonce":"no-1"}""".encodeToByteArray())}.sig"

        assertIs<ValidationResult.Valid>(buildRepo().validateCallback(callbackUrl(idToken = padded)))
    }

    @Test
    fun `an id_token whose payload carries no nonce is refused`() = runTest {
        savePending(nonce = "no-1")
        val noNonce = "header.${Base64.UrlSafe.encode("""{"sub":"psu"}""".encodeToByteArray()).trimEnd('=')}.sig"

        assertIs<ValidationResult.SecurityError>(buildRepo().validateCallback(callbackUrl(idToken = noNonce)))
    }

    @Test
    fun `exchangeCode returns Content when the token endpoint accepts the code`() = runTest {
        assertIs<ScreenState.Content<*>>(buildRepo().exchangeCode("auth-code", REDIRECT_URI))
    }

    @Test
    fun `exchangeCode returns Error when the token endpoint rejects the code`() = runTest {
        val repo = buildRepo(
            tokenJson = """{"error":"invalid_grant"}""",
            tokenStatus = HttpStatusCode.BadRequest,
        )

        assertIs<ScreenState.Error>(repo.exchangeCode("stale-code", REDIRECT_URI))
    }

    @Test
    fun `pollConsentStatus maps an authorised consent to Content`() = runTest {
        val polled = buildRepo(consentStatus = "Authorised").pollConsentStatus("cn-1")

        assertEquals(ConsentStatus.Authorised, assertIs<ScreenState.Content<*>>(polled).data)
    }

    @Test
    fun `pollConsentStatus maps an awaiting consent to Content`() = runTest {
        val polled = buildRepo(consentStatus = "AwaitingAuthorisation").pollConsentStatus("cn-1")

        assertEquals(ConsentStatus.AwaitingAuthorisation, assertIs<ScreenState.Content<*>>(polled).data)
    }

    @Test
    fun `pollConsentStatus surfaces an unauthenticated token call rather than polling on`() = runTest {
        val repo = buildRepo(
            tokenJson = """{"error":"invalid_client"}""",
            tokenStatus = HttpStatusCode.Unauthorized,
        )

        assertIs<ScreenState.Unauthenticated>(repo.pollConsentStatus("cn-1"))
    }

    @Test
    fun `pollConsentStatus returns Error when the consent lookup fails`() = runTest {
        val repo = buildRepo(consentHttpStatus = HttpStatusCode.InternalServerError)

        assertIs<ScreenState.Error>(repo.pollConsentStatus("cn-1"))
    }

    @Test
    fun `pollConsentStatus returns Unauthenticated when the consent lookup is rejected`() = runTest {
        val repo = buildRepo(consentHttpStatus = HttpStatusCode.Unauthorized)

        assertIs<ScreenState.Unauthenticated>(repo.pollConsentStatus("cn-1"))
    }

    private companion object {
        const val TOKEN_URL = "https://sandbox.test/obie/open-banking/v1.1/oauth2/token"
        const val REDIRECT_URI = "https://thekalpeshpawar.github.io/obp-callback/callback/"
        const val TOKEN_JSON =
            """{"access_token":"tok","expires_in":300,"scope":"accounts","token_type":"Bearer"}"""
    }
}
