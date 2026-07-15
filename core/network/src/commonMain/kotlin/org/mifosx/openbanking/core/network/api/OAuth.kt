/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.http.parameters
import org.mifosx.openbanking.core.model.createConsent.CreateConsentTokenSuccess
import org.mifosx.openbanking.core.network.HSBCUKSandboxConfig
import org.mifosx.openbanking.core.network.buildClientAssertion
import org.mifosx.openbanking.core.network.getBaseUrl
import kotlin.uuid.ExperimentalUuidApi

class OAuth(
    val httpClient: HttpClient,
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend fun getCCToken(
        scope: String,
        clientId: String,
        kid: String,
        tokenUrl: String,
        nowEpochSeconds: Long,
        jti: String,
        privateKeyPem: String,
    ): CreateConsentTokenSuccess {
        val clientAssertion = buildClientAssertion(
            clientId = clientId,
            kid = kid,
            tokenUrl = tokenUrl,
            nowEpochSeconds = nowEpochSeconds,
            jti = jti,
            privateKeyPem = privateKeyPem,
        )

        return httpClient.submitForm(
            url = getBaseUrl(HSBCUKSandboxConfig.UKPersonal) + "v1.1/oauth2/token",
            formParameters = parameters {
                append("grant_type", "client_credentials")
                append("scope", scope)
                append("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
                append("client_assertion", clientAssertion)
            },
        ).body()
    }

}
