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

import org.mifosx.openbanking.core.model.banking.CreditInterestTier
import org.mifosx.openbanking.core.model.banking.OverdraftTier
import org.mifosx.openbanking.core.model.banking.ProductTerms
import org.mifosx.openbanking.core.network.model.ais.product.Product
import org.mifosx.openbanking.core.network.model.ais.product.ProductBlock
import org.mifosx.openbanking.core.network.model.ais.product.ProductResponse

/**
 * Maps the OBIE `OBReadProduct2` payload into the flat [ProductTerms] the product screen renders, for
 * [fallbackAccountId] when the payload omits its own `AccountId`.
 *
 * Returns **null** when there is nothing to show — an absent or empty `Data.Product` list, or a product
 * whose `PCA` and `BCA` blocks are both null (GlobalMoney, Savings and CreditCard publish no product
 * entry). The caller turns that null into the screen's informational empty state, which is deliberately
 * distinct from a fetch failure.
 *
 * Only the first product is mapped: OBIE returns the product *of* an account, so the list carries one
 * entry per account and this endpoint is already scoped to a single account.
 */
fun ProductResponse.toProductTerms(fallbackAccountId: String): ProductTerms? {
    val product = data?.product?.firstOrNull()
    val terms = product?.let { it.pca ?: it.bca }
    return if (product != null && terms != null) {
        product.toProductTerms(terms, fallbackAccountId)
    } else {
        null
    }
}

private fun Product.toProductTerms(
    terms: ProductBlock,
    fallbackAccountId: String,
): ProductTerms = ProductTerms(
    accountId = accountId ?: fallbackAccountId,
    productId = productId.orEmpty(),
    productName = productName.orEmpty(),
    productType = productType.orEmpty(),
    monthlyMaximumCharge = terms.productDetails?.monthlyMaximumCharge,
    creditInterestTiers = terms.creditInterestTiers(),
    overdraftTiers = terms.overdraftTiers(),
    features = terms.productDetails?.features.orEmpty(),
)

/** Flattens every `TierBand` across all `TierBandSet` groups, dropping bands with no rate to show. */
private fun ProductBlock.creditInterestTiers(): List<CreditInterestTier> =
    creditInterest?.tierBandSet.orEmpty()
        .flatMap { it.tierBand.orEmpty() }
        .mapNotNull { band ->
            val aer = band.aer ?: return@mapNotNull null
            CreditInterestTier(
                bandLimit = band.bandLimit.orEmpty(),
                aer = aer,
                applicationFrequency = band.applicationFrequency.orEmpty(),
            )
        }

/** Flattens every `OverdraftTierBand` across all groups, dropping bands with no rate to show. */
private fun ProductBlock.overdraftTiers(): List<OverdraftTier> =
    overdraft?.overdraftTierBandSet.orEmpty()
        .flatMap { it.overdraftTierBand.orEmpty() }
        .mapNotNull { band ->
            val ear = band.ear ?: return@mapNotNull null
            OverdraftTier(overdraftType = band.overdraftType.orEmpty(), ear = ear)
        }
