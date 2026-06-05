/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.standingorders

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.mifosx.openbanking.core.data.customers.CustomersRepository
import org.mifosx.openbanking.core.data.testutil.FakeObpCacheDao
import org.mifosx.openbanking.core.data.testutil.testJson
import org.mifosx.openbanking.core.data.transactions.TransactionsRepository
import org.mifosx.openbanking.core.model.obp.AmountOfMoney
import org.mifosx.openbanking.core.model.obp.CounterpartyHolder
import org.mifosx.openbanking.core.model.obp.CreateStandingOrderRequest
import org.mifosx.openbanking.core.model.obp.CreateStandingOrderResponse
import org.mifosx.openbanking.core.model.obp.Customer
import org.mifosx.openbanking.core.model.obp.CustomerRequest
import org.mifosx.openbanking.core.model.obp.StandingOrderSchedule
import org.mifosx.openbanking.core.model.obp.Transaction
import org.mifosx.openbanking.core.model.obp.TransactionAttribute
import org.mifosx.openbanking.core.model.obp.TransactionCounterparty
import org.mifosx.openbanking.core.model.obp.TransactionDetails
import org.mifosx.openbanking.core.network.api.StandingOrdersApi
import org.mifosx.openbanking.core.network.obp.ObpConfig
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import template.core.base.store.screen.ScreenDataStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StandingOrdersRepositoryTest {

    private val config = ObpConfig(bankId = "ac.bank.uk")

    private fun soTxn(description: String, amount: String, completed: String) = Transaction(
        transactionId = "$description-$completed",
        otherAccount = TransactionCounterparty(
            id = "ac.savings.001",
            holder = CounterpartyHolder(name = "afternooncoffee"),
        ),
        details = TransactionDetails(
            description = description,
            completed = "${completed}T09:00:00Z",
            posted = "${completed}T09:00:00Z",
            value = AmountOfMoney(currency = "EUR", amount = amount),
        ),
        transactionAttributes = listOf(TransactionAttribute(name = "TXN_TYPE", type = "STRING", value = "SO")),
    )

    private class FakeTransactionsRepository(
        private val transactions: List<Transaction>,
    ) : TransactionsRepository {
        override fun transactionsStream(
            bankId: String,
            accountId: String,
            scope: CoroutineScope,
        ): ScreenDataStream<List<Transaction>> = TODO("not used")

        override suspend fun listTransactions(bankId: String, accountId: String, limit: Int?) =
            Result.success(transactions)

        override suspend fun listTransactionsWithAttributes(bankId: String, accountId: String, limit: Int?) =
            Result.success(transactions)

        override suspend fun getTransaction(bankId: String, accountId: String, transactionId: String) =
            Result.success(transactions.first())
    }

    private class FakeStandingOrdersApi(
        var response: NetworkResult<CreateStandingOrderResponse, NetworkError> =
            NetworkResult.Success(CreateStandingOrderResponse(standingOrderId = "so-1", active = true)),
    ) : StandingOrdersApi {
        var lastRequest: CreateStandingOrderRequest? = null
        override suspend fun createStandingOrder(
            bankId: String,
            accountId: String,
            request: CreateStandingOrderRequest,
        ): NetworkResult<CreateStandingOrderResponse, NetworkError> {
            lastRequest = request
            return response
        }
    }

    private class FakeCustomersRepository(
        private val holderName: String = "",
    ) : CustomersRepository {
        override fun customersStream(scope: CoroutineScope): ScreenDataStream<List<Customer>> = TODO("not used")
        override suspend fun list(): Result<List<Customer>> = TODO("not used")
        override suspend fun currentUserCustomers(): Result<List<Customer>> = TODO("not used")
        override suspend fun get(customerId: String): Result<Customer> = TODO("not used")
        override suspend fun create(request: CustomerRequest): Result<Customer> = TODO("not used")
        override suspend fun update(customerId: String, request: CustomerRequest): Result<Customer> =
            TODO("not used")
        override suspend fun accountHolderName(bankId: String, accountId: String): Result<String> =
            Result.success(holderName)
    }

    private fun repository(
        transactions: List<Transaction> = emptyList(),
        api: FakeStandingOrdersApi = FakeStandingOrdersApi(),
        dao: FakeObpCacheDao = FakeObpCacheDao(),
        holderName: String = "",
    ) = StandingOrdersRepositoryImpl(
        api = api,
        transactionsRepository = FakeTransactionsRepository(transactions),
        customersRepository = FakeCustomersRepository(holderName),
        config = config,
        dao = dao,
        json = testJson(),
    )

    @Test
    fun listRecurringResolvesCounterpartyHolderLegalName() = runTest {
        val repo = repository(
            transactions = listOf(soTxn("Rent", "-450.00", "2026-06-01")),
            holderName = "Kalpesh Patel",
        )
        // The transaction holder is the login username; the legal name from
        // customer-account-links must win.
        assertEquals("Kalpesh Patel", repo.listRecurring("acc-1").getOrThrow().single().counterpartyName)
    }

    @Test
    fun listRecurringKeepsTransactionHolderWhenLookupBlank() = runTest {
        val repo = repository(
            transactions = listOf(soTxn("Rent", "-450.00", "2026-06-01")),
            holderName = "",
        )
        assertEquals("afternooncoffee", repo.listRecurring("acc-1").getOrThrow().single().counterpartyName)
    }

    @Test
    fun listRecurringDerivesRowsFromSoTransactions() = runTest {
        val repo = repository(
            transactions = listOf(
                soTxn("Rent", "-450.00", "2026-05-01"),
                soTxn("Rent", "-450.00", "2026-06-01"),
            ),
        )
        val rows = repo.listRecurring("acc-1").getOrThrow()
        assertEquals(1, rows.size)
        assertEquals("Rent", rows.single().name)
    }

    @Test
    fun createPersistsLocallyAndMergesIntoList() = runTest {
        val dao = FakeObpCacheDao()
        val api = FakeStandingOrdersApi()
        val repo = repository(api = api, dao = dao)

        val created = repo.create(
            accountId = "acc-1",
            name = "Savings Counterparty",
            request = CreateStandingOrderRequest(
                customerId = "cust-1",
                userId = "user-1",
                counterpartyId = "cp-1",
                amount = AmountOfMoney(currency = "EUR", amount = "25.00"),
                `when` = StandingOrderSchedule(frequency = "MONTHLY"),
                dateSigned = "2026-06-05T00:00:00Z",
                dateStarts = "2026-07-01T00:00:00Z",
            ),
        ).getOrThrow()

        assertTrue(created.created)
        assertEquals("Savings Counterparty", created.name)
        assertEquals("25.00", created.amountValue)
        assertEquals("2026-07-01", created.nextPaymentDate)

        val rows = repo.listRecurring("acc-1").getOrThrow()
        assertEquals(listOf("Savings Counterparty"), rows.map { it.name })
    }

    @Test
    fun createFailureDoesNotPersist() = runTest {
        val api = FakeStandingOrdersApi(response = NetworkResult.Error(NetworkError.UNAUTHORIZED))
        val repo = repository(api = api)

        val result = repo.create(
            accountId = "acc-1",
            name = "X",
            request = CreateStandingOrderRequest(
                customerId = "c",
                userId = "u",
                counterpartyId = "cp",
                amount = AmountOfMoney(currency = "EUR", amount = "1.00"),
                `when` = StandingOrderSchedule(frequency = "MONTHLY"),
                dateSigned = "2026-06-05T00:00:00Z",
                dateStarts = "2026-07-01T00:00:00Z",
            ),
        )
        assertTrue(result.isFailure)
        assertEquals(0, repo.listRecurring("acc-1").getOrThrow().size)
    }
}
