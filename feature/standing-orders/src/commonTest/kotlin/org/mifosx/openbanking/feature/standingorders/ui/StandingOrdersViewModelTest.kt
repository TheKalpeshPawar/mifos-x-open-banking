/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.standingorders.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.core.data.accounts.AccountsRepository
import org.mifosx.openbanking.core.data.customers.CustomersRepository
import org.mifosx.openbanking.core.data.payments.PaymentsRepository
import org.mifosx.openbanking.core.data.profile.ProfileRepository
import org.mifosx.openbanking.core.data.standingorders.StandingOrdersRepository
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.core.model.obp.AmountOfMoney
import org.mifosx.openbanking.core.model.obp.Counterparty
import org.mifosx.openbanking.core.model.obp.CreateStandingOrderRequest
import org.mifosx.openbanking.core.model.obp.Customer
import org.mifosx.openbanking.core.model.obp.CustomerRequest
import org.mifosx.openbanking.core.model.obp.ProfileUpdateRequest
import org.mifosx.openbanking.core.model.obp.StandingOrder
import org.mifosx.openbanking.core.model.obp.TransactionRequest
import org.mifosx.openbanking.core.model.obp.TransactionRequestSummary
import org.mifosx.openbanking.core.model.obp.UserProfile
import template.core.base.store.screen.ScreenDataStream
import template.core.base.store.screen.ScreenState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private fun account(id: String = "ac.checking.001") = Account(
    id = id,
    bankId = "ac.bank.uk",
    accountType = "checking",
    balance = AmountOfMoney(currency = "EUR", amount = "1000.00"),
)

private fun order(name: String, status: String = StandingOrder.STATUS_ACTIVE) = StandingOrder(
    id = "so-derived-${name.lowercase()}",
    name = name,
    amountValue = "45.00",
    amountCurrency = "EUR",
    frequency = "MONTHLY",
    lastPaymentDate = "2026-06-01",
    nextPaymentDate = "2026-07-01",
    status = status,
)

private fun payee(id: String = "cp-1", name: String = "Savings Counterparty") = Counterparty(
    counterpartyId = id,
    name = name,
    isBeneficiary = true,
)

private class FakeStandingOrdersRepository(
    var orders: Result<List<StandingOrder>> = Result.success(emptyList()),
    var createResult: Result<StandingOrder> = Result.success(order("Created")),
) : StandingOrdersRepository {
    var lastCreateRequest: CreateStandingOrderRequest? = null
    override suspend fun listRecurring(accountId: String): Result<List<StandingOrder>> = orders
    override suspend fun create(
        accountId: String,
        name: String,
        request: CreateStandingOrderRequest,
    ): Result<StandingOrder> {
        lastCreateRequest = request
        return createResult
    }
}

private class FakeAccountsRepository(
    var accounts: Result<List<Account>> = Result.success(listOf(account())),
) : AccountsRepository {
    override fun accountsStream(scope: CoroutineScope): ScreenDataStream<List<Account>> = TODO()
    override suspend fun listAccounts(): Result<List<Account>> = accounts
    override suspend fun myAccounts(): Result<List<Account>> = accounts
    override suspend fun accountDetail(bankId: String, accountId: String): Result<Account> = TODO()
}

private class FakePaymentsRepository(
    var beneficiaries: Result<List<Counterparty>> = Result.success(listOf(payee())),
) : PaymentsRepository {
    override fun beneficiariesStream(accountId: String, scope: CoroutineScope): ScreenDataStream<List<Counterparty>> =
        TODO()
    override suspend fun listBeneficiaries(bankId: String, accountId: String): Result<List<Counterparty>> =
        beneficiaries
    override suspend fun listTransactionRequests(
        bankId: String,
        accountId: String,
    ): Result<List<TransactionRequestSummary>> = TODO()
    override suspend fun sendSepaPayment(
        bankId: String,
        accountId: String,
        iban: String,
        amount: String,
        currency: String,
        reference: String,
    ): Result<TransactionRequest> = TODO()
    override suspend fun sendToCounterparty(
        bankId: String,
        accountId: String,
        counterpartyId: String,
        amount: String,
        currency: String,
        reference: String,
    ): Result<TransactionRequest> = TODO()
    override suspend fun fundsAvailable(
        bankId: String,
        accountId: String,
        amount: String,
        currency: String,
    ): Result<Boolean> = TODO()
}

private class FakeProfileRepository : ProfileRepository {
    override suspend fun current(): Result<UserProfile> = Result.success(UserProfile(userId = "user-1"))
    override suspend fun update(request: ProfileUpdateRequest): Result<UserProfile> = TODO()
}

private class FakeCustomersRepository : CustomersRepository {
    override fun customersStream(scope: CoroutineScope): ScreenDataStream<List<Customer>> = TODO()
    override suspend fun list(): Result<List<Customer>> = TODO()
    override suspend fun currentUserCustomers(): Result<List<Customer>> =
        Result.success(listOf(Customer(customerId = "cust-1", bankId = "ac.bank.uk")))
    override suspend fun get(customerId: String): Result<Customer> = TODO()
    override suspend fun create(request: CustomerRequest): Result<Customer> = TODO()
    override suspend fun update(customerId: String, request: CustomerRequest): Result<Customer> = TODO()
    override suspend fun accountHolderName(bankId: String, accountId: String): Result<String> = TODO()
}

class StandingOrdersViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(
        repo: FakeStandingOrdersRepository = FakeStandingOrdersRepository(),
        accounts: FakeAccountsRepository = FakeAccountsRepository(),
        payments: FakePaymentsRepository = FakePaymentsRepository(),
    ) = StandingOrdersViewModel(
        standingOrdersRepository = repo,
        accountsRepository = accounts,
        paymentsRepository = payments,
        profileRepository = FakeProfileRepository(),
        customersRepository = FakeCustomersRepository(),
    )

    @Test
    fun load_success_emitsContentWithStats() = runTest(dispatcher) {
        val model = vm(
            repo = FakeStandingOrdersRepository(
                orders = Result.success(
                    listOf(
                        order("Rent"),
                        order("Netflix"),
                        order("Gym", status = StandingOrder.STATUS_PAUSED),
                    ),
                ),
            ),
        )
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        val state = model.uiState.value
        assertTrue(state is ScreenState.Content)
        assertEquals(3, state.data.orders.size)
        assertEquals(2, state.data.activeCount)
        assertEquals(1, state.data.pausedCount)
        assertEquals("€90.00", state.data.monthlyTotal)
    }

    @Test
    fun filter_pausedShowsOnlyPausedRows() = runTest(dispatcher) {
        val model = vm(
            repo = FakeStandingOrdersRepository(
                orders = Result.success(
                    listOf(
                        order("Rent"),
                        order("Gym", status = StandingOrder.STATUS_PAUSED),
                        order("Old loan", status = StandingOrder.STATUS_CANCELLED),
                    ),
                ),
            ),
        )
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        model.onFilterChanged(StandingOrderFilter.Paused)
        advanceUntilIdle()
        val state = model.uiState.value as ScreenState.Content
        assertEquals(listOf("Gym"), state.data.orders.map { it.name })
        assertEquals(StandingOrderFilter.Paused, state.data.filter)
        // Stats stay global while filtering.
        assertEquals(1, state.data.activeCount)
        assertEquals(1, state.data.pausedCount)
    }

    @Test
    fun load_noOrders_emitsEmpty() = runTest(dispatcher) {
        val model = vm()
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(model.uiState.value is ScreenState.Empty)
    }

    @Test
    fun load_failure_emitsError() = runTest(dispatcher) {
        val model = vm(
            repo = FakeStandingOrdersRepository(orders = Result.failure(RuntimeException("boom"))),
        )
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(model.uiState.value is ScreenState.Error)
    }

    @Test
    fun onCreateClicked_loadsPayeesIntoSheet() = runTest(dispatcher) {
        val model = vm()
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        model.onCreateClicked()
        advanceUntilIdle()
        val sheet = model.createSheet.value
        assertTrue(sheet.visible)
        assertFalse(sheet.loadingPayees)
        assertEquals(listOf("Savings Counterparty"), sheet.payees.map { it.name })
    }

    @Test
    fun onSubmitCreate_success_buildsRequestAndRefreshes() = runTest(dispatcher) {
        val repo = FakeStandingOrdersRepository()
        val model = vm(repo = repo)
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        model.onCreateClicked()
        advanceUntilIdle()

        repo.orders = Result.success(listOf(order("Savings Counterparty")))
        model.onSubmitCreate(counterpartyId = "cp-1", amount = "25.00", frequency = "MONTHLY")
        advanceUntilIdle()

        val request = repo.lastCreateRequest
        assertNotNull(request)
        assertEquals("cust-1", request.customerId)
        assertEquals("user-1", request.userId)
        assertEquals("cp-1", request.counterpartyId)
        assertEquals("25.00", request.amount.amount)
        assertEquals("EUR", request.amount.currency)
        assertEquals("MONTHLY", request.`when`.frequency)
        assertFalse(model.createSheet.value.visible)
        val state = model.uiState.value
        assertTrue(state is ScreenState.Content)
        assertEquals(listOf("Savings Counterparty"), state.data.orders.map { it.name })
    }

    @Test
    fun onSubmitCreate_invalidAmount_setsErrorWithoutCalling() = runTest(dispatcher) {
        val repo = FakeStandingOrdersRepository()
        val model = vm(repo = repo)
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        model.onCreateClicked()
        advanceUntilIdle()

        model.onSubmitCreate(counterpartyId = "cp-1", amount = "abc", frequency = "MONTHLY")
        advanceUntilIdle()

        assertEquals("Enter a valid amount", model.createSheet.value.error)
        assertEquals(null, repo.lastCreateRequest)
    }

    @Test
    fun onSubmitCreate_failure_keepsSheetWithError() = runTest(dispatcher) {
        val repo = FakeStandingOrdersRepository(createResult = Result.failure(RuntimeException("denied")))
        val model = vm(repo = repo)
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        model.onCreateClicked()
        advanceUntilIdle()

        model.onSubmitCreate(counterpartyId = "cp-1", amount = "25.00", frequency = "MONTHLY")
        advanceUntilIdle()

        val sheet = model.createSheet.value
        assertTrue(sheet.visible)
        assertEquals("denied", sheet.error)
        assertFalse(sheet.submitting)
    }
}
