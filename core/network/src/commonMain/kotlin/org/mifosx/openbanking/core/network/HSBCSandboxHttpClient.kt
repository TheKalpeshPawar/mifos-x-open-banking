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
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.forms.submitForm
import io.ktor.http.HttpHeaders
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.mifosx.openbanking.core.model.oauth.RefreshTokenResponse
import org.mifosx.openbanking.core.network.certs.CertPaths
import org.mifosx.openbanking.core.network.certs.loadCertBytes
import org.mifosx.openbanking.core.network.config.HsbcConfig
import org.mifosx.openbanking.core.network.mtls.MtlsIdentity
import org.mifosx.openbanking.core.network.mtls.installMtls
import template.core.base.network.httpClient
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import co.touchlab.kermit.Logger.Companion as KermitLogger

const val HSBC_TOKENS = "hsbc_tokens"

private const val TOKEN_ENDPOINT = "v1.1/oauth2/token"
private const val CLIENT_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer"
private const val REQUEST_TIMEOUT_MS = 60_000L

@Serializable
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String? = null,
)

internal fun saveTokens(settings: Settings, accessTokens: AuthTokens) {
    settings.putString(HSBC_TOKENS, Json.encodeToString(accessTokens))
}

internal fun loadTokens(settings: Settings): AuthTokens? =
    settings.getStringOrNull(HSBC_TOKENS)?.let { Json.decodeFromString(it) }

/**
 * The single HSBC Open Banking sandbox client.
 *
 * Built on the borrowed [httpClient] engine picker with our own config: mTLS via [installMtls]
 * (transport identity from `composeResources`), FAPI `private_key_jwt` bearer-token refresh (signing
 * key from `composeResources`, `client_id`/`kid` from [HsbcConfig]), JSON negotiation, sanitized
 * logging, timeouts, and the sandbox base URL. Tokens persist through the injected [settings] — wire
 * a secure `Settings` (EncryptedSharedPreferences / Keychain / desktop AES) in DI.
 */
@OptIn(ExperimentalUuidApi::class)
suspend fun hsbcSandboxHttpClient(settings: Settings): HttpClient {
    val config = HSBCUKSandboxConfig.UKPersonal
    val tokenUrl = getBaseUrl(config) + TOKEN_ENDPOINT
    val clientId = HsbcConfig.CLIENT_ID
    val kid = HsbcConfig.KID

    val identity = MtlsIdentity(pkcs12 = loadCertBytes(CertPaths.TRANSPORT_P12))
    val signingKeyPem = loadCertBytes(CertPaths.SIGNING_KEY_PEM).decodeToString()

    suspend fun refreshAccessToken(httpClient: HttpClient, refreshToken: String): AuthTokens {
        val clientAssertion = buildClientAssertion(
            clientId = clientId,
            kid = kid,
            tokenUrl = tokenUrl,
            nowEpochSeconds = Clock.System.now().epochSeconds,
            jti = Uuid.generateV4().toString(),
            privateKeyPem = signingKeyPem,
        )
        val response: RefreshTokenResponse = httpClient.submitForm(
            url = tokenUrl,
            formParameters = parameters {
                append("grant_type", "refresh_token")
                append("client_assertion_type", CLIENT_ASSERTION_TYPE)
                append("client_assertion", clientAssertion)
                append("refresh_token", refreshToken)
                append("redirect_uri", config.redirectUri)
            },
        ).body()

        return AuthTokens(
            accessToken = response.accessToken ?: "",
            refreshToken = response.refreshToken,
        )
    }

    return httpClient {
        installMtls(identity)

        install(Auth) {
            bearer {
                loadTokens {
                    loadTokens(settings)?.let { BearerTokens(it.accessToken, it.refreshToken) }
                }
                refreshTokens {
                    loadTokens(settings)?.refreshToken?.let { refreshToken ->
                        val refreshed = refreshAccessToken(client, refreshToken)
                        saveTokens(settings, refreshed)
                        BearerTokens(refreshed.accessToken, refreshed.refreshToken)
                    }
                }
                // The PSU bearer is auto-sent to AIS reads only. The token endpoint (client_assertion
                // auth) and account-access-consents (temporary token, set per call) are excluded.
                sendWithoutRequest { request ->
                    val path = request.url.encodedPathSegments.joinToString("/")
                    request.url.host == config.bankHost &&
                        "oauth2/token" !in path &&
                        "account-access-consents" !in path
                }
            }
        }

        install(ContentNegotiation) {
            json(
                Json {
                    isLenient = true
                    ignoreUnknownKeys = true
                    explicitNulls = false
                },
            )
        }

        install(Logging) {
            level = LogLevel.ALL
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
            logger = object : Logger {
                override fun log(message: String) {
                    KermitLogger.d(tag = "KtorClient", messageString = message)
                }
            }
        }

        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            socketTimeoutMillis = REQUEST_TIMEOUT_MS
        }

        defaultRequest {
            url(getBaseUrl(config))
            headers.append(HttpHeaders.Accept, "application/json")
        }
    }
}
