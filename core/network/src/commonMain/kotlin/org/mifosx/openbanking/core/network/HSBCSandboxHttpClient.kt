/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.network

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.url
import io.ktor.http.parameters
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import template.core.base.network.httpClient
import template.core.base.network.setupDefaultHttpClientConfig

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

fun hsbcSandboxHttpClient(
    settings: Settings,
): HttpClient {
    suspend fun refreshAccessToken(httpClient: HttpClient): AuthTokens {
        val refreshToken = "v1.1/oauth2/token"
        val baseUrl = getBaseUrl(HSBCUKSandboxConfig.UKPersonal)

        return httpClient.submitForm(
            url = getBaseUrl(HSBCUKSandboxConfig.UKPersonal) + "v1.1/oauth2/token",
            formParameters = parameters {
                append("grant_type", "refresh_token")
                append("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
                append("client_assertion", clientAssertion)
            },
        ).body()
    }

    val httpClientConfig = setupDefaultHttpClientConfig(
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
        bearerRefreshProvider = {
        },
    )

    return HttpClient(httpClientConfig)
}

class OidcHttpClient(
    val datastore: Settings,
    val httpClient: HttpClient,
)

enum class GrantType(val value: String) {
    CLIENT_CREDENTIALS("client_credentials"),
    AUTHORIZATION_CODE("authorization_code"),
    REFRESH_TOKEN("refresh_token"),
}

enum class AccessScope(val scope: String) {
    ACCOUNTS("accounts"),
    PAYMENTS("payments"),
    OPENID_ACCOUNTS("openid accounts"),
}
