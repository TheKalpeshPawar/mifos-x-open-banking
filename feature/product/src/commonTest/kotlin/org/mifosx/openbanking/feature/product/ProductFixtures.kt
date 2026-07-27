/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.product

import org.mifosx.openbanking.core.model.banking.CreditInterestTier
import org.mifosx.openbanking.core.model.banking.OverdraftTier
import org.mifosx.openbanking.core.model.banking.ProductTerms
import org.mifosx.openbanking.feature.product.ui.CreditTierUiModel
import org.mifosx.openbanking.feature.product.ui.OverdraftTierUiModel
import org.mifosx.openbanking.feature.product.ui.ProductUiModel

/**
 * Product fixtures mirroring `idea-layer/screens/product/demo-data.yaml` — the HSBC Advance Account with
 * two credit-interest tiers, arranged and unarranged overdraft, and six features.
 *
 * Shared by the view-model, Compose, Robolectric and screenshot suites so all four assert against one
 * product rather than several that happen to look alike.
 */
object ProductFixtures {

    const val ACCOUNT_ID: String = "40051512345678"
    const val PRODUCT_ID: String = "HSBC-ADVANCE-PCA-001"
    const val PRODUCT_NAME: String = "HSBC Advance Account"

    val FEATURES: List<String> = listOf(
        "No monthly maintenance fee",
        "Mobile and online banking included",
        "Arranged overdraft buffer up to £25",
        "HSBC Rewards cashback on eligible spend",
        "Preferential rates on HSBC savings and mortgages",
        "24/7 telephone banking support",
    )

    /** The PCA product the content state renders. */
    fun terms(): ProductTerms = ProductTerms(
        accountId = ACCOUNT_ID,
        productId = PRODUCT_ID,
        productName = PRODUCT_NAME,
        productType = "PCA",
        monthlyMaximumCharge = "0.00",
        creditInterestTiers = listOf(
            CreditInterestTier(bandLimit = "£1,000", aer = "0.00", applicationFrequency = "Monthly"),
            CreditInterestTier(bandLimit = "£10,000", aer = "0.15", applicationFrequency = "Monthly"),
        ),
        overdraftTiers = listOf(
            OverdraftTier(overdraftType = "Arranged", ear = "39.9"),
            OverdraftTier(overdraftType = "Unarranged", ear = "49.9"),
        ),
        features = FEATURES,
    )

    /** A business current account — the terms resolve from the `BCA` block instead of `PCA`. */
    fun businessTerms(): ProductTerms = terms().copy(
        productType = "BCA",
        productName = "HSBC Business Current Account",
        productId = "HSBC-BCA-001",
        monthlyMaximumCharge = "8.00",
    )

    /** Display-ready model matching [terms], for the UI suites that render content directly. */
    fun uiModel(): ProductUiModel = ProductUiModel(
        productType = "PCA",
        productName = PRODUCT_NAME,
        productId = PRODUCT_ID,
        monthlyMaximumChargeLabel = "£0.00",
        isFeeFree = true,
        creditInterestTiers = listOf(
            CreditTierUiModel(bandLimit = "£1,000", aer = "0.00", applicationFrequency = "Monthly"),
            CreditTierUiModel(bandLimit = "£10,000", aer = "0.15", applicationFrequency = "Monthly"),
        ),
        overdraftTiers = listOf(
            OverdraftTierUiModel(overdraftType = "Arranged", ear = "39.9"),
            OverdraftTierUiModel(overdraftType = "Unarranged", ear = "49.9"),
        ),
        features = FEATURES,
    )
}
