/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.products.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mifosx.openbanking.core.data.accounts.AccountsRepository
import org.mifosx.openbanking.core.data.banks.BanksRepository
import org.mifosx.openbanking.core.data.products.ProductsRepository
import org.mifosx.openbanking.core.model.obp.Product
import template.core.base.store.screen.DataFreshness
import template.core.base.store.screen.ScreenState

/** Audience split of a bank's catalogue — every product is exactly one of the two. */
enum class ProductScope(val label: String) {
    PERSONAL("Personal"),
    BUSINESS("Business"),
}

/** Display grouping shown as a badge on each product card. */
enum class ProductCategory(val label: String) {
    EVERYDAY("Everyday"),
    SAVINGS("Savings"),
    CARDS("Cards"),
    LOANS("Loans"),
}

/**
 * Products ViewModel — per-bank product catalogues with a Personal / Business split.
 * The banks on offer are the ones where the user holds accounts (names resolved via the
 * bank directory); each bank's catalogue is fetched once per session, in parallel at
 * load. The OBP products endpoint returns null `category`/`family` on the sandbox, so
 * both the audience split and the category badge are derived from each product's code
 * and name.
 */
class ProductsViewModel(
    private val productsRepository: ProductsRepository,
    private val accountsRepository: AccountsRepository,
    private val banksRepository: BanksRepository,
) : ViewModel() {

    private val rawState = MutableStateFlow<RawState>(RawState.Loading)
    private val selection = MutableStateFlow(Selection())

    val uiState: StateFlow<ScreenState<ProductsContent>> =
        combine(rawState, selection) { raw, sel -> project(raw, sel) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ScreenState.Loading,
            )

    init {
        load()
    }

    fun onRetry() = load()

    fun onBankSelected(bankId: String) = selection.update { it.copy(bankId = bankId) }

    fun onScopeSelected(scope: ProductScope) = selection.update { it.copy(scope = scope) }

    private fun load() {
        rawState.value = RawState.Loading
        viewModelScope.launch {
            val bankIds = accountsRepository.myAccounts()
                .getOrElse {
                    rawState.value = RawState.Failed(it)
                    return@launch
                }
                .map { it.bankId }
                .filter { it.isNotBlank() }
                .distinct()
            if (bankIds.isEmpty()) {
                rawState.value = RawState.Loaded(emptyList(), emptyMap(), emptySet())
                return@launch
            }
            val banks = bankIds.map { ProductBank(id = it, name = banksRepository.bankName(it)) }
            val catalogues = coroutineScope {
                bankIds.map { bankId ->
                    async { bankId to productsRepository.listProducts(bankId) }
                }.awaitAll()
            }
            rawState.value = RawState.Loaded(
                banks = banks,
                catalogues = catalogues.mapNotNull { (id, result) ->
                    result.getOrNull()?.let { id to it }
                }.toMap(),
                failedBanks = catalogues.filter { it.second.isFailure }.map { it.first }.toSet(),
            )
        }
    }

    private fun project(raw: RawState, sel: Selection): ScreenState<ProductsContent> = when (raw) {
        is RawState.Loading -> ScreenState.Loading
        is RawState.Failed -> ScreenState.Error(raw.error)
        is RawState.Loaded -> when {
            raw.banks.isEmpty() -> ScreenState.Empty
            raw.catalogues.isEmpty() && raw.failedBanks.isNotEmpty() ->
                ScreenState.Error(IllegalStateException("No bank catalogue could be loaded"))
            else -> ScreenState.Content(
                data = loadedContent(raw, sel),
                freshness = DataFreshness.FRESH,
            )
        }
    }

    private fun loadedContent(raw: RawState.Loaded, sel: Selection): ProductsContent {
        val selectedBankId = sel.bankId.takeIf { id -> raw.banks.any { it.id == id } }
            ?: raw.banks.first().id
        val rows = raw.catalogues[selectedBankId].orEmpty()
            .map { it.toRow() }
            .filter { it.scope == sel.scope }
            .sortedBy { it.name }
        return ProductsContent(
            banks = raw.banks,
            selectedBankId = selectedBankId,
            scope = sel.scope,
            products = rows,
            bankLoadFailed = selectedBankId in raw.failedBanks,
        )
    }

    private data class Selection(
        val bankId: String = "",
        val scope: ProductScope = ProductScope.PERSONAL,
    )

    private sealed interface RawState {
        data object Loading : RawState
        data class Failed(val error: Throwable) : RawState
        data class Loaded(
            val banks: List<ProductBank>,
            val catalogues: Map<String, List<Product>>,
            val failedBanks: Set<String>,
        ) : RawState
    }
}

/** One bank whose catalogue can be browsed (the user holds an account there). */
@Immutable
data class ProductBank(
    val id: String,
    val name: String,
)

/** Loaded content for the Products screen. */
@Immutable
data class ProductsContent(
    val banks: List<ProductBank>,
    val selectedBankId: String,
    val scope: ProductScope,
    val products: List<ProductRow>,
    val bankLoadFailed: Boolean,
)

/** One display-ready product card. */
@Immutable
data class ProductRow(
    val code: String,
    val name: String,
    val description: String,
    val scope: ProductScope,
    val category: ProductCategory,
    val moreInfoUrl: String,
)

internal fun Product.toRow() = ProductRow(
    code = code,
    name = name.ifBlank { code },
    description = description,
    scope = scopeOf(this),
    category = categoryOf(this),
    moreInfoUrl = moreInfoUrl,
)

/** Business-audience keywords; everything else is a personal product. */
private val BUSINESS_MARKERS = listOf(
    "BUSINESS",
    "CORPORATE",
    "WORKING-CAPITAL",
    "WORKING CAPITAL",
    "FREELANCER",
    "FX-SPOT",
    "FX SPOT",
)

internal fun scopeOf(product: Product): ProductScope {
    val haystack = "${product.code} ${product.name}".uppercase()
    return if (BUSINESS_MARKERS.any { it in haystack }) ProductScope.BUSINESS else ProductScope.PERSONAL
}

/** Buckets a product by code/name keywords; the API's own category field is null on the sandbox. */
internal fun categoryOf(product: Product): ProductCategory {
    val haystack = "${product.code} ${product.name}".uppercase()
    return when {
        listOf("SAVING", "SAVE", "DEPOSIT").any { it in haystack } -> ProductCategory.SAVINGS
        "CARD" in haystack -> ProductCategory.CARDS
        listOf("LOAN", "MORTGAGE", "WORKING-CAPITAL", "WORKING CAPITAL").any { it in haystack } ->
            ProductCategory.LOANS
        else -> ProductCategory.EVERYDAY
    }
}
