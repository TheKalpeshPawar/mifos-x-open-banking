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
import org.mifosx.openbanking.core.model.obp.Consent
import org.mifosx.openbanking.core.model.obp.ConsentsResponse
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

/** OBP consent (connected-app) endpoints. */
interface ConsentsApi {

    @GET("v4.0.0/consumers/{consumerId}/consents")
    suspend fun listConsents(
        @Path("consumerId") consumerId: String,
    ): NetworkResult<ConsentsResponse, NetworkError>

    @DELETE("v4.0.0/consumers/{consumerId}/consents/{consentId}")
    suspend fun revokeConsent(
        @Path("consumerId") consumerId: String,
        @Path("consentId") consentId: String,
    ): NetworkResult<Consent, NetworkError>
}
