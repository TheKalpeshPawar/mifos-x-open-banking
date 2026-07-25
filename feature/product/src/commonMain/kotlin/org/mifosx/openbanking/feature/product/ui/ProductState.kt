/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.product.ui

import template.core.base.network.NetworkError

/**
 * Display-ready product terms.
 *
 * The view model formats what it can format alone — the money amount — and passes the rest through as
 * data. The remaining labels ("Up to £1,000 · paid Monthly", "0.15% AER", "Product ID: …") are
 * templated i18n resources, and only a composable can resolve those, so they are assembled at the call
 * site rather than duplicated here as pre-baked strings.
 *
 * @property monthlyMaximumChargeLabel Formatted charge, e.g. `£0.00`; null when the product publishes
 *   none, in which case the Fees section is omitted entirely.
 * @property isFeeFree Drives the primary tint the design gives a zero monthly charge.
 */
data class ProductUiModel(
    val productType: String,
    val productName: String,
    val productId: String,
    val monthlyMaximumChargeLabel: String?,
    val isFeeFree: Boolean,
    val creditInterestTiers: List<CreditTierUiModel>,
    val overdraftTiers: List<OverdraftTierUiModel>,
    val features: List<String>,
)

/** One credit-interest row: `Up to £1,000 · paid Monthly` on the left, `0.15% AER` trailing. */
data class CreditTierUiModel(
    val bandLimit: String,
    val aer: String,
    val applicationFrequency: String,
)

/** One overdraft row: `Arranged overdraft` on the left, `39.9% EAR` trailing. */
data class OverdraftTierUiModel(
    val overdraftType: String,
    val ear: String,
)

/**
 * What went wrong fetching the product.
 *
 * There is no `ProductNotFound` member: a 404 means the account simply has no product entry, which the
 * screen shows as its informational empty state rather than a failure (`data-flow.yaml` on_error
 * routing). Only the three genuine failures reach this enum.
 */
enum class ProductErrorKind {
    /** HTTP 401 — the PSU token expired and re-authentication is needed. */
    TokenExpired,

    /** HTTP 403 — the active consent does not carry `ReadProducts`. */
    ConsentScope,

    /** Offline, unreachable endpoint, or any other unclassified failure. */
    Network,
}

/**
 * Maps a transport failure onto the screen's error vocabulary.
 *
 * A 404 never arrives here — [ProductViewModel] routes it to Empty before classifying.
 */
fun classifyProductError(error: NetworkError): ProductErrorKind = when (error) {
    is NetworkError.Client.Unauthorized -> ProductErrorKind.TokenExpired
    is NetworkError.Client.Forbidden -> ProductErrorKind.ConsentScope
    else -> ProductErrorKind.Network
}

/** The four states of the product screen, one per `ui.yaml#states`. */
sealed interface ProductUiState {
    data object Loading : ProductUiState

    data class Content(val product: ProductUiModel) : ProductUiState

    /** No product entry for this account — GlobalMoney, Savings, CreditCard, or a 404. */
    data object Empty : ProductUiState

    data class Error(val kind: ProductErrorKind) : ProductUiState
}

data class ProductState(
    val accountId: String = "",
    val uiState: ProductUiState = ProductUiState.Loading,
)

sealed interface ProductAction {
    /** Fetch the product terms; dispatched on mount. */
    data object ProductLoad : ProductAction

    /** Re-run [ProductLoad] after an error. */
    data object RetryLoad : ProductAction
}
