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
import org.mifosx.openbanking.core.model.obp.UserAttribute
import org.mifosx.openbanking.core.model.obp.UserAttributeRequest
import org.mifosx.openbanking.core.model.obp.UserAttributesResponse
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

/**
 * OBP v6 personal-data-field endpoints (verified live on apisandbox). The app uses them
 * as a tiny per-user key/value store for PFM budgets.
 */
interface UserAttributesApi {

    @GET("v6.0.0/my/personal-data-fields")
    suspend fun listPersonalDataFields(): NetworkResult<UserAttributesResponse, NetworkError>

    @Headers("Content-Type: application/json")
    @POST("v6.0.0/my/personal-data-fields")
    suspend fun createPersonalDataField(
        @Body request: UserAttributeRequest,
    ): NetworkResult<UserAttribute, NetworkError>
}
