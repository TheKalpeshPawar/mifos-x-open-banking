/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.model.banking

import kotlinx.serialization.Serializable

/**
 * A single credit-interest tier: the balance band it applies to and the rate paid on it.
 *
 * @property bandLimit Upper bound of the band, pre-formatted by the bank (e.g. `"£1,000"`) and shown
 *   verbatim — it is not a raw decimal, so it must not go through the money formatters.
 * @property aer Annual Equivalent Rate as a decimal percentage string, e.g. `"0.15"`.
 * @property applicationFrequency How often the interest is paid, e.g. `"Monthly"`.
 */
@Serializable
data class CreditInterestTier(
    val bandLimit: String,
    val aer: String,
    val applicationFrequency: String,
)

/**
 * A single overdraft tier.
 *
 * @property overdraftType `Arranged` or `Unarranged`.
 * @property ear Effective Annual Rate as a decimal percentage string, e.g. `"39.9"`.
 */
@Serializable
data class OverdraftTier(
    val overdraftType: String,
    val ear: String,
)

/**
 * Flattened product definition for the product-terms screen.
 *
 * OBIE nests the terms two levels deep — `OBProduct2.PCA.CreditInterest.TierBandSet[].TierBand[]` and
 * the overdraft equivalent — and splits them across `PCA` and `BCA` keys. The mapper resolves whichever
 * block is present and flattens the band groups into plain lists, so the feature module never sees an
 * OBIE shape and the screen renders straight from these fields.
 *
 * @property productType OBIE `ProductType` enum value (`PCA` / `BCA` / `Other`), rendered as a badge.
 * @property monthlyMaximumCharge Raw decimal major units (e.g. `"0.00"`), formatted for display in the
 *   view model; null when the product publishes no charge.
 * @property creditInterestTiers Every tier across all `TierBandSet` groups, in payload order.
 * @property overdraftTiers Every tier across all `OverdraftTierBandSet` groups, in payload order.
 */
@Serializable
data class ProductTerms(
    val accountId: String,
    val productId: String,
    val productName: String,
    val productType: String,
    val monthlyMaximumCharge: String?,
    val creditInterestTiers: List<CreditInterestTier>,
    val overdraftTiers: List<OverdraftTier>,
    val features: List<String>,
)
