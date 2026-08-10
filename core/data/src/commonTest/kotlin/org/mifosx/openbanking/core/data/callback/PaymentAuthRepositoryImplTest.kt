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
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.mifosx.openbanking.core.data.TestSigningKey
import org.mifosx.openbanking.core.data.callback.impl.PaymentAuthRepositoryImpl
import org.mifosx.openbanking.core.model.banking.BeneficiaryScheme
import org.mifosx.openbanking.core.model.banking.payment.ConsentType
import org.mifosx.openbanking.core.model.banking.payment.CreditorSelection
import org.mifosx.openbanking.core.model.banking.payment.PaymentDraft
import org.mifosx.openbanking.core.network.api.OAuth
import org.mifosx.openbanking.core.network.api.Pisp
import org.mifosx.openbanking.core.network.model.oauth.PsuTokenResponse
import template.core.base.network.NetworkResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val CLIENT_CREDENTIALS_TOKEN = "tpp-client-credentials-token"
private const val PSU_TOKEN = "psu-payments-token"
private const val CONSENT_ID = "812774903"
private const val STATE = "payment-state-1"
private const val NONCE = "payment-nonce-1"

private const val TOKEN_JSON =
    """{"access_token":"$CLIENT_CREDENTIALS_TOKEN","expires_in":300,"scope":"payments","token_type":"Bearer"}"""
private const val CONSENT_JSON = """{"Data":{"ConsentId":"$CONSENT_ID","Status":"Authorised"}}"""

/**
 * Covers [PaymentAuthRepositoryImpl] — the return leg's data layer.
 *
 * The load-bearing case is [readingConsentStatusPresentsTheClientCredentialsToken]. Reading a
 * consent back on the PSU token returned `401` from the live sandbox *immediately after an
 * authorisation that had succeeded*, because a consent resource is TPP-authenticated: the credential
 * that created it is the one that may read it. The PSU token authorises the payment, not the consent.
 */
class PaymentAuthRepositoryImplTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private data class Recorded(val path: String, val authorization: String?)

    private val captured = mutableListOf<Recorded>()

    private suspend fun repository(
        session: PaymentAuthSession = SettingsPaymentAuthSession(MapSettings()),
    ): PaymentAuthRepositoryImpl {
        val client = HttpClient(
            MockEngine { request: HttpRequestData ->
                captured += Recorded(
                    path = request.url.encodedPath,
                    authorization = request.headers[HttpHeaders.Authorization],
                )
                val body = when {
                    request.url.encodedPath.contains("oauth2/token") -> TOKEN_JSON
                    else -> CONSENT_JSON
                }
                respond(body, HttpStatusCode.OK, jsonHeaders)
            },
        ) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val signingKey = TestSigningKey.pem()
        return PaymentAuthRepositoryImpl(
            oauth = OAuth(client, "https://sandbox.test/oauth2/token", "test-client", "test-kid", signingKey),
            pisp = Pisp(
                httpClient = client,
                kid = "test-kid",
                signingKeyPem = signingKey,
                financialId = "",
                signingIssuer = "mifos_init_00000/0000000000000000000000",
            ),
            paymentAuthSession = session,
            redirectUri = "https://cb/",
        )
    }

    private fun pendingSession(): PaymentAuthSession =
        SettingsPaymentAuthSession(MapSettings()).apply {
            savePending(consentId = CONSENT_ID, state = STATE, nonce = NONCE, type = ConsentType.DomesticSinglePayment)
            savePaymentToken(
                PsuTokenResponse(accesstoken = PSU_TOKEN, tokentype = "Bearer", expiresin = 300),
            )
        }

    /**
     * A staged draft, which is what tells the return leg which rail it is on.
     *
     * `CurrencyOfTransfer` is the discriminator the rest of the app already uses: set on
     * international drafts, absent on domestic ones.
     */
    private fun draft(currencyOfTransfer: String? = null): PaymentDraft = PaymentDraft(
        debtorAccount = null,
        creditor = CreditorSelection(
            name = "Klara Weiss",
            scheme = BeneficiaryScheme.Iban,
            identification = "DE89370400440532013000",
        ),
        amountMinorUnits = 25_000L,
        currency = "GBP",
        reference = null,
        instructionIdentification = "MFX-1",
        endToEndIdentification = "E2E-1",
        consentIdempotencyKey = "consent-key",
        paymentIdempotencyKey = "payment-key",
        currencyOfTransfer = currencyOfTransfer,
    )

    /**
     * A session staged for one consent family, recorded the way production records it.
     *
     * The type is written explicitly rather than left to be inferred: that is the whole change, and
     * a fixture that leaned on the draft would be testing the legacy fallback instead of the path
     * every new authorisation actually takes.
     */
    private fun sessionStaging(type: ConsentType): PaymentAuthSession =
        SettingsPaymentAuthSession(MapSettings()).apply {
            savePending(consentId = CONSENT_ID, state = STATE, nonce = NONCE, type = type)
            savePaymentToken(
                PsuTokenResponse(accesstoken = PSU_TOKEN, tokentype = "Bearer", expiresin = 300),
            )
            saveDraft(draft(if (type == ConsentType.InternationalSinglePayment) "USD" else null))
        }

    // region — which credential reads the consent

    /**
     * The `401`-after-success regression, pinned. A PSU token here is the wrong credential for the
     * resource, however freshly minted it is.
     */
    @Test
    fun readingConsentStatusPresentsTheClientCredentialsToken() = runTest {
        val session = pendingSession()

        val result = repository(session).consentStatus(CONSENT_ID)

        assertIs<NetworkResult.Success<String>>(result)
        val read = assertNotNull(captured.lastOrNull { it.path.contains("domestic-payment-consents") })
        assertEquals("Bearer $CLIENT_CREDENTIALS_TOKEN", read.authorization)
        assertFalse(PSU_TOKEN in read.authorization.orEmpty())
    }

    @Test
    fun consentStatusReportsWhatTheBankSaid() = runTest {
        val result = repository(pendingSession()).consentStatus(CONSENT_ID)

        assertEquals("Authorised", assertIs<NetworkResult.Success<String>>(result).data)
    }

    // endregion

    // region — reading a consent from the endpoint that issued it

    /**
     * The regression that broke every international payment.
     *
     * The read was hardcoded to the domestic path, so an international consent was looked up where
     * it does not exist and answered `400 U011 Resource cannot be found`. The return leg could never
     * observe `AUTH`, and the payment could not complete. Captured live: consent `45282` was created
     * at `international-payment-consents` and read back at `domestic-payment-consents/45282`.
     *
     * Asserted negatively as well as positively — the old code would satisfy "a request was made"
     * and only fails on "and not to the other rail's path".
     */
    @Test
    fun anInternationalConsentIsReadFromTheInternationalEndpoint() = runTest {
        repository(sessionStaging(ConsentType.InternationalSinglePayment)).consentStatus(CONSENT_ID)

        assertNotNull(captured.lastOrNull { "international-payment-consents" in it.path })
        assertNull(
            captured.lastOrNull { it.path.contains("/domestic-payment-consents") },
            "an international consent must never be read at the domestic path",
        )
    }

    @Test
    fun aDomesticConsentIsReadFromTheDomesticEndpoint() = runTest {
        repository(sessionStaging(ConsentType.DomesticSinglePayment)).consentStatus(CONSENT_ID)

        assertNotNull(captured.lastOrNull { "/domestic-payment-consents" in it.path })
        assertNull(
            captured.lastOrNull { "international-payment-consents" in it.path },
            "a domestic consent must never be read at the international path",
        )
    }

    /**
     * A session staged before the draft was persisted has no rail to read.
     *
     * Domestic is the safe default: it reproduces the behaviour such a session already had rather
     * than failing, and it matches how an unknown rail is treated for payments.
     */
    @Test
    fun aSessionWithNoStagedDraftFallsBackToDomestic() = runTest {
        repository(pendingSession()).consentStatus(CONSENT_ID)

        assertNotNull(captured.lastOrNull { "/domestic-payment-consents" in it.path })
    }

    // endregion

    // region — telling a payment redirect from a sign-in one

    /**
     * Both legs return through the same bus on the same registered redirect URI. Getting this wrong
     * would exchange a payment code into the AIS session and overwrite the account bearer with a
     * payments-scoped token, breaking every read in the app.
     */
    @Test
    fun aRedirectMatchingTheAuthorisationInFlightIsAPaymentRedirect() = runTest {
        val repository = repository(pendingSession())

        assertTrue(repository.isPaymentRedirect("https://cb/?code=abc&state=$STATE"))
    }

    @Test
    fun aRedirectCarryingSomeoneElsesStateIsNotAPaymentRedirect() = runTest {
        val repository = repository(pendingSession())

        assertFalse(repository.isPaymentRedirect("https://cb/?code=abc&state=a-different-state"))
    }

    /** With no payment authorisation in flight, every redirect belongs to the sign-in leg. */
    @Test
    fun noPaymentIsInFlightSoNoRedirectIsAPaymentRedirect() = runTest {
        val repository = repository()

        assertFalse(repository.isPaymentRedirect("https://cb/?code=abc&state=$STATE"))
    }

    // endregion

    // region — a stale callback is not a security event

    /**
     * The split that used to be one branch.
     *
     * `discardAuthorisation()` clears the session the moment a payment finishes, so a callback for a
     * payment that already went through — a restored browser tab, a tapped history entry — arrives
     * with nothing pending. That is the ordinary way to reach this, and calling it a SecurityError
     * accused the customer of tampering for reopening their own link.
     */
    @Test
    fun aCallbackWithNothingInFlightIsReportedAsNoPending() = runTest {
        val result = repository().validateCallback("https://cb/?code=abc&state=$STATE")

        assertEquals(PaymentAuthValidation.NoPending, result)
    }

    /**
     * The other half of the split, and the one that really is a replay signal: a `state` was issued
     * for an authorisation in flight, and the one that came back is not it.
     */
    @Test
    fun aCallbackCarryingTheWrongStateIsStillASecurityError() = runTest {
        val repository = repository(pendingSession())

        val result = repository.validateCallback("https://cb/?code=abc&state=someone-elses-state")

        assertEquals(PaymentAuthValidation.SecurityError, result)
    }

    // endregion

    // region — discarding the authorisation

    /**
     * Called at each of the three points a payment stops being in flight. Leaving the PSU token
     * behind would let a later payment submit on a credential its own authorisation never issued.
     */
    @Test
    fun discardingForgetsTheTokensTheConsentAndTheDraft() = runTest {
        val session = pendingSession()

        repository(session).discardAuthorisation()

        assertNull(session.pendingConsentId())
        assertNull(session.paymentToken())
        assertNull(session.draft())
        assertFalse(session.matchesPendingState(STATE))
    }

    // endregion
}
