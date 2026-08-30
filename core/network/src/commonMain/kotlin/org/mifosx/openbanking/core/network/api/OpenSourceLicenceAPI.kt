package org.mifosx.openbanking.core.network.api

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import org.mifosx.openbanking.core.network.result.toNetworkResult
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult


private const val OPEN_SOURCE_LICENCE_URL = "https://raw.githubusercontent.com/openMF/mifos-x-open-banking/refs/heads/dev/LICENSE"

class OpenSourceLicence (
    private val httpClient: HttpClient,
) {
    suspend fun fetchLicence(): NetworkResult<String, NetworkError> =
        httpClient.get(OPEN_SOURCE_LICENCE_URL).toNetworkResult()
}