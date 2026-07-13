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

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import org.mifosx.openbanking.core.model.hsbcPermission.request.HSBCCreateConsentRequest
import org.mifosx.openbanking.core.model.hsbcPermission.response.HSBCCreateConsentResponse
import org.mifosx.openbanking.core.network.HSBCUKSandboxConfig
import org.mifosx.openbanking.core.network.getBaseUrl

class AISPApi(
    private val httpClient: HttpClient,
) {

    /**
     * Step 3 of the AIS flow — create an account-access consent.
     *
     * Uses the client-credentials Bearer token from [OAuth.getCCToken] and the
     * requested [HSBCCreateConsentRequest] permissions. On success (`201`) the
     * response carries the `ConsentId` with an initial `Status` of `AWAU`
     * (awaiting authorisation).
     */
    suspend fun createConsent(
        accessToken: String,
        request: HSBCCreateConsentRequest,
    ): HSBCCreateConsentResponse {
        return httpClient.post {
            url(getBaseUrl(HSBCUKSandboxConfig.UKPersonal) + "v4.0/aisp/account-access-consents")
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}
