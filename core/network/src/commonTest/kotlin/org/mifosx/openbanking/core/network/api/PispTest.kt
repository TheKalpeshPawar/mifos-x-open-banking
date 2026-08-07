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

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.mifosx.openbanking.core.network.TestSigningKey
import org.mifosx.openbanking.core.network.model.pisp.domesticPayment.request.CreditorAccount
import org.mifosx.openbanking.core.network.model.pisp.domesticPayment.request.Data
import org.mifosx.openbanking.core.network.model.pisp.domesticPayment.request.DebtorAccount
import org.mifosx.openbanking.core.network.model.pisp.domesticPayment.request.DomesticPaymentConsentRequest
import org.mifosx.openbanking.core.network.model.pisp.domesticPayment.request.DomesticPaymentRequest
import org.mifosx.openbanking.core.network.model.pisp.domesticPayment.request.Initiation
import org.mifosx.openbanking.core.network.model.pisp.domesticPayment.request.InstructedAmount
import org.mifosx.openbanking.core.network.model.pisp.domesticPayment.request.RemittanceInformation
import org.mifosx.openbanking.core.network.model.pisp.domesticPayment.request.Risk
import org.mifosx.openbanking.core.network.model.pisp.internationalPayment.request.InternationalPaymentConsentRequest
import org.mifosx.openbanking.core.network.model.pisp.internationalPayment.request.InternationalPaymentRequest
import org.mifosx.openbanking.core.network.pisp.HEADER_FAPI_FINANCIAL_ID
import org.mifosx.openbanking.core.network.pisp.HEADER_FAPI_INTERACTION_ID
import org.mifosx.openbanking.core.network.pisp.HEADER_IDEMPOTENCY_KEY
import org.mifosx.openbanking.core.network.pisp.HEADER_JWS_SIGNATURE
import org.mifosx.openbanking.core.network.pisp.obieBody
import template.core.base.network.NetworkResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.mifosx.openbanking.core.network.model.pisp.internationalPayment.request.CreditorAccount as IntlCreditorAccount
import org.mifosx.openbanking.core.network.model.pisp.internationalPayment.request.Data as IntlData
import org.mifosx.openbanking.core.network.model.pisp.internationalPayment.request.DebtorAccount as IntlDebtorAccount
import org.mifosx.openbanking.core.network.model.pisp.internationalPayment.request.Initiation as IntlInitiation
import org.mifosx.openbanking.core.network.model.pisp.internationalPayment.request.InstructedAmount as IntlInstructedAmount
import org.mifosx.openbanking.core.network.model.pisp.internationalPayment.request.Risk as IntlRisk

private const val KID = "test-kid-1"
private const val SIGNING_ISSUER = "mifos_init_00000/0000000000000000000000"
private const val IDEMPOTENCY_KEY = "MFX-20260805-0001"
private const val CONSENT_ID = "812774903"
private const val SUBMITTED_PAYMENT_BODY =
    """{"Data":{"DomesticPaymentId":"PMT-1","Status":"AcceptedSettlementInProcess"}}"""
private const val INTL_CONSENT_RESPONSE =
    """{"Data":{"ConsentId":"45076","Status":"AWAU","CreationDateTime":"2026-08-06T08:50:59+00:00"}}"""
private const val INTL_CONSENT_WITH_CHARGES_RESPONSE =
    """{"Data":{"ConsentId":"99","Status":"AWAU","Charges":[{"ChargeBearer":"BorneByDebtor",""" +
        """"Type":"UK.OBIE.SWIFTCharge","Amount":{"Amount":"5.00","Currency":"GBP"}}]}}"""

/**
 * Covers [Pisp] at the wire: paths, the credential each call carries, the FAPI + write headers, and
 * the consumer-shaped `Risk` block.
 *
 * The recurring theme is that this class must not send a merchant-shaped body and must not let the
 * transmitted bytes drift from the bytes that were signed.
 */
class PispTest {

    private val captured = mutableListOf<RecordedRequest>()

    private data class RecordedRequest(
        val method: HttpMethod,
        val path: String,
        val headers: Map<String, String?>,
        val body: String,
    )

    private suspend fun pisp(
        financialId: String = "",
        responseBody: String = "{}",
        status: HttpStatusCode = HttpStatusCode.Created,
    ): Pisp {
        val client = HttpClient(
            MockEngine { request ->
                captured += RecordedRequest(
                    method = request.method,
                    path = request.url.encodedPath,
                    headers = listOf(
                        HttpHeaders.Authorization,
                        HEADER_IDEMPOTENCY_KEY,
                        HEADER_JWS_SIGNATURE,
                        HEADER_FAPI_INTERACTION_ID,
                        HEADER_FAPI_FINANCIAL_ID,
                    ).associateWith { request.headers[it] },
                    body = request.body.toByteArray().decodeToString(),
                )
                respond(
                    content = responseBody,
                    status = status,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        ) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return Pisp(
            httpClient = client,
            kid = KID,
            signingKeyPem = TestSigningKey.pem(),
            financialId = financialId,
            signingIssuer = SIGNING_ISSUER,
        )
    }

    private fun consumerConsentRequest(): DomesticPaymentConsentRequest = DomesticPaymentConsentRequest(
        data = Data(
            initiation = Initiation(
                instructionIdentification = "MFX20260805T1042330001",
                endToEndIdentification = "E2E-RENT-FLAT12-202608",
                instructedAmount = InstructedAmount(amount = "850.00", currency = "GBP"),
                debtorAccount = DebtorAccount(
                    schemeName = "UK.OBIE.SortCodeAccountNumber",
                    identification = "80200110203349",
                    name = "Mr Nico",
                ),
                creditorAccount = CreditorAccount(
                    schemeName = "UK.OBIE.SortCodeAccountNumber",
                    identification = "40120965872310",
                    name = "Jameson Lettings",
                ),
                remittanceInformation = RemittanceInformation(unstructured = listOf("RENT-FLAT12")),
            ),
        ),
        risk = Risk(paymentContextCode = "TransferToThirdParty"),
    )

    @Test
    fun `stages a consent on the v4 pisp path with the payments scope token`() = runTest {
        val pisp = pisp(responseBody = """{"Data":{"ConsentId":"$CONSENT_ID","Status":"AWAU"}}""")

        val result = pisp.createDomesticPaymentConsent(
            paymentsScopeToken = "cc-payments-token",
            request = consumerConsentRequest(),
            idempotencyKey = IDEMPOTENCY_KEY,
        )

        assertIs<NetworkResult.Success<*>>(result)
        val request = captured.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/v4.0/pisp/domestic-payment-consents", request.path)
        assertEquals("Bearer cc-payments-token", request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun `write calls carry the idempotency key the jws signature and an interaction id`() = runTest {
        val pisp = pisp()

        pisp.createDomesticPaymentConsent(
            paymentsScopeToken = "cc-payments-token",
            request = consumerConsentRequest(),
            idempotencyKey = IDEMPOTENCY_KEY,
        )

        val headers = captured.single().headers
        assertEquals(IDEMPOTENCY_KEY, headers[HEADER_IDEMPOTENCY_KEY])
        assertNotNull(headers[HEADER_FAPI_INTERACTION_ID])
        val signature = assertNotNull(headers[HEADER_JWS_SIGNATURE])
        assertEquals("", signature.split(".")[1])
    }

    /**
     * The body must be the canonical serialization of the request, because that same object is what
     * the detached JWS is computed over. Any re-serialisation between signing and sending would
     * invalidate the signature.
     */
    @Test
    fun `transmits the exact bytes that were signed`() = runTest {
        val request = consumerConsentRequest()
        val pisp = pisp()

        pisp.createDomesticPaymentConsent(
            paymentsScopeToken = "cc-payments-token",
            request = request,
            idempotencyKey = IDEMPOTENCY_KEY,
        )

        assertEquals(
            obieBody(DomesticPaymentConsentRequest.serializer(), request).toString(),
            captured.single().body,
        )
    }

    /**
     * TC-SEND-005. HSBC's own Postman example sends `MerchantCategoryCode`,
     * `MerchantCustomerIdentification` and `DeliveryAddress` — that is the merchant shape, and it is
     * wrong for a consumer app. `explicitNulls = false` is what keeps them off the wire.
     */
    @Test
    fun `consumer risk block omits every merchant field`() = runTest {
        val pisp = pisp()

        pisp.createDomesticPaymentConsent(
            paymentsScopeToken = "cc-payments-token",
            request = consumerConsentRequest(),
            idempotencyKey = IDEMPOTENCY_KEY,
        )

        val risk = Json.parseToJsonElement(captured.single().body).jsonObject.getValue("Risk").jsonObject
        assertEquals(setOf("PaymentContextCode"), risk.keys)
        assertTrue("MerchantCategoryCode" !in risk.keys)
        assertTrue("MerchantCustomerIdentification" !in risk.keys)
        assertTrue("DeliveryAddress" !in risk.keys)
    }

    @Test
    fun `omits the financial id header while no value is configured`() = runTest {
        val pisp = pisp(financialId = "")

        pisp.createDomesticPaymentConsent(
            paymentsScopeToken = "cc-payments-token",
            request = consumerConsentRequest(),
            idempotencyKey = IDEMPOTENCY_KEY,
        )

        assertNull(captured.single().headers[HEADER_FAPI_FINANCIAL_ID])
    }

    @Test
    fun `sends the financial id header once configured`() = runTest {
        val pisp = pisp(financialId = "0015800000jf8aAAAQ")

        pisp.createDomesticPaymentConsent(
            paymentsScopeToken = "cc-payments-token",
            request = consumerConsentRequest(),
            idempotencyKey = IDEMPOTENCY_KEY,
        )

        assertEquals("0015800000jf8aAAAQ", captured.single().headers[HEADER_FAPI_FINANCIAL_ID])
    }

    @Test
    fun `submits a payment against the staged consent on the psu token`() = runTest {
        val staged = consumerConsentRequest().data?.initiation
        val pisp = pisp(responseBody = SUBMITTED_PAYMENT_BODY)

        val result = pisp.createDomesticPayment(
            psuAccessToken = "psu-payments-token",
            request = DomesticPaymentRequest(
                data = Data(consentId = CONSENT_ID, initiation = staged),
                risk = Risk(paymentContextCode = "TransferToThirdParty"),
            ),
            idempotencyKey = IDEMPOTENCY_KEY,
        )

        assertIs<NetworkResult.Success<*>>(result)
        val request = captured.single()
        assertEquals("/v4.0/pisp/domestic-payments", request.path)
        assertEquals("Bearer psu-payments-token", request.headers[HttpHeaders.Authorization])
        assertEquals(IDEMPOTENCY_KEY, request.headers[HEADER_IDEMPOTENCY_KEY])
    }

    /**
     * TC-SEND-010's network half: replaying the staged `Initiation` object reproduces byte-identical
     * `Data.Initiation`, which is what keeps a retry from being refused with `U008`.
     */
    @Test
    fun `replaying the staged initiation reproduces identical bytes`() = runTest {
        val staged = consumerConsentRequest().data?.initiation
        val submit = DomesticPaymentRequest(
            data = Data(consentId = CONSENT_ID, initiation = staged),
            risk = Risk(paymentContextCode = "TransferToThirdParty"),
        )
        val pisp = pisp()

        pisp.createDomesticPayment("psu-payments-token", submit, IDEMPOTENCY_KEY)
        pisp.createDomesticPayment("psu-payments-token", submit, IDEMPOTENCY_KEY)

        assertEquals(captured[0].body, captured[1].body)
        assertEquals(captured[0].headers[HEADER_IDEMPOTENCY_KEY], captured[1].headers[HEADER_IDEMPOTENCY_KEY])
    }

    @Test
    fun `reads back a funds confirmation on the psu token without write headers`() = runTest {
        val pisp = pisp(
            responseBody = """{"Data":{"FundsAvailableResult":{"FundsAvailable":true}}}""",
            status = HttpStatusCode.OK,
        )

        val result = pisp.getFundsConfirmation(
            psuAccessToken = "psu-payments-token",
            consentId = CONSENT_ID,
        )

        assertIs<NetworkResult.Success<*>>(result)
        val request = captured.single()
        assertEquals(HttpMethod.Get, request.method)
        assertEquals("/v4.0/pisp/domestic-payment-consents/$CONSENT_ID/funds-confirmation", request.path)
        assertNull(request.headers[HEADER_JWS_SIGNATURE])
        assertNull(request.headers[HEADER_IDEMPOTENCY_KEY])
    }

    @Test
    fun `reads back a submitted payment status`() = runTest {
        val pisp = pisp(
            responseBody = SUBMITTED_PAYMENT_BODY,
            status = HttpStatusCode.OK,
        )

        val result = pisp.getDomesticPayment(accessToken = "payments-token", domesticPaymentId = "PMT-1")

        assertIs<NetworkResult.Success<*>>(result)
        assertEquals("/v4.0/pisp/domestic-payments/PMT-1", captured.single().path)
    }

    @Test
    fun `refuses an idempotency key longer than the obie limit`() = runTest {
        val pisp = pisp()

        assertFailsWith<IllegalArgumentException> {
            pisp.createDomesticPaymentConsent(
                paymentsScopeToken = "cc-payments-token",
                request = consumerConsentRequest(),
                idempotencyKey = "x".repeat(41),
            )
        }
    }

    // region — International payment endpoints

    private fun internationalConsentRequest(): InternationalPaymentConsentRequest =
        InternationalPaymentConsentRequest(
            data = IntlData(
                initiation = IntlInitiation(
                    instructionIdentification = "MFX20260807T1530000001",
                    endToEndIdentification = "E2E-INTL-202608",
                    currencyOfTransfer = "EUR",
                    instructedAmount = IntlInstructedAmount(
                        amount = "100.00",
                        currency = "GBP",
                    ),
                    creditorAccount = IntlCreditorAccount(
                        schemeName = "UK.OBIE.IBAN",
                        identification = "DE89370400440532013000",
                        name = "Klara Weiss",
                    ),
                    chargeBearer = "BorneByCreditor",
                    debtorAccount = IntlDebtorAccount(
                        schemeName = "UK.OBIE.SortCodeAccountNumber",
                        identification = "80200110203349",
                    ),
                ),
            ),
            risk = IntlRisk(
                categoryPurposeCode = "EPAY",
            ),
        )

    @Test
    fun `stages an international consent with correct path and headers`() = runTest {
        val pisp = pisp()

        pisp.createInternationalPaymentConsent(
            paymentsScopeToken = "cc-payments-token",
            request = internationalConsentRequest(),
            idempotencyKey = IDEMPOTENCY_KEY,
        )

        val request = captured.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/v4.0/pisp/international-payment-consents", request.path)
        assertEquals("Bearer cc-payments-token", request.headers[HttpHeaders.Authorization])
        assertNotNull(request.headers[HEADER_JWS_SIGNATURE])
        assertEquals(IDEMPOTENCY_KEY, request.headers[HEADER_IDEMPOTENCY_KEY])
    }

    @Test
    fun `deserialises international consent response`() = runTest {
        val pisp = pisp(
            responseBody = INTL_CONSENT_RESPONSE,
        )

        val result = pisp.createInternationalPaymentConsent(
            paymentsScopeToken = "cc-token",
            request = internationalConsentRequest(),
            idempotencyKey = IDEMPOTENCY_KEY,
        )

        val consent = assertIs<NetworkResult.Success<*>>(result).data
        assertNotNull(consent)
    }

    @Test
    fun `reads an international consent status`() = runTest {
        val pisp = pisp(
            responseBody = """{"Data":{"ConsentId":"45076","Status":"AUTH"}}""",
            status = HttpStatusCode.OK,
        )

        val result = pisp.getInternationalPaymentConsent("cc-token", "45076")

        assertIs<NetworkResult.Success<*>>(result)
        assertEquals("/v4.0/pisp/international-payment-consents/45076", captured.single().path)
    }

    @Test
    fun `confirms international funds with the correct path`() = runTest {
        val pisp = pisp(
            responseBody = """{"Data":{"FundsAvailableResult":{"FundsAvailable":true}}}""",
            status = HttpStatusCode.OK,
        )

        val result = pisp.getInternationalFundsConfirmation("psu-token", "45076")

        assertIs<NetworkResult.Success<*>>(result)
        assertEquals(
            "/v4.0/pisp/international-payment-consents/45076/funds-confirmation",
            captured.single().path,
        )
    }

    @Test
    fun `submits an international payment`() = runTest {
        val pisp = pisp(
            responseBody = """{"Data":{"InternationalPaymentId":"INT-PMT-1","Status":"ACSP"}}""",
        )

        val submission = InternationalPaymentRequest(
            data = org.mifosx.openbanking.core.network.model.pisp.internationalPayment.request.Data(
                consentId = "45076",
                initiation = internationalConsentRequest().data?.initiation,
            ),
            risk = internationalConsentRequest().risk,
        )
        val result = pisp.createInternationalPayment(
            psuAccessToken = "psu-payments-token",
            request = submission,
            idempotencyKey = "intl-submit-key",
        )

        val response = assertIs<NetworkResult.Success<*>>(result).data
        assertNotNull(response)
        val request = captured.single()
        assertEquals("/v4.0/pisp/international-payments", request.path)
        assertEquals("Bearer psu-payments-token", request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun `reads an international payment status`() = runTest {
        val pisp = pisp(
            responseBody = """{"Data":{"InternationalPaymentId":"INT-PMT-1","Status":"ACSC"}}""",
            status = HttpStatusCode.OK,
        )

        val result = pisp.getInternationalPayment("cc-token", "INT-PMT-1")

        assertIs<NetworkResult.Success<*>>(result)
        assertEquals("/v4.0/pisp/international-payments/INT-PMT-1", captured.single().path)
    }

    @Test
    fun `deserialises charges from international consent`() = runTest {
        val pisp = pisp(
            responseBody = INTL_CONSENT_WITH_CHARGES_RESPONSE,
        )

        val result = pisp.createInternationalPaymentConsent(
            paymentsScopeToken = "cc-token",
            request = internationalConsentRequest(),
            idempotencyKey = IDEMPOTENCY_KEY,
        )

        assertIs<NetworkResult.Success<*>>(result)
    }
}
