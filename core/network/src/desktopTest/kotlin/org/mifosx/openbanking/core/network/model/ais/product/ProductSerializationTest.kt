/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.model.ais.product

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Round-trips the OBIE `OBReadProduct2` payload, pinning the `@SerialName` keys for the whole PCA and BCA
 * sub-tree — the tier bands nest three levels deep, so a mistyped key there fails silently as a null
 * rather than an exception.
 */
class ProductSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val payload = """
    {
      "Data": {
        "Product": [{
          "AccountId": "40051512345678",
          "ProductId": "HSBC-ADVANCE-PCA-001",
          "ProductType": "PCA",
          "ProductName": "HSBC Advance Account",
          "PCA": {
            "ProductDetails": {
              "MonthlyMaximumCharge": "0.00",
              "Features": ["No monthly maintenance fee", "24/7 telephone banking support"]
            },
            "CreditInterest": {
              "TierBandSet": [{
                "TierBandMethod": "Tiered",
                "TierBand": [
                  {
                    "TierValueMinimum": "0.01",
                    "BandLimit": "1000",
                    "AER": "0.00",
                    "ApplicationFrequency": "Monthly"
                  },
                  {
                    "TierValueMinimum": "1000.01",
                    "BandLimit": "10000",
                    "AER": "0.15",
                    "ApplicationFrequency": "Monthly"
                  }
                ]
              }]
            },
            "Overdraft": {
              "OverdraftTierBandSet": [{
                "OverdraftTierBand": [
                  { "TierValueMinimum": "0.00", "OverdraftType": "Arranged", "EAR": "39.9" },
                  { "TierValueMinimum": "0.00", "OverdraftType": "Unarranged", "EAR": "49.9" }
                ]
              }]
            }
          }
        }]
      },
      "Meta": { "TotalPages": 1 }
    }
    """.trimIndent()

    @Test
    fun theProductIdentityDecodes() {
        val product = json.decodeFromString<ProductResponse>(payload).data?.product?.single()

        assertEquals("40051512345678", product?.accountId)
        assertEquals("HSBC-ADVANCE-PCA-001", product?.productId)
        assertEquals("PCA", product?.productType)
        assertEquals("HSBC Advance Account", product?.productName)
    }

    @Test
    fun theProductDetailsDecode() {
        val details = json.decodeFromString<ProductResponse>(payload)
            .data?.product?.single()?.pca?.productDetails

        assertEquals("0.00", details?.monthlyMaximumCharge)
        assertEquals(2, details?.features?.size)
        assertEquals("No monthly maintenance fee", details?.features?.first())
    }

    @Test
    fun theCreditInterestTierBandsDecode() {
        val set = json.decodeFromString<ProductResponse>(payload)
            .data?.product?.single()?.pca?.creditInterest?.tierBandSet?.single()

        assertEquals("Tiered", set?.tierBandMethod)
        assertEquals(2, set?.tierBand?.size)
        assertEquals("0.15", set?.tierBand?.get(1)?.aer)
        assertEquals("10000", set?.tierBand?.get(1)?.bandLimit)
        assertEquals("Monthly", set?.tierBand?.get(1)?.applicationFrequency)
        assertEquals("1000.01", set?.tierBand?.get(1)?.tierValueMinimum)
    }

    @Test
    fun theOverdraftTierBandsDecode() {
        val bands = json.decodeFromString<ProductResponse>(payload)
            .data?.product?.single()?.pca?.overdraft?.overdraftTierBandSet?.single()?.overdraftTierBand

        assertEquals(2, bands?.size)
        assertEquals("Arranged", bands?.first()?.overdraftType)
        assertEquals("39.9", bands?.first()?.ear)
        assertEquals("Unarranged", bands?.get(1)?.overdraftType)
        assertEquals("49.9", bands?.get(1)?.ear)
    }

    @Test
    fun aBcaPayloadDecodesThroughTheSameBlockType() {
        val bcaPayload = payload
            .replace("\"PCA\": {", "\"BCA\": {")
            .replace("\"ProductType\": \"PCA\"", "\"ProductType\": \"BCA\"")

        val product = json.decodeFromString<ProductResponse>(bcaPayload).data?.product?.single()

        assertEquals("BCA", product?.productType)
        assertNull(product?.pca)
        assertEquals("0.00", product?.bca?.productDetails?.monthlyMaximumCharge)
    }

    @Test
    fun aProductWithNoTermsBlockDecodesToNulls() {
        val minimal = """{"Data":{"Product":[{"AccountId":"1","ProductType":"Other"}]}}"""

        val product = json.decodeFromString<ProductResponse>(minimal).data?.product?.single()

        assertNull(product?.pca)
        assertNull(product?.bca)
        assertNull(product?.productName)
    }

    @Test
    fun anEmptyProductListDecodes() {
        val empty = """{"Data":{"Product":[]}}"""

        assertTrue(json.decodeFromString<ProductResponse>(empty).data?.product.orEmpty().isEmpty())
    }

    @Test
    fun unknownFieldsAreTolerated() {
        val withExtras = """
            {"Data":{"Product":[{"AccountId":"1","ProductType":"PCA","SomeFutureField":"x",
            "PCA":{"ProductDetails":{"MonthlyMaximumCharge":"1.00","Unexpected":true}}}]}}
        """.trimIndent()

        val product = json.decodeFromString<ProductResponse>(withExtras).data?.product?.single()

        assertEquals("1.00", product?.pca?.productDetails?.monthlyMaximumCharge)
    }
}
