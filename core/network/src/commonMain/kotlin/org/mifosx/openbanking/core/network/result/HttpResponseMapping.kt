/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.result

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import kotlinx.coroutines.CancellationException
import org.mifosx.openbanking.core.network.debug.OAuthDebugLog
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

/**
 * Our own, ktorfit-free conversion from a raw Ktor [HttpResponse] to the shared [NetworkResult].
 *
 * - 2xx → decode the body into [T]; any decode failure → [NetworkError.Serialization].
 * - non-2xx → [mapHttpError] with the raw body captured (the body carries the OBIE/OAuth error code).
 *
 * A transport failure (no response at all) throws before a response exists, so it is handled by the
 * caller/service, not here.
 */
suspend inline fun <reified T> HttpResponse.toNetworkResult(): NetworkResult<T, NetworkError> {
    val code = status.value
    val bodyText = bodyAsText()
    OAuthDebugLog.log(
        "HTTP-RESPONSE",
        "${request.method.value} ${request.url} -> $code ${status.description}\n" +
            "response-headers=${headers.entries().joinToString { "${it.key}: ${it.value}" }}\n" +
            "response-body=$bodyText",
    )
    if (code !in 200..299) {
        return NetworkResult.Error(mapHttpError(code, bodyText))
    }
    return try {
        NetworkResult.Success(body<T>())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        OAuthDebugLog.log("HTTP-DECODE-FAIL", "url=${request.url} error=${e.message}")
        NetworkResult.Error(NetworkError.Serialization(e))
    }
}

/**
 * Pure mapping from an HTTP status code (+ optional raw response body) to a [NetworkError] variant.
 * Isolated as a pure function so every branch is trivially unit-testable.
 */
fun mapHttpError(statusCode: Int, body: String?): NetworkError = when (statusCode) {
    400 -> NetworkError.Client.BadRequest(body)
    401 -> NetworkError.Client.Unauthorized(body)
    403 -> NetworkError.Client.Forbidden(body)
    404 -> NetworkError.Client.NotFound(body)
    429 -> NetworkError.Client.RateLimited(body)
    in 400..499 -> NetworkError.Client.Other(statusCode, body)
    in 500..599 -> NetworkError.Server(statusCode, body)
    else -> NetworkError.Unknown()
}
