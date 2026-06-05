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
import org.mifosx.openbanking.core.model.obp.CounterpartiesResponse
import org.mifosx.openbanking.core.model.obp.CounterpartyTransactionRequestBody
import org.mifosx.openbanking.core.model.obp.FundsAvailableResponse
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
}
