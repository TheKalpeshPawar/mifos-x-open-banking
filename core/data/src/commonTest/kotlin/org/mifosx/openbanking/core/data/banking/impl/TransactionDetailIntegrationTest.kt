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
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
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
import kotlin.test.assertIs

/**
 * Drives the transaction-detail read over one Ktor [MockEngine] routing on `request.url.encodedPath`,
 * wiring the real [TransactionDetailRepositoryImpl] over the real
 * [BankingStores.transactionDetailsStore] over the same [Aisp] the app uses. Only the network is faked:
 * a successful `/transactions` fetch reaches Content, and a `403` classifies to an error carrying
 * [NetworkError.Client.Forbidden] (the consent-withdrawn signal the screen distinguishes).
 */
class TransactionDetailIntegrationTest {

    private val onlineInfo = NetworkInfo(type = NetworkType.WiFi, isMetered = false)
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private val transactionsJson = """
        {"Data":{"Transaction":[
          {"AccountId":"$ACCOUNT_ID","TransactionId":"TX-1","CreditDebitIndicator":"Debit",
           "Status":"Booked","BookingDateTime":"2026-06-26T11:22:00Z",
           "ValueDateTime":"2026-06-26T11:22:00Z","TransactionInformation":"TESCO STORES 3476 LONDON",
           "Amount":{"Amount":"42.17","Currency":"GBP"},
           "Balance":{"CreditDebitIndicator":"Credit","Amount":{"Amount":"447.63","Currency":"GBP"}},
           "MerchantDetails":{"MerchantName":"Tesco Stores","MerchantCategoryCode":"5411"},
           "ProprietaryBankTransactionCode":{"Code":"DR","Issuer":"HSBC"}},
          {"AccountId":"$ACCOUNT_ID","TransactionId":"TX-2","CreditDebitIndicator":"Credit",
           "Status":"Booked","BookingDateTime":"2026-06-25T08:00:00Z",
           "ValueDateTime":"2026-06-25T00:00:00Z","TransactionInformation":"SALARY JUN ACME LTD",
           "Amount":{"Amount":"2400.00","Currency":"GBP"},
           "Balance":{"CreditDebitIndicator":"Credit","Amount":{"Amount":"2847.63","Currency":"GBP"}},
           "ProprietaryBankTransactionCode":{"Code":"CR","Issuer":"HSBC"}}
        ]},"Meta":{"TotalPages":1}}
    """.trimIndent()

    private fun aisp(transactionsStatus: HttpStatusCode): Aisp = Aisp(
        HttpClient(
            MockEngine { request ->
                val path = request.url.encodedPath
                when {
                    path.endsWith("/transactions") -> if (transactionsStatus == HttpStatusCode.OK) {
                        respond(transactionsJson, HttpStatusCode.OK, jsonHeaders)
                    } else {
                        respond("denied", transactionsStatus)
                    }

                    else -> respond("{}", HttpStatusCode.OK, jsonHeaders)
                }
            },
        ) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        },
    )

    private fun repo(aisp: Aisp): TransactionDetailRepositoryImpl = TransactionDetailRepositoryImpl(
        store = BankingStores.transactionDetailsStore(aisp),
        networkMonitor = FakeNetworkMonitor(NetworkStatus.Available(onlineInfo)),
        fetchedAtRepository = FakeFetchedAtRepository(),
    )

    @Test
    fun theStreamReachesContentWithTheMappedRichList() = runTest(UnconfinedTestDispatcher()) {
        val state = repo(aisp(HttpStatusCode.OK))
            .transactionDetailStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Content }

        val list = assertIs<ScreenState.Content<List<TransactionDetail>>>(state).data
        assertEquals(listOf("TX-1", "TX-2"), list.map { it.transactionId })
        assertEquals("Tesco Stores", list.first().merchantName)
        assertEquals(TransactionCategory.GROCERIES, list.first().category)
        assertEquals("447.63", list.first().balanceAmount)
    }

    @Test
    fun aForbiddenResponseClassifiesToAnErrorCarryingForbidden() = runTest(UnconfinedTestDispatcher()) {
        val state = repo(aisp(HttpStatusCode.Forbidden))
            .transactionDetailStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Error || it is ScreenState.Unauthenticated }

        val error = assertIs<ScreenState.Error>(state)
        assertIs<NetworkError.Client.Forbidden>(assertIs<RemoteException>(error.error).networkError)
    }

    private companion object {
        const val ACCOUNT_ID = "40051512345678"
    }
}
