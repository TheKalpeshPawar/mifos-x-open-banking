/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.transactions.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.core.data.payments.PaymentsRepository
import org.mifosx.openbanking.core.data.transactions.CounterpartyNameResolver
import org.mifosx.openbanking.core.data.transactions.TransactionsRepository
import org.mifosx.openbanking.core.model.obp.AmountOfMoney
import org.mifosx.openbanking.core.model.obp.Counterparty
import org.mifosx.openbanking.core.model.obp.CounterpartyHolder
import org.mifosx.openbanking.core.model.obp.Transaction
import org.mifosx.openbanking.core.model.obp.TransactionAccount
import org.mifosx.openbanking.core.model.obp.TransactionAttribute
import org.mifosx.openbanking.core.model.obp.TransactionCounterparty
import org.mifosx.openbanking.core.model.obp.TransactionDetails
import org.mifosx.openbanking.core.model.obp.TransactionMetadata
import org.mifosx.openbanking.core.model.obp.TransactionRequest
import org.mifosx.openbanking.core.model.obp.TransactionRequestDetails
import org.mifosx.openbanking.core.model.obp.TransactionRequestSummary
import org.mifosx.openbanking.core.model.obp.TransactionRequestToSepa
import template.core.base.store.screen.ScreenDataStream
import template.core.base.store.screen.ScreenState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun detailTxn(
    id: String = "tx-1",
    amount: String = "-42.50",
    currency: String = "EUR",
    completed: String = "2026-05-25T14:32:00Z",
    description: String = "Weekly groceries",
    holder: String = "Tesco Supermarket",
    type: String = "SEPA",
    typeCode: String? = "POS",
    narrative: String = "",
) = Transaction(
    id = id,
    thisAccount = TransactionAccount(id = "ac.checking.0130", bankId = "ac.bank.uk", label = "Main Checking"),
    otherAccount = TransactionCounterparty(holder = CounterpartyHolder(name = holder)),
    details = TransactionDetails(
        type = type,
        description = description,
        completed = completed,
        value = AmountOfMoney(currency = currency, amount = amount),
        newBalance = AmountOfMoney(currency = currency, amount = "1067.50"),
    ),
    metadata = TransactionMetadata(narrative = narrative),
    transactionAttributes = typeCode?.let { listOf(TransactionAttribute("TXN_TYPE", "STRING", it)) }.orEmpty(),
)

private class DetailFakeTransactionsRepository(
    var result: Result<Transaction> = Result.success(detailTxn()),
) : TransactionsRepository {
    var calls = 0
    override fun transactionsStream(
        bankId: String,
        accountId: String,
        scope: CoroutineScope,
    ): ScreenDataStream<List<Transaction>> = TODO()
    override suspend fun listTransactions(
        bankId: String,
        accountId: String,
        limit: Int?,
    ): Result<List<Transaction>> = TODO()
    override suspend fun listTransactionsWithAttributes(
        bankId: String,
        accountId: String,
        limit: Int?,
    ): Result<List<Transaction>> = TODO()
    override suspend fun getTransaction(
        bankId: String,
        accountId: String,
        transactionId: String,
    ): Result<Transaction> {
        calls++
        return result
    }
}

private fun detailRequest(
    id: String = "req-1",
    amount: String = "1500.00",
    description: String = "Sofa deposit — Habitat Furniture",
    type: String = "SEPA",
    startDate: String = "2026-06-06T09:15:00Z",
    iban: String = "DE89370400440532013000",
) = TransactionRequestSummary(
    id = id,
    type = type,
    status = "INITIATED",
    details = TransactionRequestDetails(
        toSepa = TransactionRequestToSepa(iban = iban),
        value = AmountOfMoney(currency = "EUR", amount = amount),
        description = description,
    ),
    transactionIds = listOf(""),
    startDate = startDate,
)

private class DetailFakePaymentsRepository(
    var requests: Result<List<TransactionRequestSummary>> = Result.success(listOf(detailRequest())),
) : PaymentsRepository {
    var calls = 0
    override fun beneficiariesStream(
        accountId: String,
        scope: CoroutineScope,
    ): ScreenDataStream<List<Counterparty>> = TODO()
    override suspend fun listBeneficiaries(bankId: String, accountId: String): Result<List<Counterparty>> = TODO()
    override suspend fun listTransactionRequests(
        bankId: String,
        accountId: String,
    ): Result<List<TransactionRequestSummary>> {
        calls++
        return requests
    }
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

private class DetailFakeCounterpartyNameResolver(
    private val names: Map<String, String> = emptyMap(),
    private val placeholder: String = "afternooncoffee",
) : CounterpartyNameResolver {
    override suspend fun resolve(
        bankId: String,
        accountId: String,
        transactions: List<Transaction>,
    ): Map<String, String> = names

    override suspend fun placeholderHolder(): String = placeholder
}

class TransactionDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(
        repository: DetailFakeTransactionsRepository = DetailFakeTransactionsRepository(),
        paymentsRepository: DetailFakePaymentsRepository = DetailFakePaymentsRepository(),
        transactionId: String = "tx-1",
        requestId: String = "",
        counterpartyNames: Map<String, String> = emptyMap(),
    ) = TransactionDetailViewModel(
        transactionsRepository = repository,
        paymentsRepository = paymentsRepository,
        counterpartyNameResolver = DetailFakeCounterpartyNameResolver(counterpartyNames),
        bankId = "ac.bank.uk",
        accountId = "ac.checking.0130",
        transactionId = transactionId,
        requestId = requestId,
    )

    private suspend fun TestScope.content(model: TransactionDetailViewModel): TransactionDetailContent {
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        val s = model.uiState.value
        assertTrue(s is ScreenState.Content, "expected Content, was $s")
        return s.data
    }

    @Test
    fun load_derivesDisplayFields() = runTest(dispatcher) {
        val c = content(vm(DetailFakeTransactionsRepository(Result.success(detailTxn()))))
        assertEquals("-€42.50", c.amount)
        assertTrue(c.isDebit)
        assertEquals("Tesco Supermarket", c.counterpartyName)
        assertEquals("Card payment", c.category)
        assertEquals("25 May 2026, 14:32", c.dateTime)
        assertEquals("tx-1", c.reference)
        assertEquals("Main Checking", c.fromAccountLabel)
        assertEquals("...0130", c.fromAccountTail)
        assertFalse(c.isPending)
        assertEquals("Completed", c.statusLabel)
    }

    @Test
    fun load_creditFormatsWithPlusSign() = runTest(dispatcher) {
        val c = content(
            vm(
                DetailFakeTransactionsRepository(
                    Result.success(detailTxn(amount = "3200.00", currency = "GBP", typeCode = "SAL")),
                ),
            ),
        )
        assertEquals("+£3200.00", c.amount)
        assertFalse(c.isDebit)
        assertEquals("Salary", c.category)
    }

    @Test
    fun load_blankHolderFallsBackToDescription() = runTest(dispatcher) {
        val c = content(
            vm(DetailFakeTransactionsRepository(Result.success(detailTxn(holder = "")))),
        )
        assertEquals("Weekly groceries", c.counterpartyName)
    }

    @Test
    fun load_placeholderHolderResolvesToDestinationHolderName() = runTest(dispatcher) {
        val transfer = detailTxn(holder = "afternooncoffee", description = "Monthly savings transfer")
            .let { it.copy(otherAccount = it.otherAccount.copy(id = "obf-savings")) }
        val c = content(
            vm(
                DetailFakeTransactionsRepository(Result.success(transfer)),
                counterpartyNames = mapOf("obf-savings" to "Alice Johnson"),
            ),
        )
        assertEquals("Alice Johnson", c.counterpartyName)
    }

    @Test
    fun load_unresolvedPlaceholderFallsBackToDescriptionNeverUsername() = runTest(dispatcher) {
        val transfer = detailTxn(holder = "afternooncoffee", description = "Netflix Subscription")
        val c = content(vm(DetailFakeTransactionsRepository(Result.success(transfer))))
        assertEquals("Netflix Subscription", c.counterpartyName)
    }

    @Test
    fun load_unparseableDateFallsBackToRawTimestamp() = runTest(dispatcher) {
        val c = content(
            vm(DetailFakeTransactionsRepository(Result.success(detailTxn(completed = "")))),
        )
        assertEquals("", c.dateTime)
    }

    @Test
    fun load_failureIsError() = runTest(dispatcher) {
        val model = vm(DetailFakeTransactionsRepository(Result.failure(RuntimeException("boom"))))
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(model.uiState.value is ScreenState.Error)
    }

    @Test
    fun retry_afterFailureLoadsContent() = runTest(dispatcher) {
        val repository = DetailFakeTransactionsRepository(Result.failure(RuntimeException("boom")))
        val model = vm(repository)
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(model.uiState.value is ScreenState.Error)

        repository.result = Result.success(detailTxn())
        model.onRetry()
        advanceUntilIdle()
        assertTrue(model.uiState.value is ScreenState.Content)
        assertEquals(2, repository.calls)
    }

    @Test
    fun blankIds_isEmptyWithoutApiCall() = runTest(dispatcher) {
        val repository = DetailFakeTransactionsRepository()
        val payments = DetailFakePaymentsRepository()
        val model = vm(repository, payments, transactionId = "", requestId = "")
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(model.uiState.value is ScreenState.Empty)
        assertEquals(0, repository.calls)
        assertEquals(0, payments.calls)
    }

    @Test
    fun pendingLoad_derivesPendingContent() = runTest(dispatcher) {
        val c = content(vm(transactionId = "", requestId = "req-1"))
        assertEquals("€1500.00", c.amount)
        assertTrue(c.isDebit)
        assertTrue(c.isPending)
        assertEquals("Awaiting confirmation", c.statusLabel)
        assertEquals("Sofa deposit — Habitat Furniture", c.counterpartyName)
        assertEquals("Pending", c.category)
        assertEquals("6 June 2026, 09:15", c.dateTime)
        assertEquals("req-1", c.reference)
        assertEquals("SEPA Credit Transfer", c.typeLabel)
        assertEquals("...0130", c.fromAccountTail)
        assertEquals("DE89370400440532013000", c.toDetail)
    }

    @Test
    fun pendingLoad_requestNotFoundIsError() = runTest(dispatcher) {
        val payments = DetailFakePaymentsRepository(Result.success(emptyList()))
        val model = vm(paymentsRepository = payments, transactionId = "", requestId = "req-404")
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(model.uiState.value is ScreenState.Error)
    }

    @Test
    fun pendingLoad_listFailureIsError() = runTest(dispatcher) {
        val payments = DetailFakePaymentsRepository(Result.failure(RuntimeException("boom")))
        val model = vm(paymentsRepository = payments, transactionId = "", requestId = "req-1")
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(model.uiState.value is ScreenState.Error)
    }

    @Test
    fun pendingLoad_bookedPathNotCalled() = runTest(dispatcher) {
        val repository = DetailFakeTransactionsRepository()
        val payments = DetailFakePaymentsRepository()
        val model = vm(repository, payments, transactionId = "", requestId = "req-1")
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(0, repository.calls)
        assertEquals(1, payments.calls)
    }
}
