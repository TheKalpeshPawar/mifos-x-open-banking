/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking.store

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.mifosx.openbanking.core.data.banking.mapper.toAccountBalance
import org.mifosx.openbanking.core.data.banking.mapper.toAccountEntity
import org.mifosx.openbanking.core.data.banking.mapper.toBankAccount
import org.mifosx.openbanking.core.data.banking.mapper.toBankAccounts
import org.mifosx.openbanking.core.data.banking.mapper.toTransactionEntity
import org.mifosx.openbanking.core.data.banking.mapper.toTransactionItem
import org.mifosx.openbanking.core.data.banking.mapper.toTransactionItems
import org.mifosx.openbanking.core.database.banking.entity.AccountEntity
import org.mifosx.openbanking.core.database.banking.entity.TransactionEntity
import org.mifosx.openbanking.core.model.banking.AccountBalance
import org.mifosx.openbanking.core.model.banking.BankAccount
import org.mifosx.openbanking.core.model.banking.TransactionItem
import org.mifosx.openbanking.core.network.api.Aisp
import org.mifosx.openbanking.core.network.model.ais.accounts.Account
import org.mifosx.openbanking.core.network.model.ais.accounts.AccountsResponse
import org.mifosx.openbanking.core.network.model.ais.balances.Balance
import org.mifosx.openbanking.core.network.model.ais.balances.BalancesResponse
import org.mifosx.openbanking.core.network.model.ais.transactions.MerchantDetails
import org.mifosx.openbanking.core.network.model.ais.transactions.Transaction
import org.mifosx.openbanking.core.network.model.ais.transactions.TransactionsResponse
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import org.mifosx.openbanking.core.network.model.ais.accounts.Data as AccountsData
import org.mifosx.openbanking.core.network.model.ais.balances.Amount as BalanceAmount
import org.mifosx.openbanking.core.network.model.ais.balances.Data as BalancesData
import org.mifosx.openbanking.core.network.model.ais.transactions.Amount as TransactionAmount
import org.mifosx.openbanking.core.network.model.ais.transactions.Data as TransactionsData

/**
 * Drives the real Store5 stores built by [BankingStores] over a MockEngine-backed [Aisp] and the
 * in-memory DAO fakes, asserting the Fetcher → SourceOfTruth pipeline maps and persists correctly
 * and surfaces errors.
 */
class BankingStoresTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    private val json = Json { ignoreUnknownKeys = true }

    private fun mockClient(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = HttpClient(MockEngine(handler)) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    private fun aispReturning(body: String): Aisp =
        Aisp(mockClient { respond(body, HttpStatusCode.OK, jsonHeaders) })

    private fun failingAisp(): Aisp =
        Aisp(mockClient { respond("{}", HttpStatusCode.InternalServerError, jsonHeaders) })

    private suspend fun <T : Any> Store<String, T>.awaitFresh(key: String): StoreReadResponse<T> =
        stream(StoreReadRequest.fresh(key))
            .filterNot {
                it is StoreReadResponse.Loading ||
                    it is StoreReadResponse.NoNewData ||
                    it is StoreReadResponse.Initial
            }
            .first()

    private suspend fun <T : Any> Store<String, T>.awaitCachedData(key: String): T =
        stream(StoreReadRequest.cached(key, refresh = false))
            .filterIsInstance<StoreReadResponse.Data<T>>()
            .first()
            .value

    @Test
    fun accountsStoreFetchSuccessMapsDomainAndPersistsThroughDao() = runTest {
        val dao = FakeAccountDao()
        val store = BankingStores.accountsStore(aispReturning(accountsJson()), dao)

        val response = store.awaitFresh(BankingStores.ACCOUNTS_KEY)

        val data = assertIs<StoreReadResponse.Data<List<BankAccount>>>(response)
        assertEquals(expectedAccounts, data.value)
        assertEquals(listOf("clear", "upsert"), dao.ops)
        assertEquals(expectedAccounts.map { it.toAccountEntity() }, dao.current)
    }

    @Test
    fun accountsStoreFetchErrorSurfacesStoreError() = runTest {
        val store = BankingStores.accountsStore(failingAisp(), FakeAccountDao())

        assertIs<StoreReadResponse.Error>(store.awaitFresh(BankingStores.ACCOUNTS_KEY))
    }

    @Test
    fun accountsStoreCachedReadMapsSeededEntitiesWithoutNetwork() = runTest {
        val dao = FakeAccountDao()
        val seeded = AccountEntity(
            accountId = "acc-9",
            nickname = "Savings",
            accountSubType = "Savings",
            currency = "EUR",
            sortCode = "112233",
            accountNumber = "87654321",
        )
        dao.upsertAll(listOf(seeded))
        val store = BankingStores.accountsStore(failingAisp(), dao)

        assertEquals(listOf(seeded.toBankAccount()), store.awaitCachedData(BankingStores.ACCOUNTS_KEY))
    }

    @Test
    fun transactionsStoreFetchSuccessMapsDomainAndPersistsThroughDao() = runTest {
        val dao = FakeTransactionDao()
        val store = BankingStores.transactionsStore(aispReturning(transactionsJson()), dao)

        val response = store.awaitFresh(ACCOUNT_ID)

        val data = assertIs<StoreReadResponse.Data<List<TransactionItem>>>(response)
        assertEquals(expectedTransactions, data.value)
        assertEquals(listOf("clearAccount", "upsert"), dao.ops)
        assertEquals(expectedTransactions.map { it.toTransactionEntity() }, dao.current)
    }

    @Test
    fun transactionsStoreFetchErrorSurfacesStoreError() = runTest {
        val store = BankingStores.transactionsStore(failingAisp(), FakeTransactionDao())

        assertIs<StoreReadResponse.Error>(store.awaitFresh(ACCOUNT_ID))
    }

    @Test
    fun transactionsStoreCachedReadMapsSeededEntitiesWithoutNetwork() = runTest {
        val dao = FakeTransactionDao()
        val seeded = TransactionEntity(
            transactionId = "t9",
            accountId = ACCOUNT_ID,
            description = "RENT",
            bookingDateTime = "2026-06-01T09:00:00Z",
            amount = "500.00",
            currency = "GBP",
            isCredit = false,
        )
        dao.upsertAll(listOf(seeded))
        val store = BankingStores.transactionsStore(failingAisp(), dao)

        assertEquals(listOf(seeded.toTransactionItem()), store.awaitCachedData(ACCOUNT_ID))
    }

    @Test
    fun balancesStoreFetchSuccessMapsDomain() = runTest {
        val store = BankingStores.balancesStore(aispReturning(balancesJson()))

        val response = store.awaitFresh(ACCOUNT_ID)

        val data = assertIs<StoreReadResponse.Data<AccountBalance>>(response)
        assertEquals(expectedBalance, data.value)
    }

    @Test
    fun balancesStoreFetchErrorSurfacesStoreError() = runTest {
        val store = BankingStores.balancesStore(failingAisp())

        assertIs<StoreReadResponse.Error>(store.awaitFresh(ACCOUNT_ID))
    }

    private fun accountsJson(): String = json.encodeToString(AccountsResponse.serializer(), accountsResponse)

    private fun balancesJson(): String = json.encodeToString(BalancesResponse.serializer(), balancesResponse)

    private fun transactionsJson(): String =
        json.encodeToString(TransactionsResponse.serializer(), transactionsResponse)

    private val expectedAccounts: List<BankAccount> get() = accountsResponse.toBankAccounts()

    private val expectedTransactions: List<TransactionItem> get() = transactionsResponse.toTransactionItems(ACCOUNT_ID)

    private val expectedBalance: AccountBalance get() = balancesResponse.toAccountBalance(ACCOUNT_ID)

    private companion object {
        const val ACCOUNT_ID = "acc-1"

        val accountsResponse = AccountsResponse(
            data = AccountsData(
                account = listOf(
                    Account(
                        accountId = ACCOUNT_ID,
                        name = "Everyday Current",
                        currency = "GBP",
                        account = listOf(
                            Account(
                                schemeName = "UK.OBIE.SortCodeAccountNumber",
                                identification = "40051512345678",
                            ),
                        ),
                    ),
                ),
            ),
        )

        val balancesResponse = BalancesResponse(
            data = BalancesData(
                balance = listOf(
                    Balance(type = "InterimBooked", amount = BalanceAmount("2900.00", "GBP")),
                    Balance(type = "InterimAvailable", amount = BalanceAmount("2847.63", "GBP")),
                ),
            ),
        )

        val transactionsResponse = TransactionsResponse(
            data = TransactionsData(
                transaction = listOf(
                    Transaction(
                        transactionId = "t1",
                        creditDebitIndicator = "Debit",
                        bookingDateTime = "2026-06-27T10:00:00Z",
                        transactionInformation = "CARD PAYMENT",
                        amount = TransactionAmount("42.17", "GBP"),
                        merchantDetails = MerchantDetails(merchantName = "TESCO STORES"),
                    ),
                ),
            ),
        )
    }
}
