/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.network.obp

import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.plugin
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import template.core.base.network.factory.ResultSuspendConverterFactory
import template.core.base.network.httpClient
import template.core.base.network.setupDefaultHttpClient

/**
 * Ktor plugin that stamps the DirectLogin session token onto every outbound request
 * (unless the call already set its own `Authorization`, e.g. the login exchange).
 */
private fun obpAuthPlugin(tokenProvider: ObpTokenProvider) =
    createClientPlugin("ObpDirectLoginAuth") {
        onRequest { request, _ ->
            if (!request.headers.contains(HttpHeaders.Authorization)) {
                tokenProvider.authHeader()?.let { header ->
                    request.headers.append(HttpHeaders.Authorization, header)
                }
            }
        }
    }

/** OBP-configured Ktor [HttpClient]: base URL + JSON + logging + DirectLogin token auth. */
fun obpHttpClient(config: ObpConfig, tokenProvider: ObpTokenProvider): HttpClient {
    val defaults = setupDefaultHttpClient(
        baseUrl = config.baseUrl,
        loggableHosts = listOf("openbankproject.com"),
        jsonConfig = Json {
            isLenient = true
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
            encodeDefaults = true
        },
    )
    return httpClient {
        defaults(this)
        install(obpAuthPlugin(tokenProvider))
    }.also { client ->
        client.plugin(HttpSend).intercept { request ->
            val call = execute(request)
            if (call.response.status == HttpStatusCode.Unauthorized) {
                tokenProvider.invalidateSession()
            }
            call
        }
    }
}

/** Ktorfit instance over [client] with the NetworkResult-returning suspend converter. */
fun obpKtorfit(client: HttpClient): Ktorfit =
    Ktorfit.Builder()
        .httpClient(client)
        .converterFactories(ResultSuspendConverterFactory())
        .build()

/**
 * Bare JSON Ktor client for the OIDC flow. Unlike [obpHttpClient] it carries no DirectLogin auth
 * plugin (the OIDC provider must not see the OBP session token) and is used with absolute URLs,
 * since the flow spans the OBP host (discovery) and the OIDC provider host (auth/token).
 */
fun oidcHttpClient(config: ObpConfig): HttpClient {
    val defaults = setupDefaultHttpClient(
        baseUrl = config.baseUrl,
        loggableHosts = listOf("openbankproject.com"),
        jsonConfig = Json {
            isLenient = true
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
            encodeDefaults = true
        },
    )
    return httpClient { defaults(this) }
}
