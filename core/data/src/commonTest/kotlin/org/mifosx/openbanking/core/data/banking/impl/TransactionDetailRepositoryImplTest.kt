/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking.impl

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkInfo
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkStatus
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkType
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.mifosx.openbanking.core.data.banking.store.BankingStores
import org.mifosx.openbanking.core.data.banking.store.FakeFetchedAtRepository
import org.mifosx.openbanking.core.data.banking.store.FakeNetworkMonitor
import org.mifosx.openbanking.core.data.util.RemoteException
import org.mifosx.openbanking.core.model.banking.TransactionCategory
import org.mifosx.openbanking.core.model.banking.TransactionDetail
import org.mifosx.openbanking.core.network.api.Aisp
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * End-to-end over [TransactionDetailRepositoryImpl] and [BankingStores.transactionDetailsStore]: a real
 * Store5 store fed by a Ktor [MockEngine], so the OBIE `OBReadTransaction6` payload travels the same
 * path it does in the app — deserialization, [TransactionsResponse.toTransactionDetails] mapping,
 * category derivation and [ScreenState] classification. Only the network is faked.
 */
class TransactionDetailRepositoryImplTest {

    private val onlineInfo = NetworkInfo(type = NetworkType.WiFi, isMetered = false)
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    /** A Tesco card debit (MCC 5411 → Groceries) and a salary credit with no merchant and no MCC. */
    private val twoTransactions = """
        {"Data":{"Transaction":[
          ${tx(
        id = "TX-1",
        indicator = "Debit",
        info = "TESCO STORES 3476 LONDON",
        amount = "42.17",
        balance = "447.63",
        merchant = "Tesco Stores",
        mcc = "5411",
        code = "DR",
    )},
          ${tx(
        id = "TX-2",
        indicator = "Credit",
        info = "SALARY JUN ACME LTD",
        amount = "2400.00",
        balance = "2847.63",
        merchant = null,
        mcc = null,
        code = "CR",
    )}
        ]},"Meta":{"TotalPages":1}}
    """.trimIndent()

    private val noTransactions = """{"Data":{"Transaction":[]},"Meta":{"TotalPages":1}}"""

    private fun mockClient(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = HttpClient(MockEngine(handler)) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    private fun aisp(body: String, status: HttpStatusCode = HttpStatusCode.OK): Aisp =
        Aisp(mockClient { respond(body, status, jsonHeaders) })

    private fun repo(
        body: String = twoTransactions,
        status: HttpStatusCode = HttpStatusCode.OK,
        fetchedAt: FakeFetchedAtRepository = FakeFetchedAtRepository(),
    ): TransactionDetailRepositoryImpl = TransactionDetailRepositoryImpl(
        store = BankingStores.transactionDetailsStore(aisp(body, status)),
        networkMonitor = FakeNetworkMonitor(NetworkStatus.Available(onlineInfo)),
        fetchedAtRepository = fetchedAt,
    )

    @Test
    fun streamEmitsContentCarryingTheMappedRichList() = runTest(UnconfinedTestDispatcher()) {
        val state = repo()
            .transactionDetailStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Content }

        val list = assertIs<ScreenState.Content<List<TransactionDetail>>>(state).data
        assertEquals(EXPECTED_COUNT, list.size)

        val debit = list.first()
        assertEquals("TX-1", debit.transactionId)
        assertEquals(ACCOUNT_ID, debit.accountId)
        assertEquals("42.17", debit.amount)
        assertEquals("GBP", debit.currency)
        assertFalse(debit.isCredit)
        assertEquals("Tesco Stores", debit.merchantName)
        assertEquals(TransactionCategory.GROCERIES, debit.category)
        assertEquals("5411", debit.merchantCategoryCode)
        assertEquals("447.63", debit.balanceAmount)
        assertTrue(debit.balanceIsCredit)
        assertEquals("TESCO STORES 3476 LONDON", debit.transactionInformation)
        assertEquals("DR", debit.proprietaryCode)
        assertEquals("HSBC", debit.proprietaryIssuer)
    }

    @Test
    fun aTransactionWithNoMerchantDropsToNullMerchantAndNullMcc() = runTest(UnconfinedTestDispatcher()) {
        val state = repo()
            .transactionDetailStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Content }

        val credit = assertIs<ScreenState.Content<List<TransactionDetail>>>(state).data[1]
        assertEquals("TX-2", credit.transactionId)
        assertTrue(credit.isCredit)
        assertNull(credit.merchantName)
        assertNull(credit.merchantCategoryCode)
        assertEquals(TransactionCategory.OTHER, credit.category)
    }

    @Test
    fun anAccountWithNoTransactionsIsContentCarryingAnEmptyList() = runTest(UnconfinedTestDispatcher()) {
        val state = repo(body = noTransactions)
            .transactionDetailStream(ACCOUNT_ID, backgroundScope).state
            .first { it !is ScreenState.Loading }

        assertTrue(assertIs<ScreenState.Content<List<TransactionDetail>>>(state).data.isEmpty())
    }

    @Test
    fun theStreamUsesThePerAccountCacheKey() = runTest(UnconfinedTestDispatcher()) {
        val fetchedAt = FakeFetchedAtRepository()
        repo(fetchedAt = fetchedAt)
            .transactionDetailStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Content }

        assertTrue("transactionDetail:list:$ACCOUNT_ID" in fetchedAt.readKeys)
    }

    @Test
    fun aSecondAccountOnTheSameInstanceUsesItsOwnCacheKey() = runTest(UnconfinedTestDispatcher()) {
        val fetchedAt = FakeFetchedAtRepository()
        val repository = repo(fetchedAt = fetchedAt)

        repository.transactionDetailStream(ACCOUNT_ID, backgroundScope).state.first { it is ScreenState.Content }
        repository.transactionDetailStream(OTHER_ACCOUNT_ID, backgroundScope).state.first { it is ScreenState.Content }

        assertTrue("transactionDetail:list:$ACCOUNT_ID" in fetchedAt.readKeys)
        assertTrue("transactionDetail:list:$OTHER_ACCOUNT_ID" in fetchedAt.readKeys)
    }

    @Test
    fun forbiddenSurfacesAsAnErrorCarryingTheForbiddenNetworkError() = runTest(UnconfinedTestDispatcher()) {
        val state = repo(body = "denied", status = HttpStatusCode.Forbidden)
            .transactionDetailStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Error || it is ScreenState.Unauthenticated }

        val error = assertIs<ScreenState.Error>(state)
        assertIs<NetworkError.Client.Forbidden>(assertIs<RemoteException>(error.error).networkError)
    }

    @Test
    fun refreshingAStreamReEmitsContent() = runTest(UnconfinedTestDispatcher()) {
        val stream = repo().transactionDetailStream(ACCOUNT_ID, backgroundScope)
        stream.state.first { it is ScreenState.Content }

        stream.refresh()

        assertIs<ScreenState.Content<List<TransactionDetail>>>(stream.state.first { it is ScreenState.Content })
    }

    private companion object {
        const val ACCOUNT_ID = "40051512345678"
        const val OTHER_ACCOUNT_ID = "40051599999999"
        const val EXPECTED_COUNT = 2

        @Suppress("LongParameterList")
        fun tx(
            id: String,
            indicator: String,
            info: String,
            amount: String,
            balance: String,
            merchant: String?,
            mcc: String?,
            code: String,
        ): String {
            val merchantBlock = if (merchant != null && mcc != null) {
                ""","MerchantDetails":{"MerchantName":"$merchant","MerchantCategoryCode":"$mcc"}"""
            } else {
                ""
            }
            return """
                {"AccountId":"$ACCOUNT_ID","TransactionId":"$id","CreditDebitIndicator":"$indicator",
                 "Status":"Booked","BookingDateTime":"2026-06-26T11:22:00Z",
                 "ValueDateTime":"2026-06-26T11:22:00Z","TransactionInformation":"$info",
                 "Amount":{"Amount":"$amount","Currency":"GBP"},
                 "Balance":{"CreditDebitIndicator":"Credit","Amount":{"Amount":"$balance","Currency":"GBP"}},
                 "ProprietaryBankTransactionCode":{"Code":"$code","Issuer":"HSBC"}$merchantBlock}
            """.trimIndent()
        }
    }
}
