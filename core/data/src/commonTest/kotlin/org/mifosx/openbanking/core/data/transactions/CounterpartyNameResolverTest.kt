/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.transactions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.mifosx.openbanking.core.data.accounts.AccountsRepository
import org.mifosx.openbanking.core.data.customers.CustomersRepository
import org.mifosx.openbanking.core.data.profile.ProfileRepository
import org.mifosx.openbanking.core.data.testutil.FakeObpCacheDao
import org.mifosx.openbanking.core.data.testutil.testJson
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.core.model.obp.AmountOfMoney
import org.mifosx.openbanking.core.model.obp.CounterpartyHolder
import org.mifosx.openbanking.core.model.obp.Customer
import org.mifosx.openbanking.core.model.obp.CustomerRequest
import org.mifosx.openbanking.core.model.obp.ProfileUpdateRequest
import org.mifosx.openbanking.core.model.obp.Transaction
import org.mifosx.openbanking.core.model.obp.TransactionCounterparty
import org.mifosx.openbanking.core.model.obp.TransactionDetails
import org.mifosx.openbanking.core.model.obp.UserProfile
import template.core.base.store.screen.ScreenDataStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CounterpartyNameResolverTest {

    private val bank = "ac.bank.uk"
    private val viewer = "ac.checking.001"

    private fun txn(
        description: String,
        amount: String,
        completed: String,
        holder: String,
        otherId: String,
    ) = Transaction(
        transactionId = "$description-$completed-$amount",
        otherAccount = TransactionCounterparty(id = otherId, holder = CounterpartyHolder(name = holder)),
        details = TransactionDetails(
            description = description,
            completed = completed,
            value = AmountOfMoney(currency = "EUR", amount = amount),
        ),
    )

    private val viewerTransactions = listOf(
        txn("Monthly savings transfer", "-200.00", "2026-06-05T09:00:00Z", "afternooncoffee", "obf-savings"),
        txn("Berlin holiday split", "-172.00", "2026-05-10T07:57:39Z", "afternooncoffee", "obf-alice"),
        txn("Netflix Subscription", "-15.99", "2026-06-04T09:00:00Z", "afternooncoffee", "obf-external"),
        txn("SEPA rail probe", "-1.00", "2026-06-03T09:00:00Z", "sbissmann21", "obf-sepa"),
    )

    private val mirrorsByAccount = mapOf(
        "ac.savings.001" to listOf(
            txn("Monthly savings transfer", "200.00", "2026-06-05T09:00:00Z", "afternooncoffee", "obf-checking"),
        ),
        "ac.alice.current.001" to listOf(
            txn("Berlin holiday split", "172.00", "2026-05-10T07:57:39Z", "afternooncoffee", "obf-checking"),
        ),
    )

    private val ownAccounts = listOf(
        Account(id = viewer, bankId = bank, label = "Main Checking"),
        Account(id = "ac.savings.001", bankId = bank, label = "Savings"),
        Account(id = "ac.alice.current.001", bankId = bank, label = "Alice — Packaged Current"),
    )

    private class FakeAccountsRepository(private val accounts: List<Account>) : AccountsRepository {
        override fun accountsStream(scope: CoroutineScope): ScreenDataStream<List<Account>> = TODO("not used")
        override suspend fun listAccounts(): Result<List<Account>> = Result.success(accounts)
        override suspend fun myAccounts(): Result<List<Account>> = Result.success(accounts)
        override suspend fun accountDetail(bankId: String, accountId: String): Result<Account> = TODO("not used")
    }

    private class FakeTransactionsRepository(
        private val byAccount: Map<String, List<Transaction>>,
    ) : TransactionsRepository {
        var fetchCount = 0
        override fun transactionsStream(
            bankId: String,
            accountId: String,
            scope: CoroutineScope,
        ): ScreenDataStream<List<Transaction>> = TODO("not used")

        override suspend fun listTransactions(
            bankId: String,
            accountId: String,
            limit: Int?,
        ): Result<List<Transaction>> {
            fetchCount += 1
            return Result.success(byAccount[accountId].orEmpty())
        }

        override suspend fun listTransactionsWithAttributes(bankId: String, accountId: String, limit: Int?) =
            listTransactions(bankId, accountId, limit)

        override suspend fun getTransaction(bankId: String, accountId: String, transactionId: String) =
            TODO("not used")
    }

    private class FakeCustomersRepository(
        private val holderNames: Map<String, String>,
    ) : CustomersRepository {
        override fun customersStream(scope: CoroutineScope): ScreenDataStream<List<Customer>> = TODO("not used")
        override suspend fun list(): Result<List<Customer>> = TODO("not used")
        override suspend fun currentUserCustomers(): Result<List<Customer>> = TODO("not used")
        override suspend fun get(customerId: String): Result<Customer> = TODO("not used")
        override suspend fun create(request: CustomerRequest): Result<Customer> = TODO("not used")
        override suspend fun update(customerId: String, request: CustomerRequest): Result<Customer> =
            TODO("not used")
        override suspend fun accountHolderName(bankId: String, accountId: String): Result<String> =
            Result.success(holderNames[accountId].orEmpty())
    }

    private class FakeProfileRepository(private val username: String) : ProfileRepository {
        override suspend fun current(): Result<UserProfile> = Result.success(UserProfile(username = username))
        override suspend fun update(request: ProfileUpdateRequest): Result<UserProfile> = TODO("not used")
    }

    private fun resolver(
        transactionsRepository: FakeTransactionsRepository = FakeTransactionsRepository(mirrorsByAccount),
        dao: FakeObpCacheDao = FakeObpCacheDao(),
        username: String = "afternooncoffee",
        holderNames: Map<String, String> = mapOf("ac.alice.current.001" to "Alice Johnson"),
    ) = CounterpartyNameResolverImpl(
        accountsRepository = FakeAccountsRepository(ownAccounts),
        transactionsRepository = transactionsRepository,
        customersRepository = FakeCustomersRepository(holderNames),
        profileRepository = FakeProfileRepository(username),
        dao = dao,
        json = testJson(),
    )

    @Test
    fun resolvesSelfTransfersToDestinationHolderNames() = runTest {
        val names = resolver().resolve(bank, viewer, viewerTransactions)
        assertEquals("Alice Johnson", names["obf-alice"])
        assertEquals("Savings", names["obf-savings"])
    }

    @Test
    fun leavesExternalAndNamedCounterpartiesUnresolved() = runTest {
        val names = resolver().resolve(bank, viewer, viewerTransactions)
        assertFalse("obf-external" in names)
        assertFalse("obf-sepa" in names)
    }

    @Test
    fun blankUsernameResolvesNothing() = runTest {
        val names = resolver(username = "").resolve(bank, viewer, viewerTransactions)
        assertTrue(names.isEmpty())
    }

    @Test
    fun secondResolveAnswersFromCacheWithoutRefetching() = runTest {
        val transactionsRepository = FakeTransactionsRepository(mirrorsByAccount)
        val dao = FakeObpCacheDao()
        val first = resolver(transactionsRepository = transactionsRepository, dao = dao)
        first.resolve(bank, viewer, viewerTransactions)
        val fetchesAfterFirst = transactionsRepository.fetchCount

        val resolved = listOf(viewerTransactions[0], viewerTransactions[1])
        val names = first.resolve(bank, viewer, resolved)
        assertEquals("Alice Johnson", names["obf-alice"])
        assertEquals("Savings", names["obf-savings"])
        assertEquals(fetchesAfterFirst, transactionsRepository.fetchCount)
    }

    @Test
    fun freshResolverReadsPersistedCache() = runTest {
        val dao = FakeObpCacheDao()
        resolver(dao = dao).resolve(bank, viewer, viewerTransactions)

        val emptyRepo = FakeTransactionsRepository(emptyMap())
        val resolved = listOf(viewerTransactions[0], viewerTransactions[1])
        val names = resolver(transactionsRepository = emptyRepo, dao = dao).resolve(bank, viewer, resolved)
        assertEquals("Alice Johnson", names["obf-alice"])
        assertEquals("Savings", names["obf-savings"])
    }
}
