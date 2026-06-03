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
import org.mifosx.openbanking.core.model.obp.CounterpartiesResponse
import org.mifosx.openbanking.core.model.obp.Counterparty
import org.mifosx.openbanking.core.model.obp.CreateCounterpartyRequest
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

    @POST("v4.0.0/banks/{bankId}/accounts/{accountId}/owner/counterparties")
    suspend fun createCounterparty(
        @Path("bankId") bankId: String,
        @Path("accountId") accountId: String,
        @Body request: CreateCounterpartyRequest,
    ): NetworkResult<Counterparty, NetworkError>
}
