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

import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Path
import de.jensklingenberg.ktorfit.http.Query
import org.mifosx.openbanking.core.model.obp.Transaction
import org.mifosx.openbanking.core.model.obp.TransactionsResponse
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

/** OBP transaction endpoints for an account. */
interface TransactionsApi {

    @GET("v3.0.0/banks/{bankId}/accounts/{accountId}/owner/transactions")
    suspend fun listTransactions(
        @Path("bankId") bankId: String,
        @Path("accountId") accountId: String,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null,
    ): NetworkResult<TransactionsResponse, NetworkError>

    /**
     * v6.0.0 "full" transactions — returns `transaction_attributes` inline per transaction
     * (and `transaction_id`), so per-card filtering (CARD_ID) and type labels (TXN_TYPE) need
     * no N+1 attribute fetch. Used by the cards screen.
     */
    @GET("v6.0.0/banks/{bankId}/accounts/{accountId}/owner/transactions")
    suspend fun listTransactionsWithAttributes(
        @Path("bankId") bankId: String,
        @Path("accountId") accountId: String,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null,
    ): NetworkResult<TransactionsResponse, NetworkError>

    @GET("v3.0.0/banks/{bankId}/accounts/{accountId}/owner/transactions/{transactionId}/transaction")
    suspend fun getTransaction(
        @Path("bankId") bankId: String,
        @Path("accountId") accountId: String,
        @Path("transactionId") transactionId: String,
    ): NetworkResult<Transaction, NetworkError>
}
