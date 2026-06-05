/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.accounts.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.core.data.accounts.AccountsRepository
import org.mifosx.openbanking.core.data.banks.BanksRepository
import org.mifosx.openbanking.core.data.customers.CustomersRepository
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.core.model.obp.AccountAttribute
import org.mifosx.openbanking.core.model.obp.AccountOwner
import org.mifosx.openbanking.core.model.obp.AccountRouting
import org.mifosx.openbanking.core.model.obp.AmountOfMoney
import org.mifosx.openbanking.core.model.obp.Customer
import org.mifosx.openbanking.core.model.obp.CustomerRequest
import template.core.base.store.screen.ScreenDataStream
import template.core.base.store.screen.ScreenState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeAccountsRepository(
    var detail: Result<Account>,
) : AccountsRepository {
    override fun accountsStream(scope: CoroutineScope): ScreenDataStream<List<Account>> = TODO()
    override suspend fun listAccounts(): Result<List<Account>> = TODO()
    override suspend fun myAccounts(): Result<List<Account>> = TODO()
    override suspend fun accountDetail(bankId: String, accountId: String): Result<Account> = detail
}

private class FakeBanksRepository(private val name: String) : BanksRepository {
    override suspend fun bankName(bankId: String): String = name
}

private class FakeCustomersRepository(
    private val holder: Result<String> = Result.success(""),
) : CustomersRepository {
    override fun customersStream(scope: CoroutineScope): ScreenDataStream<List<Customer>> = TODO()
    override suspend fun list(): Result<List<Customer>> = TODO()
    override suspend fun get(customerId: String): Result<Customer> = TODO()
    override suspend fun create(request: CustomerRequest): Result<Customer> = TODO()
    override suspend fun update(customerId: String, request: CustomerRequest): Result<Customer> = TODO()
    override suspend fun accountHolderName(bankId: String, accountId: String): Result<String> = holder
}

private fun account() = Account(
    id = "mifos.techstart.current",
    bankId = "mifos-x-openbank",
    label = "TechStart — Business Current GBP",
    productCode = "BUSINESS-CURRENT-GBP",
    number = "2706651702",
    balance = AmountOfMoney(currency = "GBP", amount = "47211.27"),
    accountRoutings = listOf(
        AccountRouting(scheme = "OBP", address = "mifos.techstart.current"),
        AccountRouting(scheme = "IBAN", address = "GB29MFOS98765601001234"),
    ),
    accountAttributes = listOf(
        AccountAttribute(name = "MONTHLY_FEE", type = "DOUBLE", value = "25.00"),
    ),
    owners = listOf(AccountOwner(displayName = "afternooncoffee")),
)

class AccountDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(
        detail: Result<Account> = Result.success(account()),
        bankName: String = "Mifos-X-Open-Bank",
        holder: Result<String> = Result.success("Sarah Williams"),
    ) = AccountDetailViewModel(
        FakeAccountsRepository(detail),
        FakeBanksRepository(bankName),
        FakeCustomersRepository(holder),
    )

    @Test
    fun load_mapsAllRepresentableFields() = runTest(dispatcher) {
        val model = vm()
        backgroundScope.launch { model.uiState.collect {} }
        model.load("mifos-x-openbank", "mifos.techstart.current")
        advanceUntilIdle()

        val s = model.uiState.value
        assertTrue(s is ScreenState.Content)
        val d = s.data
        assertEquals("TechStart — Business Current GBP", d.accountLabel)
        assertEquals("£47,211.27", d.balanceDisplay)
        assertEquals("GBP", d.currency)
        assertEquals("Sarah Williams", d.holder) // linked customer (Owner), not the owners[] user
        assertEquals("2706651702", d.accountNumber)
        assertEquals("GB29MFOS98765601001234", d.ibanRaw)
        assertEquals("GB29 MFOS 9876 5601 0012 34", d.ibanDisplay)
        assertEquals("Mifos-X-Open-Bank", d.bankName)
        assertEquals("Business Current GBP", d.productLabel)
        assertEquals(1, d.attributes.size)
        assertEquals("Monthly Fee", d.attributes.single().label)
        assertEquals("£25.00", d.attributes.single().value)
        assertTrue(d.hasPlanSection)
    }

    @Test
    fun load_overdrawn_balanceKeepsSign() = runTest(dispatcher) {
        val model = vm(
            detail = Result.success(
                account().copy(balance = AmountOfMoney(currency = "EUR", amount = "-3970.29")),
            ),
        )
        backgroundScope.launch { model.uiState.collect {} }
        model.load("ac.bank.uk", "ac.checking.001")
        advanceUntilIdle()
        val s = model.uiState.value as ScreenState.Content
        assertEquals("-€3,970.29", s.data.balanceDisplay)
    }

    @Test
    fun load_noIbanRouting_leavesIbanBlank() = runTest(dispatcher) {
        val model = vm(detail = Result.success(account().copy(accountRoutings = emptyList())))
        backgroundScope.launch { model.uiState.collect {} }
        model.load("ac.bank.uk", "ac.checking.001")
        advanceUntilIdle()
        val s = model.uiState.value as ScreenState.Content
        assertEquals("", s.data.ibanRaw)
    }

    @Test
    fun load_noCustomerLink_fallsBackToOwnerUser() = runTest(dispatcher) {
        val model = vm(holder = Result.success("")) // no linked customer
        backgroundScope.launch { model.uiState.collect {} }
        model.load("mifos-x-openbank", "mifos.techstart.current")
        advanceUntilIdle()
        val s = model.uiState.value as ScreenState.Content
        assertEquals("afternooncoffee", s.data.holder) // owners[] display name
    }

    @Test
    fun load_failure_emitsError() = runTest(dispatcher) {
        val model = vm(detail = Result.failure(RuntimeException("boom")))
        backgroundScope.launch { model.uiState.collect {} }
        model.load("b", "a")
        advanceUntilIdle()
        assertTrue(model.uiState.value is ScreenState.Error)
    }
}
