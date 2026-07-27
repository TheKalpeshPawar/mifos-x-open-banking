/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking.mapper

import org.mifosx.openbanking.core.network.model.ais.product.CreditInterest
import org.mifosx.openbanking.core.network.model.ais.product.Data
import org.mifosx.openbanking.core.network.model.ais.product.Overdraft
import org.mifosx.openbanking.core.network.model.ais.product.OverdraftTierBand
import org.mifosx.openbanking.core.network.model.ais.product.OverdraftTierBandSet
import org.mifosx.openbanking.core.network.model.ais.product.Product
import org.mifosx.openbanking.core.network.model.ais.product.ProductBlock
import org.mifosx.openbanking.core.network.model.ais.product.ProductDetails
import org.mifosx.openbanking.core.network.model.ais.product.ProductResponse
import org.mifosx.openbanking.core.network.model.ais.product.TierBand
import org.mifosx.openbanking.core.network.model.ais.product.TierBandSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val ACCOUNT_ID = "40051512345678"

/**
 * Covers the OBIE `OBReadProduct2` to [org.mifosx.openbanking.core.model.banking.ProductTerms] mapping:
 * PCA and BCA resolution, tier flattening, and the several shapes that all mean "nothing to show".
 */
class ProductMapperTest {

    private fun block() = ProductBlock(
        productDetails = ProductDetails(
            monthlyMaximumCharge = "0.00",
            features = listOf("No monthly maintenance fee", "Mobile and online banking included"),
        ),
        creditInterest = CreditInterest(
            tierBandSet = listOf(
                TierBandSet(
                    tierBandMethod = "Tiered",
                    tierBand = listOf(
                        TierBand(
                            tierValueMinimum = "0.01",
                            bandLimit = "£1,000",
                            aer = "0.00",
                            applicationFrequency = "Monthly",
                        ),
                        TierBand(
                            tierValueMinimum = "1000.01",
                            bandLimit = "£10,000",
                            aer = "0.15",
                            applicationFrequency = "Monthly",
                        ),
                    ),
                ),
            ),
        ),
        overdraft = Overdraft(
            overdraftTierBandSet = listOf(
                OverdraftTierBandSet(
                    overdraftTierBand = listOf(
                        OverdraftTierBand(overdraftType = "Arranged", ear = "39.9"),
                        OverdraftTierBand(overdraftType = "Unarranged", ear = "49.9"),
                    ),
                ),
            ),
        ),
    )

    private fun response(product: Product) = ProductResponse(data = Data(product = listOf(product)))

    private fun pcaProduct() = Product(
        productName = "HSBC Advance Account",
        productId = "HSBC-ADVANCE-PCA-001",
        accountId = ACCOUNT_ID,
        productType = "PCA",
        pca = block(),
    )

    @Test
    fun aPcaProductMapsEveryField() {
        val terms = response(pcaProduct()).toProductTerms(ACCOUNT_ID)

        assertEquals(ACCOUNT_ID, terms?.accountId)
        assertEquals("HSBC-ADVANCE-PCA-001", terms?.productId)
        assertEquals("HSBC Advance Account", terms?.productName)
        assertEquals("PCA", terms?.productType)
        assertEquals("0.00", terms?.monthlyMaximumCharge)
    }

    @Test
    fun creditInterestTiersAreFlattenedInOrder() {
        val tiers = response(pcaProduct()).toProductTerms(ACCOUNT_ID)?.creditInterestTiers.orEmpty()

        assertEquals(2, tiers.size)
        assertEquals("£1,000", tiers[0].bandLimit)
        assertEquals("0.00", tiers[0].aer)
        assertEquals("Monthly", tiers[0].applicationFrequency)
        assertEquals("£10,000", tiers[1].bandLimit)
        assertEquals("0.15", tiers[1].aer)
    }

    @Test
    fun overdraftTiersAreFlattenedInOrder() {
        val tiers = response(pcaProduct()).toProductTerms(ACCOUNT_ID)?.overdraftTiers.orEmpty()

        assertEquals(2, tiers.size)
        assertEquals("Arranged", tiers[0].overdraftType)
        assertEquals("39.9", tiers[0].ear)
        assertEquals("Unarranged", tiers[1].overdraftType)
        assertEquals("49.9", tiers[1].ear)
    }

    @Test
    fun featuresAreCarriedThrough() {
        val terms = response(pcaProduct()).toProductTerms(ACCOUNT_ID)

        assertEquals(2, terms?.features?.size)
        assertEquals("No monthly maintenance fee", terms?.features?.first())
    }

    @Test
    fun aBusinessProductResolvesFromTheBcaBlock() {
        val product = Product(
            productName = "HSBC Business Current Account",
            productId = "HSBC-BCA-001",
            accountId = ACCOUNT_ID,
            productType = "BCA",
            pca = null,
            bca = block(),
        )

        val terms = response(product).toProductTerms(ACCOUNT_ID)

        assertEquals("BCA", terms?.productType)
        assertEquals("HSBC Business Current Account", terms?.productName)
        assertEquals(2, terms?.creditInterestTiers?.size)
    }

    @Test
    fun pcaWinsWhenBothBlocksArePresent() {
        val product = pcaProduct().copy(
            bca = ProductBlock(productDetails = ProductDetails(monthlyMaximumCharge = "99.00")),
        )

        assertEquals("0.00", response(product).toProductTerms(ACCOUNT_ID)?.monthlyMaximumCharge)
    }

    @Test
    fun aProductWithNeitherBlockMapsToNull() {
        val product = Product(productName = "GlobalMoney", productType = null, pca = null, bca = null)

        assertNull(response(product).toProductTerms(ACCOUNT_ID))
    }

    @Test
    fun anEmptyProductListMapsToNull() {
        val response = ProductResponse(data = Data(product = emptyList()))

        assertNull(response.toProductTerms(ACCOUNT_ID))
    }

    @Test
    fun anAbsentDataBlockMapsToNull() {
        assertNull(ProductResponse(data = null).toProductTerms(ACCOUNT_ID))
    }

    @Test
    fun theFallbackAccountIdIsUsedWhenThePayloadOmitsIt() {
        val terms = response(pcaProduct().copy(accountId = null)).toProductTerms(ACCOUNT_ID)

        assertEquals(ACCOUNT_ID, terms?.accountId)
    }

    @Test
    fun absentNamesBecomeBlankRatherThanNull() {
        val product = Product(productName = null, productId = null, productType = null, pca = block())

        val terms = response(product).toProductTerms(ACCOUNT_ID)

        assertEquals("", terms?.productName)
        assertEquals("", terms?.productId)
        assertEquals("", terms?.productType)
    }

    @Test
    fun aBandWithoutARateIsDropped() {
        val product = pcaProduct().copy(
            pca = block().copy(
                creditInterest = CreditInterest(
                    tierBandSet = listOf(
                        TierBandSet(tierBand = listOf(TierBand(bandLimit = "£500", aer = null))),
                    ),
                ),
                overdraft = Overdraft(
                    overdraftTierBandSet = listOf(
                        OverdraftTierBandSet(
                            overdraftTierBand = listOf(OverdraftTierBand(overdraftType = "Arranged", ear = null)),
                        ),
                    ),
                ),
            ),
        )

        val terms = response(product).toProductTerms(ACCOUNT_ID)

        assertTrue(terms?.creditInterestTiers.orEmpty().isEmpty())
        assertTrue(terms?.overdraftTiers.orEmpty().isEmpty())
    }

    @Test
    fun anEmptyTermsBlockStillMapsToContent() {
        val terms = response(pcaProduct().copy(pca = ProductBlock())).toProductTerms(ACCOUNT_ID)

        assertNull(terms?.monthlyMaximumCharge)
        assertTrue(terms?.creditInterestTiers.orEmpty().isEmpty())
        assertTrue(terms?.features.orEmpty().isEmpty())
    }
}
