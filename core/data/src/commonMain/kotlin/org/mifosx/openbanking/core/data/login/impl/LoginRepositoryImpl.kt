/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.login.impl

import org.mifosx.openbanking.core.data.login.ConsentResult
import org.mifosx.openbanking.core.data.login.LoginRepository
import org.mifosx.openbanking.core.model.hsbcPermission.OBPermission
import org.mifosx.openbanking.core.model.hsbcPermission.request.Data
import org.mifosx.openbanking.core.model.hsbcPermission.request.HSBCCreateConsentRequest
import org.mifosx.openbanking.core.model.hsbcPermission.request.Risk
import org.mifosx.openbanking.core.network.api.Aisp
import org.mifosx.openbanking.core.network.api.ConsentCreationScope
import org.mifosx.openbanking.core.network.api.OAuth
import org.mifosx.openbanking.core.network.authorize.generateConsentAuthorizationUrl
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

class LoginRepositoryImpl(
    private val oauth: OAuth,
    private val aisp: Aisp,
    private val signingKeyPem: String,
    private val clientId: String,
    private val kid: String,
    private val bankHost: String,
    private val authorizeHost: String,
    private val redirectUri: String,
) : LoginRepository {

    override fun getPermissions(): List<OBPermission> = OBPermission.ALL

    @Suppress("ReturnCount")
    override suspend fun createConsentAndBuildAuthorizationUrl(): NetworkResult<ConsentResult, NetworkError> {
        val tokenResult = oauth.clientCredentialsToken(ConsentCreationScope.ACCOUNTS)
        val tempToken = when (tokenResult) {
            is NetworkResult.Success -> tokenResult.data.accessToken
            is NetworkResult.Error -> return tokenResult
        }

        val permissions = OBPermission.ALL.map { it.id.obieScope }
        val now = Clock.System.now()
        val request = HSBCCreateConsentRequest(
            data = Data(
                expirationDateTime = (now + 90.days).toString(),
                permissions = permissions,
                transactionFromDateTime = (now - 90.days).toString(),
                transactionToDateTime = now.toString(),
            ),
            risk = Risk(),
        )
        val consentResult = aisp.createConsent(tempToken, request)
        val consentId = when (consentResult) {
            is NetworkResult.Success -> consentResult.data.data.consentId
            is NetworkResult.Error -> return consentResult
        }

        val audience = "https://$bankHost"
        val authorizeUrl = "https://$authorizeHost/obie/open-banking/v1.1/oauth2/authorize"
        val auth = generateConsentAuthorizationUrl(
            audience = audience,
            authorizeUrl = authorizeUrl,
            clientId = clientId,
            kid = kid,
            scope = ConsentCreationScope.ACCOUNTS,
            responseType = "code id_token",
            redirectUri = redirectUri,
            consentId = consentId,
            signingKeyPem = signingKeyPem,
            nowEpochSeconds = Clock.System.now().epochSeconds,
        )

        return NetworkResult.Success(
            ConsentResult(
                authorizationUrl = auth.authorizationUrl,
                state = auth.state,
                nonce = auth.nonce,
                consentId = consentId,
            ),
        )
    }
}
