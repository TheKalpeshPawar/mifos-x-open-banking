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
import de.jensklingenberg.ktorfit.http.PUT
import de.jensklingenberg.ktorfit.http.Path
import org.mifosx.openbanking.core.model.obp.AccountApplication
import org.mifosx.openbanking.core.model.obp.AccountApplicationStatusRequest
import org.mifosx.openbanking.core.model.obp.AccountApplicationsResponse
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

/** OBP account-application endpoints (field-officer review). */
interface AccountApplicationsApi {

    @GET("v4.0.0/banks/{bankId}/account-applications")
    suspend fun listApplications(
        @Path("bankId") bankId: String,
    ): NetworkResult<AccountApplicationsResponse, NetworkError>

    @GET("v4.0.0/banks/{bankId}/account-applications/{applicationId}")
    suspend fun getApplication(
        @Path("bankId") bankId: String,
        @Path("applicationId") applicationId: String,
    ): NetworkResult<AccountApplication, NetworkError>

    @PUT("v4.0.0/banks/{bankId}/account-applications/{applicationId}")
    suspend fun updateStatus(
        @Path("bankId") bankId: String,
        @Path("applicationId") applicationId: String,
        @Body request: AccountApplicationStatusRequest,
    ): NetworkResult<AccountApplication, NetworkError>
}
