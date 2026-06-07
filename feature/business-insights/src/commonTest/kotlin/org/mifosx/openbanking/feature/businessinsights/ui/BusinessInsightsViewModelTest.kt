/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.businessinsights.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import org.mifosx.openbanking.core.data.accounts.AccountsRepository
import org.mifosx.openbanking.core.data.accounts.PfmAccountsService
import org.mifosx.openbanking.core.data.transactions.TransactionsRepository
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.core.model.obp.AmountOfMoney
import org.mifosx.openbanking.core.model.obp.CounterpartyHolder
import org.mifosx.openbanking.core.model.obp.Transaction
import org.mifosx.openbanking.core.model.obp.TransactionAttribute
import org.mifosx.openbanking.core.model.obp.TransactionCounterparty
import org.mifosx.openbanking.core.model.obp.TransactionDetails
import template.core.base.store.screen.ScreenDataStream
import template.core.base.store.screen.ScreenState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val TODAY = LocalDate(2026, 6, 6)

private fun bizTxn(
    amount: String,
    description: String = "Payment",
    holder: String = "TechStart Ltd",
    date: String = "2026-06-03",
    typeCode: String? = "SANDBOX_TAN",
) = Transaction(
    id = "$holder-$date-$amount-$description",
    otherAccount = TransactionCounterparty(holder = CounterpartyHolder(name = holder)),
    details = TransactionDetails(
        description = description,
        completed = "${date}T10:00:00Z",
        value = AmountOfMoney(currency = "GBP", amount = amount),
    ),
    transactionAttributes = typeCode?.let { listOf(TransactionAttribute("TXN_TYPE", "STRING", it)) }.orEmpty(),
)

private fun bizAccount(id: String, type: String = "BUSINESS", currency: String = "GBP") = Account(
    id = id,
    label = "Account $id",
    bankId = "mifos-x-openbank",
    accountType = type,
    balance = AmountOfMoney(currency = currency, amount = "50000.00"),
)

private class BizFakeAccountsRepository(
    var accounts: Result<List<Account>> = Result.success(listOf(bizAccount("biz-1"))),
) : AccountsRepository {
    override fun accountsStream(scope: CoroutineScope): ScreenDataStream<List<Account>> = TODO()
    override suspend fun listAccounts(): Result<List<Account>> = TODO()
    override suspend fun myAccounts(): Result<List<Account>> = accounts
    override suspend fun accountDetail(bankId: String, accountId: String): Result<Account> =
        Result.failure(IllegalStateException("no detail in fake — list shape carries the type"))
}

private class BizFakeTransactionsRepository(
    var result: Result<List<Transaction>> = Result.success(emptyList()),
) : TransactionsRepository {
    var fetches = 0
    var lastAccountId = ""
    override fun transactionsStream(
        bankId: String,
        accountId: String,
        scope: CoroutineScope,
    ): ScreenDataStream<List<Transaction>> = TODO()
    override suspend fun listTransactions(bankId: String, accountId: String, limit: Int?) = TODO()
    override suspend fun listTransactionsWithAttributes(
        bankId: String,
        accountId: String,
        limit: Int?,
    ): Result<List<Transaction>> {
        fetches++
        lastAccountId = accountId
        return result
    }
    override suspend fun getTransaction(bankId: String, accountId: String, transactionId: String) = TODO()
}

class BusinessInsightsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(
        transactions: BizFakeTransactionsRepository = BizFakeTransactionsRepository(),
        accounts: BizFakeAccountsRepository = BizFakeAccountsRepository(),
    ) = BusinessInsightsViewModel(
        pfmAccountsService = PfmAccountsService(accounts),
        transactionsRepository = transactions,
        todayProvider = { TODAY },
    )

    private suspend fun TestScope.content(model: BusinessInsightsViewModel): BizContent {
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        val s = model.uiState.value
        assertTrue(s is ScreenState.Content, "expected Content, was $s")
        return s.data
    }

    @Test
    fun load_pickerRestrictedToBusinessAccounts() = runTest(dispatcher) {
        val accounts = BizFakeAccountsRepository(
            Result.success(
                listOf(
                    bizAccount("acc-1", type = "CURRENT"),
                    bizAccount("biz-1"),
                    bizAccount("biz-2"),
                ),
            ),
        )
        val c = content(vm(accounts = accounts))
        assertEquals(listOf("biz-1", "biz-2"), c.accounts.map { it.id })
        assertEquals("biz-1", c.selectedAccountId)
    }

    @Test
    fun load_cashFlowInOutNet() = runTest(dispatcher) {
        val transactions = BizFakeTransactionsRepository(
            Result.success(
                listOf(
                    bizTxn("75000.00", description = "Q1 client invoice receipts"),
                    bizTxn("-15000.00", description = "Payroll April"),
                    bizTxn("-8000.00", description = "VAT payment Q1"),
                ),
            ),
        )
        val c = content(vm(transactions))
        assertEquals(75000.0, c.moneyIn)
        assertEquals(23000.0, c.moneyOut)
        assertEquals(52000.0, c.net)
        assertEquals("GBP", c.currency)
        assertTrue(c.hasActivity)
    }

    @Test
    fun load_expensesUseBusinessTaxonomy() = runTest(dispatcher) {
        val transactions = BizFakeTransactionsRepository(
            Result.success(
                listOf(
                    bizTxn("-15000.00", description = "Payroll April"),
                    bizTxn("-8000.00", description = "VAT payment Q1"),
                    bizTxn("-2500.00", description = "Office rent April"),
                    bizTxn("-450.00", description = "SaaS subscriptions"),
                ),
            ),
        )
        val c = content(vm(transactions))
        assertEquals(
            listOf("payroll-contractors", "tax", "rent-facilities", "software-subscriptions"),
            c.categories.map { it.id },
        )
    }

    @Test
    fun load_incomeKeywordDebitNeverRendersInExpenseBreakdown() = runTest(dispatcher) {
        val transactions = BizFakeTransactionsRepository(
            Result.success(
                listOf(
                    bizTxn("-516.01", description = "May contractor invoice"),
                    bizTxn("-200.00", description = "Client gift hamper", holder = "Hamper Co"),
                ),
            ),
        )
        val c = content(vm(transactions))
        assertTrue(c.categories.none { it.id == "income" })
        assertEquals(716.01, c.moneyOut, absoluteTolerance = 0.001)
    }

    @Test
    fun load_noBusinessAccountsIsEmptyState() = runTest(dispatcher) {
        val accounts = BizFakeAccountsRepository(
            Result.success(listOf(bizAccount("acc-1", type = "CURRENT"))),
        )
        val model = vm(accounts = accounts)
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(model.uiState.value is ScreenState.Empty)
    }

    @Test
    fun accountSwitch_refetchesTransactions() = runTest(dispatcher) {
        val transactions = BizFakeTransactionsRepository()
        val accounts = BizFakeAccountsRepository(
            Result.success(listOf(bizAccount("biz-1"), bizAccount("biz-2"))),
        )
        val model = vm(transactions, accounts)
        content(model)
        model.onAccountSelected("biz-2")
        advanceUntilIdle()
        assertEquals(2, transactions.fetches)
        assertEquals("biz-2", transactions.lastAccountId)
    }

    @Test
    fun accountsFailure_isErrorThenRetryRecovers() = runTest(dispatcher) {
        val accounts = BizFakeAccountsRepository(Result.failure(RuntimeException("boom")))
        val model = vm(accounts = accounts)
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(model.uiState.value is ScreenState.Error)

        accounts.accounts = Result.success(listOf(bizAccount("biz-1")))
        model.onRetry()
        advanceUntilIdle()
        assertTrue(model.uiState.value is ScreenState.Content)
    }

    @Test
    fun periodSwitch_reprojectsWithoutRefetch() = runTest(dispatcher) {
        val transactions = BizFakeTransactionsRepository(
            Result.success(
                listOf(
                    bizTxn("-100.00", date = "2026-06-03"),
                    bizTxn("-999.00", date = "2026-05-10"),
                ),
            ),
        )
        val model = vm(transactions)
        content(model)
        model.onPeriodSelected(org.mifosx.openbanking.core.model.pfm.PfmPeriod.LAST_MONTH)
        advanceUntilIdle()
        val c = (model.uiState.value as ScreenState.Content).data
        assertEquals(999.0, c.moneyOut)
        assertEquals(1, transactions.fetches)
    }
}
