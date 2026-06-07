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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import org.mifosx.openbanking.core.data.accounts.AccountsRepository
import org.mifosx.openbanking.core.data.payments.PaymentsRepository
import org.mifosx.openbanking.core.data.transactions.CounterpartyNameResolver
import org.mifosx.openbanking.core.data.transactions.TransactionsRepository
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.core.model.obp.AmountOfMoney
import org.mifosx.openbanking.core.model.obp.Counterparty
import org.mifosx.openbanking.core.model.obp.CounterpartyHolder
import org.mifosx.openbanking.core.model.obp.Transaction
import org.mifosx.openbanking.core.model.obp.TransactionAttribute
import org.mifosx.openbanking.core.model.obp.TransactionCounterparty
import org.mifosx.openbanking.core.model.obp.TransactionDetails
import org.mifosx.openbanking.core.model.obp.TransactionRequest
import org.mifosx.openbanking.core.model.obp.TransactionRequestDetails
import org.mifosx.openbanking.core.model.obp.TransactionRequestSummary
import template.core.base.store.screen.ScreenDataStream
import template.core.base.store.screen.ScreenState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val TODAY = LocalDate(2026, 6, 6)

private fun txn(
    id: String,
    amount: String,
    date: String,
    description: String = "Txn $id",
    holder: String = "Acme Ltd",
    typeCode: String? = "POS",
    otherId: String = "",
) = Transaction(
    id = id,
    otherAccount = TransactionCounterparty(id = otherId, holder = CounterpartyHolder(name = holder)),
    details = TransactionDetails(
        description = description,
        completed = "${date}T00:00:00Z",
        value = AmountOfMoney(currency = "EUR", amount = amount),
    ),
    transactionAttributes = typeCode?.let { listOf(TransactionAttribute("TXN_TYPE", "STRING", it)) }.orEmpty(),
)

private fun pendingRequest(
    id: String,
    amount: String = "1500.00",
    description: String = "Sofa deposit",
    date: String = "2026-06-06",
    status: String = "INITIATED",
    transactionIds: List<String> = listOf(""),
) = TransactionRequestSummary(
    id = id,
    type = "SANDBOX_TAN",
    status = status,
    details = TransactionRequestDetails(
        value = AmountOfMoney(currency = "EUR", amount = amount),
        description = description,
    ),
    transactionIds = transactionIds,
    startDate = "${date}T00:00:00Z",
)

private class TxnFakeTransactionsRepository(
    var result: Result<List<Transaction>> = Result.success(emptyList()),
) : TransactionsRepository {
    override fun transactionsStream(
        bankId: String,
        accountId: String,
        scope: CoroutineScope,
    ): ScreenDataStream<List<Transaction>> = TODO()
    override suspend fun listTransactions(
        bankId: String,
        accountId: String,
        limit: Int?,
    ): Result<List<Transaction>> = result
    override suspend fun listTransactionsWithAttributes(
        bankId: String,
        accountId: String,
        limit: Int?,
    ): Result<List<Transaction>> = result
    override suspend fun getTransaction(
        bankId: String,
        accountId: String,
        transactionId: String,
    ): Result<Transaction> = TODO()
}

private class TxnFakePaymentsRepository(
    var requests: Result<List<TransactionRequestSummary>> = Result.success(emptyList()),
) : PaymentsRepository {
    override fun beneficiariesStream(
        accountId: String,
        scope: CoroutineScope,
    ): ScreenDataStream<List<Counterparty>> = TODO()
    override suspend fun listBeneficiaries(bankId: String, accountId: String): Result<List<Counterparty>> =
        Result.success(emptyList())
    override suspend fun listTransactionRequests(
        bankId: String,
        accountId: String,
    ): Result<List<TransactionRequestSummary>> = requests
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
    ): Result<Boolean> = Result.success(true)
}

private class TxnFakeAccountsRepository(
    var account: Result<Account> = Result.success(
        Account(id = "ac.checking.001", bankId = "ac.bank.uk", balance = AmountOfMoney("EUR", "1000")),
    ),
) : AccountsRepository {
    override fun accountsStream(scope: CoroutineScope): ScreenDataStream<List<Account>> = TODO()
    override suspend fun listAccounts(): Result<List<Account>> = TODO()
    override suspend fun myAccounts(): Result<List<Account>> = TODO()
    override suspend fun accountDetail(bankId: String, accountId: String): Result<Account> = account
}

private class TxnFakeCounterpartyNameResolver(
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

class TransactionsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(
        transactions: List<Transaction> = emptyList(),
        requests: List<TransactionRequestSummary> = emptyList(),
        transactionsResult: Result<List<Transaction>>? = null,
        counterpartyNames: Map<String, String> = emptyMap(),
    ) = TransactionsViewModel(
        transactionsRepository = TxnFakeTransactionsRepository(
            transactionsResult ?: Result.success(transactions),
        ),
        paymentsRepository = TxnFakePaymentsRepository(Result.success(requests)),
        accountsRepository = TxnFakeAccountsRepository(),
        counterpartyNameResolver = TxnFakeCounterpartyNameResolver(counterpartyNames),
        bankId = "ac.bank.uk",
        accountId = "ac.checking.001",
        todayProvider = { TODAY },
    )

    private suspend fun kotlinx.coroutines.test.TestScope.content(
        model: TransactionsViewModel,
    ): TransactionsContent {
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        val s = model.uiState.value
        assertTrue(s is ScreenState.Content, "expected Content, was $s")
        return s.data
    }

    @Test
    fun load_groupsTransactionsByDateDescending() = runTest(dispatcher) {
        val c = content(
            vm(
                transactions = listOf(
                    txn("t1", "-42.50", "2026-06-04"),
                    txn("t2", "3200.00", "2026-06-05"),
                    txn("t3", "-12.00", "2026-06-04"),
                ),
            ),
        )
        assertEquals(listOf(LocalDate(2026, 6, 5), LocalDate(2026, 6, 4)), c.groups.map { it.date })
        assertEquals(2, c.groups[1].transactions.size)
    }

    @Test
    fun summary_currentMonthOnly_excludesPending() = runTest(dispatcher) {
        val c = content(
            vm(
                transactions = listOf(
                    txn("t1", "-100.00", "2026-06-02"),
                    // t2 lands in the previous month — excluded from the summary
                    txn("t2", "-50.00", "2026-05-28"),
                    txn("t3", "3200.00", "2026-06-01"),
                ),
                requests = listOf(pendingRequest("p1", amount = "1500.00")),
            ),
        )
        assertEquals("100.00", c.summary.spent)
        assertEquals("3200.00", c.summary.received)
        assertEquals("EUR", c.summary.currency)
    }

    @Test
    fun pending_onlyInitiatedWithoutBookedTransactions() = runTest(dispatcher) {
        val c = content(
            vm(
                requests = listOf(
                    pendingRequest("p1"),
                    pendingRequest("p2", status = "COMPLETED", transactionIds = listOf("tx-9")),
                    pendingRequest("p3", status = "INITIATED", transactionIds = listOf("tx-8")),
                ),
            ),
        )
        assertEquals(listOf("p1"), c.pending.map { it.id })
    }

    @Test
    fun filter_debit_showsOnlyNegativeAmounts() = runTest(dispatcher) {
        val model = vm(
            transactions = listOf(
                txn("t1", "-42.50", "2026-06-04"),
                txn("t2", "3200.00", "2026-06-05"),
            ),
            requests = listOf(pendingRequest("p1")),
        )
        val before = content(model)
        assertEquals(1, before.pending.size)
        model.onFilterChanged(TransactionTypeFilter.DEBIT)
        advanceUntilIdle()
        val c = (model.uiState.value as ScreenState.Content).data
        assertEquals(listOf("t1"), c.groups.flatMap { it.transactions }.map { it.txId })
        assertTrue(c.pending.isEmpty())
    }

    @Test
    fun filter_pending_showsOnlyPendingSection() = runTest(dispatcher) {
        val model = vm(
            transactions = listOf(txn("t1", "-42.50", "2026-06-04")),
            requests = listOf(pendingRequest("p1")),
        )
        content(model)
        model.onFilterChanged(TransactionTypeFilter.PENDING)
        advanceUntilIdle()
        val c = (model.uiState.value as ScreenState.Content).data
        assertTrue(c.groups.isEmpty())
        assertEquals(listOf("p1"), c.pending.map { it.id })
    }

    @Test
    fun search_matchesDescriptionHolderAndAmount() = runTest(dispatcher) {
        val model = vm(
            transactions = listOf(
                txn("t1", "-42.50", "2026-06-04", description = "Grocery run", holder = "Tesco"),
                txn("t2", "-94.20", "2026-06-03", description = "Energy bill", holder = "EDF"),
            ),
        )
        content(model)
        model.onQueryChanged("tesco")
        advanceUntilIdle()
        var c = (model.uiState.value as ScreenState.Content).data
        assertEquals(listOf("t1"), c.groups.flatMap { it.transactions }.map { it.txId })

        model.onQueryChanged("94.20")
        advanceUntilIdle()
        c = (model.uiState.value as ScreenState.Content).data
        assertEquals(listOf("t2"), c.groups.flatMap { it.transactions }.map { it.txId })
    }

    @Test
    fun search_matchesResolvedCounterpartyButNeverThePlaceholder() = runTest(dispatcher) {
        val model = vm(
            transactions = listOf(
                txn(
                    "t1",
                    "-200.00",
                    "2026-06-04",
                    description = "Monthly transfer",
                    holder = "afternooncoffee",
                    otherId = "obf-alice",
                ),
                txn(
                    "t2",
                    "-9.99",
                    "2026-06-03",
                    description = "Subscription",
                    holder = "afternooncoffee",
                    otherId = "obf-ext",
                ),
            ),
            counterpartyNames = mapOf("obf-alice" to "Alice Johnson"),
        )
        content(model)
        model.onQueryChanged("alice")
        advanceUntilIdle()
        val c = (model.uiState.value as ScreenState.Content).data
        assertEquals(listOf("t1"), c.groups.flatMap { it.transactions }.map { it.txId })

        model.onQueryChanged("afternooncoffee")
        advanceUntilIdle()
        assertTrue(model.uiState.value is ScreenState.Empty)
    }

    @Test
    fun pagination_tenPerPage_loadMoreAppends() = runTest(dispatcher) {
        val many = (1..25).map { i ->
            txn("t$i", "-1.00", "2026-06-0${(i % 5) + 1}")
        }
        val model = vm(transactions = many)
        var c = content(model)
        assertEquals(10, c.groups.sumOf { it.transactions.size })
        assertTrue(c.hasMore)

        model.onLoadMore()
        advanceUntilIdle()
        c = (model.uiState.value as ScreenState.Content).data
        assertEquals(20, c.groups.sumOf { it.transactions.size })
        assertTrue(c.hasMore)

        model.onLoadMore()
        advanceUntilIdle()
        c = (model.uiState.value as ScreenState.Content).data
        assertEquals(25, c.groups.sumOf { it.transactions.size })
        assertFalse(c.hasMore)
    }

    @Test
    fun filterChange_resetsPagination() = runTest(dispatcher) {
        val many = (1..25).map { i -> txn("t$i", "-1.00", "2026-06-01") }
        val model = vm(transactions = many)
        content(model)
        model.onLoadMore()
        advanceUntilIdle()
        model.onFilterChanged(TransactionTypeFilter.DEBIT)
        advanceUntilIdle()
        val c = (model.uiState.value as ScreenState.Content).data
        assertEquals(10, c.groups.sumOf { it.transactions.size })
    }

    @Test
    fun dateRange_presetLast7DaysExcludesOlder() = runTest(dispatcher) {
        val model = vm(
            transactions = listOf(
                txn("recent", "-5.00", "2026-06-03"),
                txn("old", "-5.00", "2026-05-20"),
            ),
        )
        content(model)
        model.onRangePresetSelected(DateRangePreset.LAST_7_DAYS)
        advanceUntilIdle()
        val c = (model.uiState.value as ScreenState.Content).data
        assertEquals(listOf("recent"), c.groups.flatMap { it.transactions }.map { it.txId })
    }

    @Test
    fun dateRange_customBoundsApplied() = runTest(dispatcher) {
        val model = vm(
            transactions = listOf(
                txn("inRange", "-5.00", "2026-05-20"),
                txn("after", "-5.00", "2026-06-02"),
                txn("before", "-5.00", "2026-05-01"),
            ),
        )
        content(model)
        model.onCustomRangeSelected(LocalDate(2026, 5, 10), LocalDate(2026, 5, 31))
        advanceUntilIdle()
        val c = (model.uiState.value as ScreenState.Content).data
        assertEquals(listOf("inRange"), c.groups.flatMap { it.transactions }.map { it.txId })
        assertEquals(DateRangePreset.CUSTOM, c.range.preset)
    }

    @Test
    fun defaultRange_last30Days_filtersOutOlderPending() = runTest(dispatcher) {
        val c = content(
            vm(
                requests = listOf(
                    pendingRequest("fresh", date = "2026-06-06"),
                    pendingRequest("stale", date = "2026-05-01"),
                ),
            ),
        )
        assertEquals(listOf("fresh"), c.pending.map { it.id })
    }

    @Test
    fun error_surfacesErrorState() = runTest(dispatcher) {
        val model = vm(transactionsResult = Result.failure(IllegalStateException("boom")))
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(model.uiState.value is ScreenState.Error)
    }

    @Test
    fun noMatches_isEmptyState() = runTest(dispatcher) {
        val model = vm(transactions = listOf(txn("t1", "-1.00", "2026-06-01")))
        content(model)
        model.onQueryChanged("zzz-no-match")
        advanceUntilIdle()
        assertTrue(model.uiState.value is ScreenState.Empty)
    }
}
