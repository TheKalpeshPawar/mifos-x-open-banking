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
import io.ktor.client.request.forms.submitForm
import io.ktor.http.parameters
import org.mifosx.openbanking.core.model.createConsent.CreateConsentTokenSuccess
import org.mifosx.openbanking.core.model.oauth.PsuTokenResponse
import org.mifosx.openbanking.core.model.oauth.RefreshTokenResponse
import org.mifosx.openbanking.core.network.buildClientAssertion
import org.mifosx.openbanking.core.network.result.toNetworkResult
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val CLIENT_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer"

/**
 * The HSBC OBIE OAuth2 token endpoint (`private_key_jwt` client auth). Every call returns a
 * [NetworkResult]; the caller decides what to do with the token (the client-credentials token is
 * temporary and NOT stored, the PSU token from the auth-code exchange is what gets persisted).
 */
class OAuth(
    private val httpClient: HttpClient,
    private val tokenUrl: String,
    private val clientId: String,
    private val kid: String,
    private val signingKeyPem: String,
) {
    @OptIn(ExperimentalUuidApi::class)
    private suspend fun clientAssertion(): String = buildClientAssertion(
        clientId = clientId,
        kid = kid,
        tokenUrl = tokenUrl,
        nowEpochSeconds = Clock.System.now().epochSeconds,
        jti = Uuid.generateV4().toString(),
        privateKeyPem = signingKeyPem,
    )

    /** Temporary client-credentials token used to create/manage account-access-consents. */
    suspend fun clientCredentialsToken(
        scope: ConsentCreationScope,
    ): NetworkResult<CreateConsentTokenSuccess, NetworkError> {
        val assertion = clientAssertion()
        return httpClient.submitForm(
            url = tokenUrl,
            formParameters = parameters {
                append("grant_type", "client_credentials")
                append("scope", scope.value)
                append("client_assertion_type", CLIENT_ASSERTION_TYPE)
                append("client_assertion", assertion)
            },
        ).toNetworkResult()
    }

    /** Exchanges the authorization code (from the redirect callback) for the PSU access token. */
    suspend fun exchangeAuthorizationCode(
        code: String,
        redirectUri: String,
    ): NetworkResult<PsuTokenResponse, NetworkError> {
        val assertion = clientAssertion()
        return httpClient.submitForm(
            url = tokenUrl,
            formParameters = parameters {
                append("grant_type", "authorization_code")
                append("code", code)
                append("redirect_uri", redirectUri)
                append("client_assertion_type", CLIENT_ASSERTION_TYPE)
                append("client_assertion", assertion)
            },
        ).toNetworkResult()
    }

    /** Exchanges a refresh token for a fresh PSU access token. */
    suspend fun refreshToken(
        refreshToken: String,
    ): NetworkResult<RefreshTokenResponse, NetworkError> {
        val assertion = clientAssertion()
        return httpClient.submitForm(
            url = tokenUrl,
            formParameters = parameters {
                append("grant_type", "refresh_token")
                append("refresh_token", refreshToken)
                append("client_assertion_type", CLIENT_ASSERTION_TYPE)
                append("client_assertion", assertion)
            },
        ).toNetworkResult()
    }
}

enum class ConsentCreationScope(val value: String) {
    ACCOUNTS("accounts"),
    PAYMENTS("payments"),
}
