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
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Path
import org.mifosx.openbanking.core.model.obp.KycCheckRequest
import org.mifosx.openbanking.core.model.obp.KycDocumentsResponse
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

/** OBP KYC endpoints for a customer. */
interface KycApi {

    @GET("v3.0.0/banks/{bankId}/customers/{customerId}/kyc_documents")
    suspend fun listDocuments(
        @Path("bankId") bankId: String,
        @Path("customerId") customerId: String,
    ): NetworkResult<KycDocumentsResponse, NetworkError>

    @POST("v3.0.0/banks/{bankId}/customers/{customerId}/kyc_checks")
    suspend fun submitCheck(
        @Path("bankId") bankId: String,
        @Path("customerId") customerId: String,
        @Body request: KycCheckRequest,
    ): NetworkResult<KycCheckRequest, NetworkError>
}
