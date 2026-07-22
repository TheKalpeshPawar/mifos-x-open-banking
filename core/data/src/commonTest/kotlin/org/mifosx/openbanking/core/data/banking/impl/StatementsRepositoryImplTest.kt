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
import org.mifosx.openbanking.core.model.banking.StatementPeriod
import org.mifosx.openbanking.core.network.api.Aisp
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * End-to-end over [StatementsRepositoryImpl] and [BankingStores.statementsStore]: a real Store5 store
 * fed by a Ktor [MockEngine], so the OBIE payload travels the same path it does in the app —
 * deserialization, mapping, sort, [StatementPeriod] projection and [ScreenState] classification.
 *
 * The mock returns the six periods deliberately out of order, so the newest-first sort is proven by
 * the emitted content rather than assumed from the fixture.
 */
class StatementsRepositoryImplTest {

    private val onlineInfo = NetworkInfo(type = NetworkType.WiFi, isMetered = false)
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    /** Six periods, shuffled — Mar, May, Jan, Dec, Apr, Feb — to force the sort to do real work. */
    private val sixStatements = """
        {"Data":{"Statement":[
          ${statementJson("2026-03", "MAR-2026-STMT", "2026-03-01T00:00:00Z", "2314.92")},
          ${statementJson("2026-05", "MAY-2026-STMT", "2026-05-01T00:00:00Z", "2847.63")},
          ${statementJson("2026-01", "JAN-2026-STMT", "2026-01-01T00:00:00Z", "1754.10")},
          ${statementJson("2025-12", "DEC-2025-STMT", "2025-12-01T00:00:00Z", "1502.88")},
          ${statementJson("2026-04", "APR-2026-STMT", "2026-04-01T00:00:00Z", "2610.40")},
          ${statementJson("2026-02", "FEB-2026-STMT", "2026-02-01T00:00:00Z", "1988.57")}
        ]},"Meta":{"TotalPages":1}}
    """.trimIndent()

    private val noStatements = """{"Data":{"Statement":[]},"Meta":{"TotalPages":1}}"""

    private fun mockClient(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = HttpClient(MockEngine(handler)) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    private fun aisp(body: String, status: HttpStatusCode = HttpStatusCode.OK): Aisp =
        Aisp(mockClient { respond(body, status, jsonHeaders) })

    private fun repo(
        body: String = sixStatements,
        status: HttpStatusCode = HttpStatusCode.OK,
        fetchedAt: FakeFetchedAtRepository = FakeFetchedAtRepository(),
    ): StatementsRepositoryImpl = StatementsRepositoryImpl(
        store = BankingStores.statementsStore(aisp(body, status)),
        networkMonitor = FakeNetworkMonitor(NetworkStatus.Available(onlineInfo)),
        fetchedAtRepository = fetchedAt,
    )

    @Test
    fun statementsStateEmitsContentSortedNewestFirst() = runTest(UnconfinedTestDispatcher()) {
        val state = repo()
            .statementsStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Content }

        val periods = assertIs<ScreenState.Content<List<StatementPeriod>>>(state).data
        assertEquals(EXPECTED_COUNT, periods.size)
        assertEquals(
            listOf(
                "STMT-2026-05-$ACCOUNT_ID",
                "STMT-2026-04-$ACCOUNT_ID",
                "STMT-2026-03-$ACCOUNT_ID",
                "STMT-2026-02-$ACCOUNT_ID",
                "STMT-2026-01-$ACCOUNT_ID",
                "STMT-2025-12-$ACCOUNT_ID",
            ),
            periods.map { it.statementId },
        )
    }

    @Test
    fun contentCarriesTheClosingBalanceNotTheOpeningBalance() = runTest(UnconfinedTestDispatcher()) {
        val state = repo()
            .statementsStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Content }

        val first = assertIs<ScreenState.Content<List<StatementPeriod>>>(state).data.first()
        assertEquals("MAY-2026-STMT", first.statementReference)
        assertEquals("2847.63", first.closingBalanceAmount)
        assertEquals("GBP", first.closingBalanceCurrency)
        assertEquals("2026-05-01T00:00:00Z", first.startDateTime)
    }

    @Test
    fun statementsStateUsesThePerAccountCacheKey() = runTest(UnconfinedTestDispatcher()) {
        val fetchedAt = FakeFetchedAtRepository()
        repo(fetchedAt = fetchedAt)
            .statementsStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Content }

        assertTrue("statements:$ACCOUNT_ID" in fetchedAt.readKeys)
    }

    @Test
    fun aSecondAccountOnTheSameInstanceUsesItsOwnCacheKey() = runTest(UnconfinedTestDispatcher()) {
        val fetchedAt = FakeFetchedAtRepository()
        val repository = repo(fetchedAt = fetchedAt)

        repository.statementsStream(ACCOUNT_ID, backgroundScope).state.first { it is ScreenState.Content }
        repository.statementsStream(OTHER_ACCOUNT_ID, backgroundScope).state.first { it is ScreenState.Content }

        assertTrue("statements:$ACCOUNT_ID" in fetchedAt.readKeys)
        assertTrue("statements:$OTHER_ACCOUNT_ID" in fetchedAt.readKeys)
    }

    @Test
    fun anAccountWithNoStatementsIsContentCarryingAnEmptyList() = runTest(UnconfinedTestDispatcher()) {
        val state = repo(body = noStatements)
            .statementsStream(ACCOUNT_ID, backgroundScope).state
            .first { it !is ScreenState.Loading }

        val periods = assertIs<ScreenState.Content<List<StatementPeriod>>>(state).data
        assertTrue(periods.isEmpty())
    }

    @Test
    fun tokenExpirySurfacesAsUnauthenticatedOrError() = runTest(UnconfinedTestDispatcher()) {
        val state = repo(body = "expired", status = HttpStatusCode.Unauthorized)
            .statementsStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Error || it is ScreenState.Unauthenticated }

        assertTrue(state is ScreenState.Error || state is ScreenState.Unauthenticated)
    }

    @Test
    fun forbiddenSurfacesAsAnErrorCarryingTheForbiddenNetworkError() = runTest(UnconfinedTestDispatcher()) {
        val state = repo(body = "denied", status = HttpStatusCode.Forbidden)
            .statementsStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Error || it is ScreenState.Unauthenticated }

        val error = assertIs<ScreenState.Error>(state)
        assertIs<NetworkError.Client.Forbidden>(assertIs<RemoteException>(error.error).networkError)
    }

    @Test
    fun rateLimitedSurfacesAsAnErrorCarryingTheRateLimitedNetworkError() = runTest(UnconfinedTestDispatcher()) {
        val state = repo(body = "slow down", status = HttpStatusCode.TooManyRequests)
            .statementsStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Error }

        val error = assertIs<ScreenState.Error>(state)
        assertIs<NetworkError.Client.RateLimited>(assertIs<RemoteException>(error.error).networkError)
    }

    @Test
    fun serverFailureSurfacesAsAnError() = runTest(UnconfinedTestDispatcher()) {
        val state = repo(body = "boom", status = HttpStatusCode.InternalServerError)
            .statementsStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Error }

        val error = assertIs<ScreenState.Error>(state)
        assertIs<NetworkError.Server>(assertIs<RemoteException>(error.error).networkError)
    }

    @Test
    fun refreshingAStreamReEmitsContent() = runTest(UnconfinedTestDispatcher()) {
        val repository = repo()
        val stream = repository.statementsStream(ACCOUNT_ID, backgroundScope)
        stream.state.first { it is ScreenState.Content }

        stream.refresh()

        assertIs<ScreenState.Content<List<StatementPeriod>>>(stream.state.first { it is ScreenState.Content })
    }

    private companion object {
        const val ACCOUNT_ID = "40051512345678"
        const val OTHER_ACCOUNT_ID = "40051599999999"
        const val EXPECTED_COUNT = 6

        /** One OBIE `OBStatement2` object with an Opening + Closing balance pair. */
        fun statementJson(
            month: String,
            reference: String,
            startDateTime: String,
            closingBalance: String,
        ): String = """
            {"AccountId":"$ACCOUNT_ID","StatementId":"STMT-$month-$ACCOUNT_ID",
             "StatementReference":"$reference","Type":"RegularPeriodic",
             "StartDateTime":"$startDateTime","EndDateTime":"$startDateTime",
             "StatementAmount":[
               {"Type":"OpeningBalance","CreditDebitIndicator":"Credit",
                "Amount":{"Amount":"0.00","Currency":"GBP"}},
               {"Type":"ClosingBalance","CreditDebitIndicator":"Credit",
                "Amount":{"Amount":"$closingBalance","Currency":"GBP"}}
             ]}
        """.trimIndent()
    }
}
