/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network

import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.request.forms.submitForm
import io.ktor.http.parameters
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.mifosx.openbanking.core.model.oauth.RefreshTokenResponse
import template.core.base.network.setupDefaultHttpClient
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

const val HSBC_TOKENS = "hsbc_tokens"

@Serializable
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String? = null,
)

internal fun saveTokens(
    settings: Settings,
    accessTokens: AuthTokens,
) {
    settings.putString(
        HSBC_TOKENS,
        Json.encodeToString(accessTokens),
    )
}

internal fun loadTokens(
    settings: Settings,
): AuthTokens? {
    settings.getStringOrNull(HSBC_TOKENS)?.let {
        return Json.decodeFromString(it)
    }
    return null
}

@OptIn(ExperimentalUuidApi::class)
fun hsbcSandboxHttpClient(
    clientId: String,
    kid: String,
    privateKeyPem: String,
    settings: Settings,
): HttpClient {
    suspend fun refreshAccessToken(
        httpClient: HttpClient,
        refreshToken: String,
    ): AuthTokens {
        val url = getBaseUrl(HSBCUKSandboxConfig.UKPersonal) + "v1.1/oauth2/token"
        val redirectUri = HSBCUKSandboxConfig.UKPersonal.bankHost

        val clientAssertion: String = buildClientAssertion(
            clientId = clientId,
            kid = kid,
            tokenUrl = url,
            nowEpochSeconds = Clock.System.now().epochSeconds,
            jti = Uuid.generateV4().toString(),
            privateKeyPem = privateKeyPem,
        )
        val refreshToken: RefreshTokenResponse = httpClient.submitForm(
            url = getBaseUrl(HSBCUKSandboxConfig.UKPersonal) + "v1.1/oauth2/token",
            formParameters = parameters {
                append("grant_type", "refresh_token")
                append("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
                append("client_assertion", clientAssertion)
                append("refresh_token", refreshToken)
                append("redirect_uri", redirectUri)
            },
        ).body()

        return AuthTokens(
            accessToken = refreshToken.accessToken ?: "",
            refreshToken = refreshToken.refreshToken,
        )
    }

    return setupDefaultHttpClient(
        baseUrl = getBaseUrl(HSBCUKSandboxConfig.UKPersonal),
        isReleaseBuild = false,
        authRequiredUrl = listOf(getBaseUrl(HSBCUKSandboxConfig.UKPersonal)),
        defaultHeaders = mapOf(
            "Accept" to "application/json",
        ),
        bearerTokensProvider = {
            val tokens = loadTokens(settings)

            tokens?.let {
                BearerTokens(
                    it.accessToken,
                    it.refreshToken,
                )
            }
        },
        bearerRefreshProvider = { client ->
            val oldAuthToke = loadTokens(settings)

            oldAuthToke?.refreshToken?.let {
                val response = refreshAccessToken(
                    client,
                    refreshToken = it,
                )
                saveTokens(
                    settings,
                    accessTokens = response,
                )

                BearerTokens(
                    response.accessToken,
                    response.refreshToken,
                )
            }
        },
    )
}
