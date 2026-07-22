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
import kotlin.test.assertTrue

/**
 * End-to-end over [StatementDetailRepositoryImpl] and both banking stores: real Store5 stores fed by a
 * Ktor [MockEngine] routed on `encodedPath`, so the OBIE payloads travel the same path they do in the
 * app — the composite `"$accountId|$statementId"` key, deserialization, mapping and [ScreenState]
 * classification.
 */
class StatementDetailRepositoryImplTest {

    private val onlineInfo = NetworkInfo(type = NetworkType.WiFi, isMetered = false)
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun mockClient(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = HttpClient(MockEngine(handler)) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    private fun aisp(
        statementBody: String = STATEMENT_JSON,
        statementStatus: HttpStatusCode = HttpStatusCode.OK,
    ): Aisp = Aisp(
        mockClient { request ->
            when {
                request.url.encodedPath.endsWith("/transactions") ->
                    respond(TRANSACTIONS_JSON, HttpStatusCode.OK, jsonHeaders)

                statementStatus == HttpStatusCode.OK -> respond(statementBody, HttpStatusCode.OK, jsonHeaders)
                else -> respond("denied", statementStatus)
            }
        },
    )

    private fun repo(
        aisp: Aisp = aisp(),
        fetchedAt: FakeFetchedAtRepository = FakeFetchedAtRepository(),
    ): StatementDetailRepositoryImpl = StatementDetailRepositoryImpl(
        detailStore = BankingStores.statementDetailStore(aisp),
        transactionsStore = BankingStores.statementTransactionsStore(aisp),
        networkMonitor = FakeNetworkMonitor(NetworkStatus.Available(onlineInfo)),
        fetchedAtRepository = fetchedAt,
    )

    @Test
    fun statementStreamReachesContentWithTheMappedStatement() = runTest(UnconfinedTestDispatcher()) {
        val state = repo()
            .statementStream(ACCOUNT_ID, STATEMENT_ID, backgroundScope).state
            .first { it is ScreenState.Content }

        val detail = assertIs<ScreenState.Content<StatementDetail>>(state).data
        assertEquals("MAY-2026-STMT", detail.reference)
        assertEquals(2, detail.balances.size)
        assertEquals("2847.63", detail.balances[1].amount)
        assertEquals("Monthly maintenance fee", detail.fees.single().description)
        assertEquals("In-credit interest", detail.interest.single().description)
    }

    @Test
    fun transactionsStreamReachesContentWithTheMappedRows() = runTest(UnconfinedTestDispatcher()) {
        val state = repo()
            .statementTransactionsStream(ACCOUNT_ID, STATEMENT_ID, backgroundScope).state
            .first { it is ScreenState.Content }

        val rows = assertIs<ScreenState.Content<List<TransactionItem>>>(state).data
        assertEquals(listOf("TXN-2026-05-001", "TXN-2026-05-002"), rows.map { it.transactionId })
        assertTrue(rows[1].isCredit)
    }

    @Test
    fun bothStreamsUseTheirOwnCompositeCacheKey() = runTest(UnconfinedTestDispatcher()) {
        val fetchedAt = FakeFetchedAtRepository()
        val repository = repo(fetchedAt = fetchedAt)

        repository.statementStream(ACCOUNT_ID, STATEMENT_ID, backgroundScope).state.first { it is ScreenState.Content }
        repository.statementTransactionsStream(ACCOUNT_ID, STATEMENT_ID, backgroundScope).state
            .first { it is ScreenState.Content }

        assertTrue("statementDetail:meta:$ACCOUNT_ID:$STATEMENT_ID" in fetchedAt.readKeys)
        assertTrue("statementDetail:txns:$ACCOUNT_ID:$STATEMENT_ID" in fetchedAt.readKeys)
    }

    @Test
    fun anEmptyStatementArraySurfacesAsAnError() = runTest(UnconfinedTestDispatcher()) {
        val state = repo(aisp(statementBody = """{"Data":{"Statement":[]}}"""))
            .statementStream(ACCOUNT_ID, STATEMENT_ID, backgroundScope).state
            .first { it is ScreenState.Error }

        assertIs<ScreenState.Error>(state)
    }

    @Test
    fun forbiddenSurfacesAsAnErrorCarryingForbidden() = runTest(UnconfinedTestDispatcher()) {
        val state = repo(aisp(statementStatus = HttpStatusCode.Forbidden))
            .statementStream(ACCOUNT_ID, STATEMENT_ID, backgroundScope).state
            .first { it is ScreenState.Error || it is ScreenState.Unauthenticated }

        val error = assertIs<ScreenState.Error>(state)
        assertIs<NetworkError.Client.Forbidden>(assertIs<RemoteException>(error.error).networkError)
    }

    @Test
    fun refreshingTheStatementStreamReEmitsContent() = runTest(UnconfinedTestDispatcher()) {
        val stream = repo().statementStream(ACCOUNT_ID, STATEMENT_ID, backgroundScope)
        stream.state.first { it is ScreenState.Content }

        stream.refresh()

        assertIs<ScreenState.Content<StatementDetail>>(stream.state.first { it is ScreenState.Content })
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
                 {"Type":"OpeningBalance","CreditDebitIndicator":"Credit","Amount":{"Amount":"2610.40","Currency":"GBP"}},
                 {"Type":"ClosingBalance","CreditDebitIndicator":"Credit","Amount":{"Amount":"2847.63","Currency":"GBP"}}
               ],
               "StatementFee":[
                 {"Description":"Monthly maintenance fee","CreditDebitIndicator":"Debit","Amount":{"Amount":"0.00","Currency":"GBP"}}
               ],
               "StatementInterest":[
                 {"Description":"In-credit interest","CreditDebitIndicator":"Credit","Amount":{"Amount":"0.21","Currency":"GBP"}}
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
