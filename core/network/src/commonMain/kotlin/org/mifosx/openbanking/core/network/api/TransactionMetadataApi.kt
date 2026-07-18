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
import de.jensklingenberg.ktorfit.http.DELETE
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Path
import org.mifosx.openbanking.core.model.obp.MetadataValueRequest
import org.mifosx.openbanking.core.model.obp.TransactionComment
import org.mifosx.openbanking.core.model.obp.TransactionCommentsResponse
import org.mifosx.openbanking.core.model.obp.TransactionTag
import org.mifosx.openbanking.core.model.obp.TransactionTagsResponse
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

/**
 * OBP v1.2.1 transaction-metadata endpoints — user tags and comments on one transaction.
 * (Verified live on apisandbox: GET/POST/DELETE all work; DELETE returns an empty body.)
 */
interface TransactionMetadataApi {

    @GET("v1.2.1/banks/{bankId}/accounts/{accountId}/owner/transactions/{transactionId}/metadata/tags")
    suspend fun listTags(
        @Path("bankId") bankId: String,
        @Path("accountId") accountId: String,
        @Path("transactionId") transactionId: String,
    ): NetworkResult<TransactionTagsResponse, NetworkError>

    @Headers("Content-Type: application/json")
    @POST("v1.2.1/banks/{bankId}/accounts/{accountId}/owner/transactions/{transactionId}/metadata/tags")
    suspend fun addTag(
        @Path("bankId") bankId: String,
        @Path("accountId") accountId: String,
        @Path("transactionId") transactionId: String,
        @Body request: MetadataValueRequest,
    ): NetworkResult<TransactionTag, NetworkError>

    @DELETE("v1.2.1/banks/{bankId}/accounts/{accountId}/owner/transactions/{transactionId}/metadata/tags/{tagId}")
    suspend fun deleteTag(
        @Path("bankId") bankId: String,
        @Path("accountId") accountId: String,
        @Path("transactionId") transactionId: String,
        @Path("tagId") tagId: String,
    ): NetworkResult<Unit, NetworkError>

    @GET("v1.2.1/banks/{bankId}/accounts/{accountId}/owner/transactions/{transactionId}/metadata/comments")
    suspend fun listComments(
        @Path("bankId") bankId: String,
        @Path("accountId") accountId: String,
        @Path("transactionId") transactionId: String,
    ): NetworkResult<TransactionCommentsResponse, NetworkError>

    @Headers("Content-Type: application/json")
    @POST("v1.2.1/banks/{bankId}/accounts/{accountId}/owner/transactions/{transactionId}/metadata/comments")
    suspend fun addComment(
        @Path("bankId") bankId: String,
        @Path("accountId") accountId: String,
        @Path("transactionId") transactionId: String,
        @Body request: MetadataValueRequest,
    ): NetworkResult<TransactionComment, NetworkError>

    @DELETE(
        "v1.2.1/banks/{bankId}/accounts/{accountId}/owner/transactions/{transactionId}/metadata/comments/{commentId}",
    )
    suspend fun deleteComment(
        @Path("bankId") bankId: String,
        @Path("accountId") accountId: String,
        @Path("transactionId") transactionId: String,
        @Path("commentId") commentId: String,
    ): NetworkResult<Unit, NetworkError>
}
