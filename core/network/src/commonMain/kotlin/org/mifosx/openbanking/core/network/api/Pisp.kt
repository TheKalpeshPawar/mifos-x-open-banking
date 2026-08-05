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
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import kotlinx.serialization.json.JsonObject
import org.mifosx.openbanking.core.network.model.pisp.domesticPayment.request.DomesticPaymentConsentRequest
import org.mifosx.openbanking.core.network.model.pisp.domesticPayment.request.DomesticPaymentRequest
import org.mifosx.openbanking.core.network.model.pisp.domesticPayment.response.DomesticPaymentConsentResponse
import org.mifosx.openbanking.core.network.model.pisp.domesticPayment.response.DomesticPaymentResponse
import org.mifosx.openbanking.core.network.model.pisp.fundsConfirmation.response.FundsConfirmationResponse
import org.mifosx.openbanking.core.network.pisp.detachedJwsSignature
import org.mifosx.openbanking.core.network.pisp.fapiHeaders
import org.mifosx.openbanking.core.network.pisp.obieBody
import org.mifosx.openbanking.core.network.pisp.writeHeaders
import org.mifosx.openbanking.core.network.result.toNetworkResult
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

private const val PIS = "v4.0/pisp"

/**
 * HSBC OBIE PISP endpoints — the app's only write surface. Paths are relative and resolve against
 * the client's `defaultRequest` base URL. Every call returns a [NetworkResult].
 *
 * Unlike the AIS reads, **no call here is authenticated by the client's bearer interceptor**: each
 * takes its token explicitly, because the payment journey uses two credentials that are both
 * distinct from the AIS PSU bearer the rest of the app runs on. Staging is authorised by a
 * client-credentials token minted with `scope=payments`; funds confirmation, submission and status
 * are authorised by the PSU token from the payments authorisation leg. `isAisResourceRequest`
 * excludes this whole path prefix so the interceptor cannot overwrite the header set here.
 *
 * The two write calls additionally carry a detached JWS over the request body and an idempotency
 * key. The key is a parameter rather than something this class generates: it is staged once per
 * payment and replayed unchanged on every retry.
 */
class Pisp(
    private val httpClient: HttpClient,
    private val kid: String,
    private val signingKeyPem: String,
    private val financialId: String,
) {

    /**
     * Stages a payment. The bank returns a `ConsentId` with `Status` `AWAU`, awaiting the PSU's
     * authorisation; nothing moves until [createDomesticPayment] is called against it.
     *
     * [paymentsScopeToken] must be a client-credentials token minted with `scope=payments` — the AIS
     * client-credentials token fails here on scope, and the failure reads as an auth problem.
     */
    suspend fun createDomesticPaymentConsent(
        paymentsScopeToken: String,
        request: DomesticPaymentConsentRequest,
        idempotencyKey: String,
    ): NetworkResult<DomesticPaymentConsentResponse, NetworkError> =
        signedPost(
            path = "$PIS/domestic-payment-consents",
            accessToken = paymentsScopeToken,
            idempotencyKey = idempotencyKey,
            body = obieBody(DomesticPaymentConsentRequest.serializer(), request),
        ).toNetworkResult()

    /** Reads a staged consent back, principally to observe its `Status` after authorisation. */
    suspend fun getDomesticPaymentConsent(
        accessToken: String,
        consentId: String,
    ): NetworkResult<DomesticPaymentConsentResponse, NetworkError> =
        authorizedGet("$PIS/domestic-payment-consents/$consentId", accessToken).toNetworkResult()

    /**
     * Asks whether the debtor account can cover the staged amount. Callable only once the consent is
     * authorised, since it needs the PSU token.
     *
     * Optional in the protocol, but binding when made: a `false` result must stop the submission.
     * This is the authoritative check — the amount screen's balance comparison is advisory and can
     * pass while this refuses.
     */
    suspend fun getFundsConfirmation(
        psuAccessToken: String,
        consentId: String,
    ): NetworkResult<FundsConfirmationResponse, NetworkError> =
        authorizedGet(
            "$PIS/domestic-payment-consents/$consentId/funds-confirmation",
            psuAccessToken,
        ).toNetworkResult()

    /**
     * Executes the payment against an authorised consent.
     *
     * `Data.ConsentId` must match the staged consent and `Data.Initiation` must be byte-identical to
     * the one that was staged, or the bank refuses with `U008`. Pass the same `Initiation` instance
     * that was staged rather than rebuilding it — serialization is deterministic, reconstruction is
     * not.
     */
    suspend fun createDomesticPayment(
        psuAccessToken: String,
        request: DomesticPaymentRequest,
        idempotencyKey: String,
    ): NetworkResult<DomesticPaymentResponse, NetworkError> =
        signedPost(
            path = "$PIS/domestic-payments",
            accessToken = psuAccessToken,
            idempotencyKey = idempotencyKey,
            body = obieBody(DomesticPaymentRequest.serializer(), request),
        ).toNetworkResult()

    /**
     * Reads a submitted payment's settlement status.
     *
     * A pure read — no JWS, no idempotency key — so it survives expiry of the single-payment PSU
     * token on a client-credentials `payments` token.
     */
    suspend fun getDomesticPayment(
        accessToken: String,
        domesticPaymentId: String,
    ): NetworkResult<DomesticPaymentResponse, NetworkError> =
        authorizedGet("$PIS/domestic-payments/$domesticPaymentId", accessToken).toNetworkResult()

    private suspend fun authorizedGet(path: String, accessToken: String): HttpResponse =
        httpClient.get(path) {
            bearerAuth(accessToken)
            fapiHeaders(financialId)
        }

    /**
     * Posts [body] with a detached JWS over it.
     *
     * The body is transmitted as the same string that was signed, wrapped in [TextContent] so
     * content negotiation cannot re-serialise it into different bytes and invalidate the signature.
     */
    private suspend fun signedPost(
        path: String,
        accessToken: String,
        idempotencyKey: String,
        body: JsonObject,
    ): HttpResponse {
        val payload = body.toString()
        val signature = detachedJwsSignature(payload = body, kid = kid, signingKeyPem = signingKeyPem)
        return httpClient.post(path) {
            bearerAuth(accessToken)
            fapiHeaders(financialId)
            writeHeaders(idempotencyKey = idempotencyKey, jwsSignature = signature)
            setBody(TextContent(payload, ContentType.Application.Json))
        }
    }
}
