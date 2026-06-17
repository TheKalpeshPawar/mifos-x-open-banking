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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.core.data.accounts.AccountsRepository
import org.mifosx.openbanking.core.data.banks.BanksRepository
import org.mifosx.openbanking.core.data.products.ProductsRepository
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.core.model.obp.Bank
import org.mifosx.openbanking.core.model.obp.Product
import template.core.base.store.screen.ScreenDataStream
import template.core.base.store.screen.ScreenState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val ACME_PRODUCTS = listOf(
    Product(code = "CURRENT-PERSONAL", name = "Acme Current Account Personal"),
    Product(code = "SAVINGS-EASY", name = "Acme Easy Access Savings"),
    Product(code = "CREDIT_CARD", name = "Credit Card Account"),
    Product(code = "BUSINESS", name = "Business Current Account"),
)

private val MIFOS_PRODUCTS = listOf(
    Product(code = "BUSINESS-CURRENT-GBP", name = "Mifos Business Current GBP"),
    Product(code = "WORKING-CAPITAL", name = "Mifos Working Capital Facility"),
    Product(code = "FX-SPOT-GBP-EUR", name = "Mifos FX Spot GBP/EUR"),
    Product(code = "SAVINGS", name = "Savings Account"),
)

private class ProductsFakeRepository(
    var catalogues: Map<String, Result<List<Product>>> = mapOf(
        "ac.bank.uk" to Result.success(ACME_PRODUCTS),
        "mifos-x-openbank" to Result.success(MIFOS_PRODUCTS),
    ),
) : ProductsRepository {
    var fetches = 0
    override fun productsStream(scope: CoroutineScope): ScreenDataStream<List<Product>> = TODO("not used")
    override suspend fun listProducts(bankId: String): Result<List<Product>> {
        fetches++
        return catalogues[bankId] ?: Result.success(emptyList())
    }
}

private class ProductsFakeAccountsRepository(
    private val accounts: List<Account> = listOf(
        Account(id = "acc-1", bankId = "ac.bank.uk"),
        Account(id = "acc-2", bankId = "ac.bank.uk"),
        Account(id = "acc-3", bankId = "mifos-x-openbank"),
    ),
    private val failure: Throwable? = null,
) : AccountsRepository {
    override fun accountsStream(scope: CoroutineScope): ScreenDataStream<List<Account>> = TODO("not used")
    override suspend fun listAccounts(): Result<List<Account>> = TODO("not used")
    override suspend fun myAccounts(): Result<List<Account>> =
        failure?.let { Result.failure(it) } ?: Result.success(accounts)
    override suspend fun accountDetail(bankId: String, accountId: String): Result<Account> = TODO("not used")
}

private class ProductsFakeBanksRepository : BanksRepository {
    private val names = mapOf("ac.bank.uk" to "Acme Bank", "mifos-x-openbank" to "Mifos Open Bank")
    override suspend fun bankName(bankId: String): String = names[bankId] ?: bankId
    override suspend fun bank(bankId: String): Bank? = null
}

class ProductsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(
        repository: ProductsFakeRepository = ProductsFakeRepository(),
        accounts: ProductsFakeAccountsRepository = ProductsFakeAccountsRepository(),
    ) = ProductsViewModel(
        productsRepository = repository,
        accountsRepository = accounts,
        banksRepository = ProductsFakeBanksRepository(),
    )

    private suspend fun TestScope.content(model: ProductsViewModel): ProductsContent {
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        val s = model.uiState.value
        assertTrue(s is ScreenState.Content, "expected Content, was $s")
        return s.data
    }

    @Test
    fun load_listsUserBanksWithResolvedNamesAndDefaultsToFirst() = runTest(dispatcher) {
        val c = content(vm())
        assertEquals(listOf("Acme Bank", "Mifos Open Bank"), c.banks.map { it.name })
        assertEquals("ac.bank.uk", c.selectedBankId)
        assertEquals(ProductScope.PERSONAL, c.scope)
    }

    @Test
    fun personalTab_excludesBusinessProducts() = runTest(dispatcher) {
        val c = content(vm())
        assertEquals(
            listOf("CURRENT-PERSONAL", "SAVINGS-EASY", "CREDIT_CARD").sorted(),
            c.products.map { it.code }.sorted(),
        )
    }

    @Test
    fun businessTab_showsOnlyBusinessProducts() = runTest(dispatcher) {
        val model = vm()
        content(model)
        model.onScopeSelected(ProductScope.BUSINESS)
        advanceUntilIdle()
        val c = (model.uiState.value as ScreenState.Content).data
        assertEquals(listOf("BUSINESS"), c.products.map { it.code })
    }

    @Test
    fun bankSwitch_showsThatBanksCatalogueWithoutRefetch() = runTest(dispatcher) {
        val repository = ProductsFakeRepository()
        val model = vm(repository)
        content(model)
        val fetchesAfterLoad = repository.fetches

        model.onBankSelected("mifos-x-openbank")
        model.onScopeSelected(ProductScope.BUSINESS)
        advanceUntilIdle()
        val c = (model.uiState.value as ScreenState.Content).data
        assertEquals("mifos-x-openbank", c.selectedBankId)
        assertEquals(
            listOf("BUSINESS-CURRENT-GBP", "FX-SPOT-GBP-EUR", "WORKING-CAPITAL").sorted(),
            c.products.map { it.code }.sorted(),
        )
        assertEquals(fetchesAfterLoad, repository.fetches)
    }

    @Test
    fun singleBankFailure_flagsThatBankOnly() = runTest(dispatcher) {
        val repository = ProductsFakeRepository(
            catalogues = mapOf(
                "ac.bank.uk" to Result.failure(IllegalStateException("boom")),
                "mifos-x-openbank" to Result.success(MIFOS_PRODUCTS),
            ),
        )
        val model = vm(repository)
        val c = content(model)
        assertTrue(c.bankLoadFailed)

        model.onBankSelected("mifos-x-openbank")
        advanceUntilIdle()
        val mifos = (model.uiState.value as ScreenState.Content).data
        assertTrue(!mifos.bankLoadFailed)
        assertEquals(listOf("SAVINGS"), mifos.products.map { it.code })
    }

    @Test
    fun allBanksFailing_isError() = runTest(dispatcher) {
        val repository = ProductsFakeRepository(
            catalogues = mapOf(
                "ac.bank.uk" to Result.failure(IllegalStateException("boom")),
                "mifos-x-openbank" to Result.failure(IllegalStateException("boom")),
            ),
        )
        val model = vm(repository)
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(model.uiState.value is ScreenState.Error)
    }

    @Test
    fun accountsFailure_isErrorAndRetryRecovers() = runTest(dispatcher) {
        val accounts = ProductsFakeAccountsRepository(failure = IllegalStateException("boom"))
        val model = vm(accounts = accounts)
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(model.uiState.value is ScreenState.Error)
    }

    @Test
    fun noAccounts_isEmptyState() = runTest(dispatcher) {
        val model = vm(accounts = ProductsFakeAccountsRepository(accounts = emptyList()))
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(model.uiState.value is ScreenState.Empty)
    }
}
