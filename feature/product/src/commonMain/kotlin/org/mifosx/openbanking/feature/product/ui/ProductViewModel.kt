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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.mifosx.openbanking.core.common.formatMoney
import org.mifosx.openbanking.core.data.banking.ProductRepository
import org.mifosx.openbanking.core.model.banking.ProductTerms
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import template.core.base.ui.viewmodel.BaseViewModel

/**
 * Drives the product-terms screen.
 *
 * Unlike every other screen here this one does not observe a `ScreenDataStream`: `data-flow.yaml`
 * declares `cache_strategy: none`, because the bank can revise product terms and a cached copy could
 * misstate the charges a customer is subject to. So the view model calls a one-shot repository and maps
 * the raw [NetworkResult] onto the four states itself.
 *
 * Two results both mean "nothing to show", and both land on Empty rather than Error: a `Success` with no
 * product (`PCA` and `BCA` both absent — GlobalMoney, Savings, CreditCard) and an HTTP 404 (no product
 * record for the account). Neither is a failure the customer can act on, so neither gets the error
 * treatment or a Retry.
 *
 * Money is formatted here; the templated i18n labels are resolved by the composables, which are the only
 * things able to read string resources. Navigation is the screen's job, so no events are emitted.
 */
class ProductViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: ProductRepository,
) : BaseViewModel<ProductState, Nothing, ProductAction>(
    initialState = ProductState(
        accountId = savedStateHandle.get<String>(ACCOUNT_ID_ARG).orEmpty(),
    ),
) {

    init {
        trySendAction(ProductAction.ProductLoad)
    }

    override fun handleAction(action: ProductAction) {
        when (action) {
            ProductAction.ProductLoad -> load()
            ProductAction.RetryLoad -> load()
        }
    }

    private fun load() {
        updateState { copy(uiState = ProductUiState.Loading) }
        viewModelScope.launch {
            val uiState = when (val result = repository.getProduct(state.accountId)) {
                is NetworkResult.Success -> result.data.toUiState()
                is NetworkResult.Error -> result.error.toUiState()
            }
            updateState { copy(uiState = uiState) }
        }
    }

    /** A null payload means the account publishes no product — Empty, not Error. */
    private fun ProductTerms?.toUiState(): ProductUiState =
        this?.let { ProductUiState.Content(it.toUiModel()) } ?: ProductUiState.Empty

    /** A 404 is "no product for this account", so it joins the Empty branch; the rest are failures. */
    private fun NetworkError.toUiState(): ProductUiState = when (this) {
        is NetworkError.Client.NotFound -> ProductUiState.Empty
        else -> ProductUiState.Error(classifyProductError(this))
    }

    private fun ProductTerms.toUiModel(): ProductUiModel = ProductUiModel(
        productType = productType,
        productName = productName,
        productId = productId,
        monthlyMaximumChargeLabel = monthlyMaximumCharge?.let { formatMoney(it, GBP) },
        isFeeFree = monthlyMaximumCharge?.isFeeFree() == true,
        creditInterestTiers = creditInterestTiers.map {
            CreditTierUiModel(
                bandLimit = it.bandLimit,
                aer = it.aer,
                applicationFrequency = it.applicationFrequency,
            )
        },
        overdraftTiers = overdraftTiers.map {
            OverdraftTierUiModel(overdraftType = it.overdraftType, ear = it.ear)
        },
        features = features,
    )

    companion object {
        /** Must match the [ProductRoute] `accountId` property name — nav uses it as the key. */
        const val ACCOUNT_ID_ARG: String = "accountId"
    }
}

/**
 * OBIE publishes `MonthlyMaximumCharge` without a currency — the product endpoint carries no currency
 * field at all — and every HSBC UK PCA and BCA is denominated in sterling, so the charge is formatted as
 * GBP.
 */
private const val GBP = "GBP"

/** True when the charge is zero, however the bank spelled it (`"0"`, `"0.00"`, `"0.0000"`). */
private fun String.isFeeFree(): Boolean = toDoubleOrNull() == 0.0
