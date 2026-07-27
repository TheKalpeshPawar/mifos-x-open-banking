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
import org.mifosx.openbanking.core.model.banking.StatementDetail
import org.mifosx.openbanking.core.model.banking.TransactionItem
import org.mifosx.openbanking.core.network.api.Aisp
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Drives the statement-detail read over one Ktor [MockEngine] routing on `request.url.encodedPath`,
 * wiring the real [StatementDetailRepositoryImpl] over the real [BankingStores.statementDetailStore] and
 * [BankingStores.statementTransactionsStore] over the same [Aisp] the app uses. Only the network is
 * faked: both streams reach Content off one PSU session (the merge substrate the screen renders), and a
 * `403` on the statement endpoint classifies to an error carrying [NetworkError.Client.Forbidden].
 */
class StatementDetailIntegrationTest {

    private val onlineInfo = NetworkInfo(type = NetworkType.WiFi, isMetered = false)
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun aisp(statementStatus: HttpStatusCode): Aisp = Aisp(
        HttpClient(
            MockEngine { request ->
                route(request, statementStatus)
            },
        ) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        },
    )

    private fun MockRequestHandleScope.route(
        request: HttpRequestData,
        statementStatus: HttpStatusCode,
    ): HttpResponseData = when {
        request.url.encodedPath.endsWith("/transactions") ->
            respond(TRANSACTIONS_JSON, HttpStatusCode.OK, jsonHeaders)

        statementStatus == HttpStatusCode.OK -> respond(STATEMENT_JSON, HttpStatusCode.OK, jsonHeaders)
        else -> respond("denied", statementStatus)
    }

    private fun repo(aisp: Aisp): StatementDetailRepositoryImpl = StatementDetailRepositoryImpl(
        detailStore = BankingStores.statementDetailStore(aisp),
        transactionsStore = BankingStores.statementTransactionsStore(aisp),
        networkMonitor = FakeNetworkMonitor(NetworkStatus.Available(onlineInfo)),
        fetchedAtRepository = FakeFetchedAtRepository(),
    )

    @Test
    fun bothStreamsReachContentFromOneSession() = runTest(UnconfinedTestDispatcher()) {
        val repository = repo(aisp(HttpStatusCode.OK))

        val statement = repository.statementStream(ACCOUNT_ID, STATEMENT_ID, backgroundScope).state
            .first { it is ScreenState.Content }
        val transactions = repository.statementTransactionsStream(ACCOUNT_ID, STATEMENT_ID, backgroundScope).state
            .first { it is ScreenState.Content }

        assertEquals("MAY-2026-STMT", assertIs<ScreenState.Content<StatementDetail>>(statement).data.reference)
        assertEquals(
            listOf("TXN-2026-05-001", "TXN-2026-05-002"),
            assertIs<ScreenState.Content<List<TransactionItem>>>(transactions).data.map { it.transactionId },
        )
    }

    @Test
    fun aForbiddenStatementResponseClassifiesToForbidden() = runTest(UnconfinedTestDispatcher()) {
        val state = repo(aisp(HttpStatusCode.Forbidden))
            .statementStream(ACCOUNT_ID, STATEMENT_ID, backgroundScope).state
            .first { it is ScreenState.Error || it is ScreenState.Unauthenticated }

        val error = assertIs<ScreenState.Error>(state)
        assertIs<NetworkError.Client.Forbidden>(assertIs<RemoteException>(error.error).networkError)
    }

    private companion object {
        const val ACCOUNT_ID = "40051512345678"
        const val STATEMENT_ID = "STMT-2026-05-40051512345678"

        val STATEMENT_JSON = """
            {"Data":{"Statement":[
              {"AccountId":"40051512345678","StatementId":"STMT-2026-05-40051512345678",
               "StatementReference":"MAY-2026-STMT","Type":"RegularPeriodic",
               "StartDateTime":"2026-05-01T00:00:00Z","EndDateTime":"2026-05-31T23:59:59Z",
               "CreationDateTime":"2026-06-01T06:00:00Z",
               "StatementAmount":[
                 {"Type":"ClosingBalance","CreditDebitIndicator":"Credit","Amount":{"Amount":"2847.63","Currency":"GBP"}}
               ]}
            ]},"Meta":{"TotalPages":1}}
        """.trimIndent()

        val TRANSACTIONS_JSON = """
            {"Data":{"Transaction":[
              {"AccountId":"40051512345678","TransactionId":"TXN-2026-05-001","CreditDebitIndicator":"Debit",
               "Status":"Booked","BookingDateTime":"2026-05-03T09:14:22Z",
               "TransactionInformation":"TESCO STORES 3225 LONDON","Amount":{"Amount":"82.50","Currency":"GBP"}},
              {"AccountId":"40051512345678","TransactionId":"TXN-2026-05-002","CreditDebitIndicator":"Credit",
               "Status":"Booked","BookingDateTime":"2026-05-10T00:00:00Z",
               "TransactionInformation":"BACS CREDIT ACME CORP PAYROLL","Amount":{"Amount":"3200.00","Currency":"GBP"}}
            ]},"Meta":{"TotalPages":1}}
        """.trimIndent()
    }
}
