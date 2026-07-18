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
import kotlinx.datetime.LocalDate
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
import org.mifosx.openbanking.core.model.obp.StandingOrderDetail
import org.mifosx.openbanking.core.model.obp.TransactionRequest
import org.mifosx.openbanking.core.model.obp.TransactionRequestSummary
import org.mifosx.openbanking.core.model.obp.UserProfile
import template.core.base.store.screen.ScreenDataStream
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val TODAY = LocalDate(2026, 6, 5)

private fun checkingAccount(id: String = "ac.checking.001") = Account(
    id = id,
    bankId = "ac.bank.uk",
    accountType = "checking",
    balance = AmountOfMoney(currency = "EUR", amount = "1000.00"),
)

private fun payee(id: String = "cp-1", name: String = "Savings Counterparty") = Counterparty(
    counterpartyId = id,
    name = name,
    isBeneficiary = true,
)

private class CreateFakeStandingOrdersRepository(
    var createResult: Result<StandingOrder> = Result.success(StandingOrder(id = "so-1")),
) : StandingOrdersRepository {
    var lastCreateRequest: CreateStandingOrderRequest? = null
    override suspend fun detail(
        bankId: String,
        accountId: String,
        standingOrderId: String,
    ): Result<StandingOrderDetail> = TODO("not used")
    override suspend fun listRecurring(bankId: String, accountId: String): Result<List<StandingOrder>> =
        Result.success(emptyList())
    override suspend fun create(
        bankId: String,
        accountId: String,
        name: String,
        request: CreateStandingOrderRequest,
    ): Result<StandingOrder> {
        lastCreateRequest = request
        return createResult
    }
}

private class CreateFakeAccountsRepository(
    var accounts: Result<List<Account>> = Result.success(listOf(checkingAccount())),
) : AccountsRepository {
    override fun accountsStream(scope: CoroutineScope): ScreenDataStream<List<Account>> = TODO()
    override suspend fun listAccounts(): Result<List<Account>> = accounts
    override suspend fun myAccounts(): Result<List<Account>> = accounts
    override suspend fun accountDetail(bankId: String, accountId: String): Result<Account> = TODO()
}

private class CreateFakePaymentsRepository(
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
    override suspend fun sendToSandboxTan(
        bankId: String,
        accountId: String,
        toBankId: String,
        toAccountId: String,
        amount: String,
        currency: String,
        reference: String,
    ): Result<TransactionRequest> = TODO()
    override suspend fun answerChallenge(
        bankId: String,
        accountId: String,
        type: String,
        requestId: String,
        challengeId: String,
        answer: String,
    ): Result<TransactionRequest> = TODO()
}

private class CreateFakeProfileRepository : ProfileRepository {
    override suspend fun current(): Result<UserProfile> = Result.success(UserProfile(userId = "user-1"))
    override suspend fun update(request: ProfileUpdateRequest): Result<UserProfile> = TODO()
}

private class CreateFakeCustomersRepository : CustomersRepository {
    override fun customersStream(scope: CoroutineScope): ScreenDataStream<List<Customer>> = TODO()
    override suspend fun list(): Result<List<Customer>> = TODO()
    override suspend fun currentUserCustomers(): Result<List<Customer>> =
        Result.success(listOf(Customer(customerId = "cust-1", bankId = "ac.bank.uk")))
    override suspend fun get(customerId: String): Result<Customer> = TODO()
    override suspend fun create(request: CustomerRequest): Result<Customer> = TODO()
    override suspend fun update(customerId: String, request: CustomerRequest): Result<Customer> = TODO()
    override suspend fun accountHolderName(bankId: String, accountId: String): Result<String> = TODO()
}

class CreateStandingOrderViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(
        repo: CreateFakeStandingOrdersRepository = CreateFakeStandingOrdersRepository(),
        payments: CreateFakePaymentsRepository = CreateFakePaymentsRepository(),
        accounts: CreateFakeAccountsRepository = CreateFakeAccountsRepository(),
        initialAccountId: String = "",
    ) = CreateStandingOrderViewModel(
        standingOrdersRepository = repo,
        accountsRepository = accounts,
        paymentsRepository = payments,
        profileRepository = CreateFakeProfileRepository(),
        customersRepository = CreateFakeCustomersRepository(),
        initialAccountId = initialAccountId,
        todayProvider = { TODAY },
    )

    private fun CreateStandingOrderViewModel.fillValidForm() {
        onPayeeSelected("cp-1")
        onAmountChanged("25.00")
        onFrequencySelected("WEEKLY")
        onStartDateSelected(LocalDate(2026, 6, 8))
    }

    @Test
    fun init_loadsPayees() = runTest(dispatcher) {
        val model = vm()
        backgroundScope.launch { model.form.collect {} }
        advanceUntilIdle()
        assertFalse(model.form.value.loadingPayees)
        assertEquals(listOf("Savings Counterparty"), model.form.value.payees.map { it.name })
        assertEquals("ac.checking.001", model.form.value.selectedAccountId)
    }

    @Test
    fun init_prefersTheAccountChosenOnTheListScreen() = runTest(dispatcher) {
        val model = vm(
            accounts = CreateFakeAccountsRepository(
                accounts = Result.success(listOf(checkingAccount(), checkingAccount(id = "ac.savings.001"))),
            ),
            initialAccountId = "ac.savings.001",
        )
        backgroundScope.launch { model.form.collect {} }
        advanceUntilIdle()
        assertEquals("ac.savings.001", model.form.value.selectedAccountId)
    }

    @Test
    fun sourceAccountSwitch_reloadsPayees_andClearsStaleSelection() = runTest(dispatcher) {
        val payments = CreateFakePaymentsRepository()
        val model = vm(
            payments = payments,
            accounts = CreateFakeAccountsRepository(
                accounts = Result.success(listOf(checkingAccount(), checkingAccount(id = "ac.savings.001"))),
            ),
        )
        backgroundScope.launch { model.form.collect {} }
        advanceUntilIdle()
        model.onPayeeSelected("cp-1")

        payments.beneficiaries = Result.success(listOf(payee(id = "cp-9", name = "Gym Membership")))
        model.onSourceAccountSelected(checkingAccount(id = "ac.savings.001"))
        advanceUntilIdle()

        val form = model.form.value
        assertEquals("ac.savings.001", form.selectedAccountId)
        assertEquals(listOf("cp-9"), form.payees.map { it.counterpartyId })
        assertEquals("", form.selectedPayeeId)
        assertFalse(form.loadingPayees)
    }

    @Test
    fun sourceAccountSwitch_keepsPayee_whenNewAccountAlsoOwnsIt() = runTest(dispatcher) {
        val model = vm(
            accounts = CreateFakeAccountsRepository(
                accounts = Result.success(listOf(checkingAccount(), checkingAccount(id = "ac.savings.001"))),
            ),
        )
        backgroundScope.launch { model.form.collect {} }
        advanceUntilIdle()
        model.onPayeeSelected("cp-1")

        model.onSourceAccountSelected(checkingAccount(id = "ac.savings.001"))
        advanceUntilIdle()

        assertEquals("cp-1", model.form.value.selectedPayeeId)
    }

    @Test
    fun submit_invalidAmount_setsErrorWithoutCalling() = runTest(dispatcher) {
        val repo = CreateFakeStandingOrdersRepository()
        val model = vm(repo = repo)
        backgroundScope.launch { model.form.collect {} }
        advanceUntilIdle()
        model.fillValidForm()
        model.onAmountChanged("abc")
        model.onSubmit()
        advanceUntilIdle()
        assertEquals("Enter a valid amount", model.form.value.error)
        assertNull(repo.lastCreateRequest)
    }

    @Test
    fun submit_missingStartDate_setsError() = runTest(dispatcher) {
        val model = vm()
        backgroundScope.launch { model.form.collect {} }
        advanceUntilIdle()
        model.onPayeeSelected("cp-1")
        model.onAmountChanged("25.00")
        model.onSubmit()
        advanceUntilIdle()
        assertEquals("Choose a start date", model.form.value.error)
    }

    @Test
    fun submit_startDateNotInFuture_setsError() = runTest(dispatcher) {
        val model = vm()
        backgroundScope.launch { model.form.collect {} }
        advanceUntilIdle()
        model.fillValidForm()
        model.onStartDateSelected(TODAY)
        model.onSubmit()
        advanceUntilIdle()
        assertEquals("Start date must be after today", model.form.value.error)
    }

    @Test
    fun submit_endDateBeforeStart_setsError() = runTest(dispatcher) {
        val model = vm()
        backgroundScope.launch { model.form.collect {} }
        advanceUntilIdle()
        model.fillValidForm()
        model.onEndDateSelected(LocalDate(2026, 6, 7))
        model.onSubmit()
        advanceUntilIdle()
        assertEquals("End date must be after the start date", model.form.value.error)
    }

    @Test
    fun submit_success_buildsRequestWithDatesAndSignalsCreated() = runTest(dispatcher) {
        val repo = CreateFakeStandingOrdersRepository()
        val model = vm(repo = repo)
        backgroundScope.launch { model.form.collect {} }
        advanceUntilIdle()
        model.fillValidForm()
        model.onEndDateSelected(LocalDate(2027, 6, 8))
        model.onSubmit()
        advanceUntilIdle()

        val request = repo.lastCreateRequest
        assertNotNull(request)
        assertEquals("cust-1", request.customerId)
        assertEquals("user-1", request.userId)
        assertEquals("cp-1", request.counterpartyId)
        assertEquals("25.00", request.amount.amount)
        assertEquals("EUR", request.amount.currency)
        assertEquals("WEEKLY", request.`when`.frequency)
        assertEquals("2026-06-08T00:00:00Z", request.dateStarts)
        assertEquals("2027-06-08T00:00:00Z", request.dateExpires)
        assertTrue(model.form.value.created)
    }

    @Test
    fun submit_failure_keepsFormWithError() = runTest(dispatcher) {
        val repo = CreateFakeStandingOrdersRepository(createResult = Result.failure(RuntimeException("denied")))
        val model = vm(repo = repo)
        backgroundScope.launch { model.form.collect {} }
        advanceUntilIdle()
        model.fillValidForm()
        model.onSubmit()
        advanceUntilIdle()
        assertEquals("denied", model.form.value.error)
        assertFalse(model.form.value.submitting)
        assertFalse(model.form.value.created)
    }

    @Test
    fun recurrenceHint_perFrequency() {
        val monday = LocalDate(2026, 6, 8)
        assertEquals("Repeats every day", recurrenceHint("DAILY", monday))
        assertEquals("Repeats every Monday", recurrenceHint("WEEKLY", monday))
        assertEquals("Repeats every other Monday", recurrenceHint("BI-WEEKLY", monday))
        assertEquals("Repeats on the 8th of each month", recurrenceHint("MONTHLY", monday))
        assertEquals("Repeats every 8 June", recurrenceHint("YEARLY", monday))
        assertEquals("", recurrenceHint("WEEKLY", null))
    }

    @Test
    fun recurrenceHint_monthlyOrdinals() {
        assertEquals("Repeats on the 1st of each month", recurrenceHint("MONTHLY", LocalDate(2026, 7, 1)))
        assertEquals("Repeats on the 2nd of each month", recurrenceHint("MONTHLY", LocalDate(2026, 7, 2)))
        assertEquals("Repeats on the 3rd of each month", recurrenceHint("MONTHLY", LocalDate(2026, 7, 3)))
        assertEquals("Repeats on the 11th of each month", recurrenceHint("MONTHLY", LocalDate(2026, 7, 11)))
        assertEquals("Repeats on the 21st of each month", recurrenceHint("MONTHLY", LocalDate(2026, 7, 21)))
        assertEquals("Repeats on the 31st of each month", recurrenceHint("MONTHLY", LocalDate(2026, 7, 31)))
    }
}
