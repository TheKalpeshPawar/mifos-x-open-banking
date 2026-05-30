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
import org.mifosx.openbanking.core.model.obp.Card
import org.mifosx.openbanking.core.model.obp.CardsResponse
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

/** OBP card endpoints for an account. */
interface CardsApi {

    @GET("v3.0.0/banks/{bankId}/accounts/{accountId}/cards")
    suspend fun listCards(
        @Path("bankId") bankId: String,
        @Path("accountId") accountId: String,
    ): NetworkResult<CardsResponse, NetworkError>

    @GET("v3.0.0/banks/{bankId}/accounts/{accountId}/cards/{cardId}")
    suspend fun getCard(
        @Path("bankId") bankId: String,
        @Path("accountId") accountId: String,
        @Path("cardId") cardId: String,
    ): NetworkResult<Card, NetworkError>
}
