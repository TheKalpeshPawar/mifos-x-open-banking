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

/**
 * Stable Compose `testTag` identifiers for the product-terms surfaces. Shared by the desktop, Robolectric,
 * screenshot and on-device suites so all four drive the screen through one vocabulary. Append-only.
 */
internal object ProductTestTags {
    const val CONTENT = "product:content"
    const val LOADING = "product:loading"
    const val EMPTY = "product:empty"
    const val ERROR = "product:error"
    const val RETRY_BUTTON = "product:retryButton"
    const val BACK_BUTTON = "product:backButton"

    const val HEADER_CARD = "product:headerCard"
    const val TYPE_BADGE = "product:typeBadge"
    const val PRODUCT_NAME = "product:productName"
    const val PRODUCT_ID = "product:productId"

    const val FEES_SECTION = "product:feesSection"
    const val MONTHLY_MAX_CHARGE_ROW = "product:monthlyMaxChargeRow"
    const val CREDIT_INTEREST_SECTION = "product:creditInterestSection"
    const val OVERDRAFT_SECTION = "product:overdraftSection"
    const val FEATURES_SECTION = "product:featuresSection"

    private const val CREDIT_TIER_PREFIX = "product:creditTier:"
    private const val OVERDRAFT_TIER_PREFIX = "product:overdraftTier:"
    private const val FEATURE_PREFIX = "product:feature:"

    /** Per-row tag for a credit-interest tier, keyed by its position in the payload. */
    fun creditTier(index: Int): String = CREDIT_TIER_PREFIX + index

    /** Per-row tag for an overdraft tier, keyed by its position in the payload. */
    fun overdraftTier(index: Int): String = OVERDRAFT_TIER_PREFIX + index

    /** Per-row tag for a feature, keyed by its position in the payload. */
    fun feature(index: Int): String = FEATURE_PREFIX + index
}
