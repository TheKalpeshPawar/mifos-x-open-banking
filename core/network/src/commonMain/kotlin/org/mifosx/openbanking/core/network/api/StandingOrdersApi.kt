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

import de.jensklingenberg.ktorfit.http.DELETE
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Path
import org.mifosx.openbanking.core.model.obp.StandingOrder
import org.mifosx.openbanking.core.model.obp.StandingOrdersResponse
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

/** OBP standing-order endpoints for an account. */
interface StandingOrdersApi {

    @GET("v3.0.0/banks/{bankId}/accounts/{accountId}/standing-orders")
    suspend fun listStandingOrders(
        @Path("bankId") bankId: String,
        @Path("accountId") accountId: String,
    ): NetworkResult<StandingOrdersResponse, NetworkError>

    @GET("v4.0.0/banks/{bankId}/accounts/{accountId}/standing-orders/{standingOrderId}")
    suspend fun getStandingOrder(
        @Path("bankId") bankId: String,
        @Path("accountId") accountId: String,
        @Path("standingOrderId") standingOrderId: String,
    ): NetworkResult<StandingOrder, NetworkError>

    @DELETE("v4.0.0/banks/{bankId}/accounts/{accountId}/standing-orders/{standingOrderId}")
    suspend fun cancelStandingOrder(
        @Path("bankId") bankId: String,
        @Path("accountId") accountId: String,
        @Path("standingOrderId") standingOrderId: String,
    ): NetworkResult<StandingOrder, NetworkError>
}
