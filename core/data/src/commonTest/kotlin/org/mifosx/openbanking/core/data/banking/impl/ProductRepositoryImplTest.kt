/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking.impl

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.mifosx.openbanking.core.network.api.Aisp
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val ACCOUNT_ID = "40051512345678"

private val PRODUCT_JSON = """
{
  "Data": {
    "Product": [{
      "AccountId": "$ACCOUNT_ID",
      "ProductId": "HSBC-ADVANCE-PCA-001",
      "ProductType": "PCA",
      "ProductName": "HSBC Advance Account",
      "PCA": {
        "ProductDetails": {
          "MonthlyMaximumCharge": "0.00",
          "Features": ["No monthly maintenance fee"]
        },
        "CreditInterest": {
          "TierBandSet": [{
            "TierBandMethod": "Tiered",
            "TierBand": [{
              "TierValueMinimum": "0.01",
              "BandLimit": "£1,000",
              "AER": "0.15",
              "ApplicationFrequency": "Monthly"
            }]
          }]
        },
        "Overdraft": {
          "OverdraftTierBandSet": [{
            "OverdraftTierBand": [{ "OverdraftType": "Arranged", "EAR": "39.9" }]
          }]
        }
      }
    }]
  }
}
""".trimIndent()

private const val EMPTY_PRODUCT_JSON = """{ "Data": { "Product": [] } }"""

/**
 * Drives [ProductRepositoryImpl] through a real Ktor client over [MockEngine], so the serialization and
 * the status-to-error mapping are exercised for real rather than stubbed.
 */
class ProductRepositoryImplTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun repository(status: HttpStatusCode, body: String): ProductRepositoryImpl {
        val client = HttpClient(MockEngine { respond(body, status, jsonHeaders) }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return ProductRepositoryImpl(aisp = Aisp(client))
    }

    @Test
    fun aSuccessfulFetchMapsToProductTerms() = runTest {
        val result = repository(HttpStatusCode.OK, PRODUCT_JSON).getProduct(ACCOUNT_ID)

        val success = assertIs<NetworkResult.Success<*>>(result)
        val terms = assertIs<org.mifosx.openbanking.core.model.banking.ProductTerms>(success.data)
        assertEquals("HSBC Advance Account", terms.productName)
        assertEquals("0.15", terms.creditInterestTiers.single().aer)
        assertEquals("39.9", terms.overdraftTiers.single().ear)
        assertTrue(terms.features.isNotEmpty())
    }

    @Test
    fun anEmptyProductListSucceedsWithNoTerms() = runTest {
        val result = repository(HttpStatusCode.OK, EMPTY_PRODUCT_JSON).getProduct(ACCOUNT_ID)

        val success = assertIs<NetworkResult.Success<*>>(result)
        assertNull(success.data)
    }

    @Test
    fun anUnauthorizedResponseSurfacesAsUnauthorized() = runTest {
        val result = repository(HttpStatusCode.Unauthorized, "{}").getProduct(ACCOUNT_ID)

        val error = assertIs<NetworkResult.Error<*>>(result)
        assertIs<NetworkError.Client.Unauthorized>(error.error)
    }

    @Test
    fun aForbiddenResponseSurfacesAsForbidden() = runTest {
        val result = repository(HttpStatusCode.Forbidden, "{}").getProduct(ACCOUNT_ID)

        val error = assertIs<NetworkResult.Error<*>>(result)
        assertIs<NetworkError.Client.Forbidden>(error.error)
    }

    @Test
    fun aNotFoundResponseSurfacesAsNotFound() = runTest {
        val result = repository(HttpStatusCode.NotFound, "{}").getProduct(ACCOUNT_ID)

        val error = assertIs<NetworkResult.Error<*>>(result)
        assertIs<NetworkError.Client.NotFound>(error.error)
    }

    @Test
    fun aServerFailureSurfacesAsServer() = runTest {
        val result = repository(HttpStatusCode.InternalServerError, "{}").getProduct(ACCOUNT_ID)

        val error = assertIs<NetworkResult.Error<*>>(result)
        assertIs<NetworkError.Server>(error.error)
    }

    @Test
    fun theRequestTargetsTheProductSubPath() = runTest {
        var requestedPath: String? = null
        val client = HttpClient(
            MockEngine { request ->
                requestedPath = request.url.encodedPath
                respond(PRODUCT_JSON, HttpStatusCode.OK, jsonHeaders)
            },
        ) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        ProductRepositoryImpl(aisp = Aisp(client)).getProduct(ACCOUNT_ID)

        assertTrue(requestedPath.orEmpty().endsWith("/accounts/$ACCOUNT_ID/product"))
    }
}
