/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.network.api

import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Path
import de.jensklingenberg.ktorfit.http.Query
import org.mifosx.openbanking.core.model.obp.ChallengeAnswerBody
import org.mifosx.openbanking.core.model.obp.CounterpartiesResponse
import org.mifosx.openbanking.core.model.obp.CounterpartyTransactionRequestBody
import org.mifosx.openbanking.core.model.obp.FundsAvailableResponse
import org.mifosx.openbanking.core.model.obp.SandboxTanTransactionRequestBody
import org.mifosx.openbanking.core.model.obp.SepaTransactionRequestBody
import org.mifosx.openbanking.core.model.obp.TransactionRequest
import org.mifosx.openbanking.core.model.obp.TransactionRequestsResponse
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

/**
 * OBP payments / counterparty (beneficiary) endpoints. The `owner` view is fixed in the
 * path (matches the app's view convention; verified against the live v4.0.0 API — the
 * earlier v3.0.0 no-view path returned 404).
 */
interface PaymentsApi {

    @GET("v4.0.0/banks/{bankId}/accounts/{accountId}/owner/counterparties")
    suspend fun listCounterparties(
        @Path("bankId") bankId: String,
        @Path("accountId") accountId: String,
    ): NetworkResult<CounterpartiesResponse, NetworkError>

    /** Past transaction-requests for the account — records the paid counterparty/IBAN + date. */
    @GET("v4.0.0/banks/{bankId}/accounts/{accountId}/owner/transaction-requests")
    suspend fun listTransactionRequests(
        @Path("bankId") bankId: String,
        @Path("accountId") accountId: String,
    ): NetworkResult<TransactionRequestsResponse, NetworkError>

    /**
     * A single transaction-request by id. The SCA challenge is NOT inlined in the create response,
     * so an INITIATED payment must be re-fetched here to obtain its `challenge.id` before answering.
     */
    @GET("v4.0.0/banks/{bankId}/accounts/{accountId}/owner/transaction-requests/{requestId}")
    suspend fun getTransactionRequest(
        @Path("bankId") bankId: String,
        @Path("accountId") accountId: String,
        @Path("requestId") requestId: String,
    ): NetworkResult<TransactionRequest, NetworkError>

    @Headers("Content-Type: application/json")
    @POST("v4.0.0/banks/{bankId}/accounts/{accountId}/owner/transaction-request-types/SEPA/transaction-requests")
    suspend fun createSepaTransactionRequest(
        @Path("bankId") bankId: String,
        @Path("accountId") accountId: String,
        @Body request: SepaTransactionRequestBody,
    ): NetworkResult<TransactionRequest, NetworkError>

    @Suppress("MaxLineLength")
    @Headers("Content-Type: application/json")
    @POST("v4.0.0/banks/{bankId}/accounts/{accountId}/owner/transaction-request-types/COUNTERPARTY/transaction-requests")
    suspend fun createCounterpartyTransactionRequest(
        @Path("bankId") bankId: String,
        @Path("accountId") accountId: String,
        @Body request: CounterpartyTransactionRequestBody,
    ): NetworkResult<TransactionRequest, NetworkError>

    @GET("v3.1.0/banks/{bankId}/accounts/{accountId}/owner/funds-available")
    suspend fun checkFundsAvailable(
        @Path("bankId") bankId: String,
        @Path("accountId") accountId: String,
        @Query("amount") amount: String,
        @Query("currency") currency: String,
    ): NetworkResult<FundsAvailableResponse, NetworkError>

    /**
     * Answers the SCA challenge for an INITIATED transaction-request, completing the payment. The
     * body carries the challenge id and the user's answer (any positive integer in the sandbox).
     */
    @Suppress("MaxLineLength")
    @Headers("Content-Type: application/json")
    @POST("v4.0.0/banks/{bankId}/accounts/{accountId}/owner/transaction-request-types/{type}/transaction-requests/{requestId}/challenge")
    suspend fun answerTransactionRequestChallenge(
        @Path("bankId") bankId: String,
        @Path("accountId") accountId: String,
        @Path("type") type: String,
        @Path("requestId") requestId: String,
        @Body request: ChallengeAnswerBody,
    ): NetworkResult<TransactionRequest, NetworkError>

    /**
     * SANDBOX_TAN payment to an OBP-hosted account (sandbox only). Uses the v2.1.0 endpoint because
     * its challenge — created and answered through [answerSandboxTanChallenge] — does not enforce
     * maker/checker, so the maker can self-complete the SCA.
     */
    @Headers("Content-Type: application/json")
    @POST("v2.1.0/banks/{bankId}/accounts/{accountId}/owner/transaction-request-types/SANDBOX_TAN/transaction-requests")
    @Suppress("MaxLineLength")
    suspend fun createSandboxTanTransactionRequest(
        @Path("bankId") bankId: String,
        @Path("accountId") accountId: String,
        @Body request: SandboxTanTransactionRequestBody,
    ): NetworkResult<TransactionRequest, NetworkError>

    /** Answers a SANDBOX_TAN challenge via v2.1.0 (no maker/checker enforcement). */
    @Suppress("MaxLineLength")
    @Headers("Content-Type: application/json")
    @POST("v2.1.0/banks/{bankId}/accounts/{accountId}/owner/transaction-request-types/SANDBOX_TAN/transaction-requests/{requestId}/challenge")
    suspend fun answerSandboxTanChallenge(
        @Path("bankId") bankId: String,
        @Path("accountId") accountId: String,
        @Path("requestId") requestId: String,
        @Body request: ChallengeAnswerBody,
    ): NetworkResult<TransactionRequest, NetworkError>
}
