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
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Path
import org.mifosx.openbanking.core.model.obp.CreateStandingOrderRequest
import org.mifosx.openbanking.core.model.obp.CreateStandingOrderResponse
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

/**
 * OBP standing-order endpoints. Create is the ONLY operation OBP exposes — there is no
 * list/get/delete at any API version (verified against the v6.0.0 spec), so the standing
 * orders screen derives its rows from transaction history instead of reading them back.
 */
interface StandingOrdersApi {

    @Headers("Content-Type: application/json")
    @POST("v4.0.0/banks/{bankId}/accounts/{accountId}/owner/standing-order")
    suspend fun createStandingOrder(
        @Path("bankId") bankId: String,
        @Path("accountId") accountId: String,
        @Body request: CreateStandingOrderRequest,
    ): NetworkResult<CreateStandingOrderResponse, NetworkError>
}
