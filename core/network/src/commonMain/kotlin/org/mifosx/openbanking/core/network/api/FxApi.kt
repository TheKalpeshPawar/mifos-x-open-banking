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
import org.mifosx.openbanking.core.model.obp.CurrenciesResponse
import org.mifosx.openbanking.core.model.obp.FxRate
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

/** OBP foreign-exchange endpoints (live rate per pair + supported currencies per bank). */
interface FxApi {

    @GET("v3.0.0/banks/{bankId}/fx/{fromCurrencyCode}/{toCurrencyCode}")
    suspend fun getRate(
        @Path("bankId") bankId: String,
        @Path("fromCurrencyCode") fromCurrencyCode: String,
        @Path("toCurrencyCode") toCurrencyCode: String,
    ): NetworkResult<FxRate, NetworkError>

    @GET("v5.1.0/banks/{bankId}/currencies")
    suspend fun getCurrencies(
        @Path("bankId") bankId: String,
    ): NetworkResult<CurrenciesResponse, NetworkError>
}
