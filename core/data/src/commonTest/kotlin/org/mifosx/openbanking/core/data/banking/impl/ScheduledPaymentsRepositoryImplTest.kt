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
import org.mifosx.openbanking.core.data.banking.FakeAccountCapabilityRegistry
import org.mifosx.openbanking.core.data.banking.store.BankingStores
import org.mifosx.openbanking.core.data.banking.store.FakeFetchedAtRepository
import org.mifosx.openbanking.core.data.banking.store.FakeNetworkMonitor
import org.mifosx.openbanking.core.data.util.RemoteException
import org.mifosx.openbanking.core.model.banking.ScheduledPaymentItem
import org.mifosx.openbanking.core.model.banking.ScheduledPaymentType
import org.mifosx.openbanking.core.network.api.Aisp
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * End-to-end over [ScheduledPaymentsRepositoryImpl] and [BankingStores.scheduledPaymentsStore]: a
 * real Store5 store fed by a Ktor [MockEngine], so the OBIE `OBReadScheduledPayment3` payload travels
 * the same path it does in the app — deserialization, [ScheduledPaymentsResponse.toScheduledPaymentItems]
 * mapping, ScheduledType parse and [ScreenState] classification. Only the network is faked.
 */
class ScheduledPaymentsRepositoryImplTest {

    private val onlineInfo = NetworkInfo(type = NetworkType.WiFi, isMetered = false)
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private val twoPayments = """
        {"Data":{"ScheduledPayment":[
          ${payment("SP-001", "Execution", "HMRC Self Assessment", "842.00", "HMRC-SA-2526", "08-32-00 12001039")},
          ${payment("SP-003", "Arrival", "Direct Line Insurance", "412.50", "DL-HOME-INS-26", "20-00-00 73428901")}
        ]},"Meta":{"TotalPages":1}}
    """.trimIndent()

    private val noPayments = """{"Data":{"ScheduledPayment":[]},"Meta":{"TotalPages":1}}"""

    private fun mockClient(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = HttpClient(MockEngine(handler)) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    private fun aisp(body: String, status: HttpStatusCode = HttpStatusCode.OK): Aisp =
        Aisp(mockClient { respond(body, status, jsonHeaders) })

    private fun repo(
        body: String = twoPayments,
        status: HttpStatusCode = HttpStatusCode.OK,
        fetchedAt: FakeFetchedAtRepository = FakeFetchedAtRepository(),
    ): ScheduledPaymentsRepositoryImpl = ScheduledPaymentsRepositoryImpl(
        store = BankingStores.scheduledPaymentsStore(
            aisp = aisp(body, status),
            capabilityRegistry = FakeAccountCapabilityRegistry(),
        ),
        networkMonitor = FakeNetworkMonitor(NetworkStatus.Available(onlineInfo)),
        fetchedAtRepository = fetchedAt,
    )

    @Test
    fun streamEmitsContentCarryingTheMappedList() = runTest(UnconfinedTestDispatcher()) {
        val state = repo()
            .scheduledPaymentsStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Content }

        val list = assertIs<ScreenState.Content<List<ScheduledPaymentItem>>>(state).data
        assertEquals(EXPECTED_COUNT, list.size)

        val execution = list.first()
        assertEquals("SP-001", execution.scheduledPaymentId)
        assertEquals(ACCOUNT_ID, execution.accountId)
        assertEquals("HMRC Self Assessment", execution.payeeName)
        assertEquals("842.00", execution.amount)
        assertEquals("GBP", execution.currency)
        assertEquals(ScheduledPaymentType.Execution, execution.scheduledType)
        assertEquals("HMRC-SA-2526", execution.reference)
        assertEquals("08-32-00 12001039", execution.creditorIdentification)

        assertEquals(ScheduledPaymentType.Arrival, list[1].scheduledType)
    }

    @Test
    fun anAccountWithNoScheduledPaymentsIsContentCarryingAnEmptyList() = runTest(UnconfinedTestDispatcher()) {
        val state = repo(body = noPayments)
            .scheduledPaymentsStream(ACCOUNT_ID, backgroundScope).state
            .first { it !is ScreenState.Loading }

        assertTrue(assertIs<ScreenState.Content<List<ScheduledPaymentItem>>>(state).data.isEmpty())
    }

    @Test
    fun theStreamUsesThePerAccountCacheKey() = runTest(UnconfinedTestDispatcher()) {
        val fetchedAt = FakeFetchedAtRepository()
        repo(fetchedAt = fetchedAt)
            .scheduledPaymentsStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Content }

        assertTrue("scheduledPayments:list:$ACCOUNT_ID" in fetchedAt.readKeys)
    }

    @Test
    fun aSecondAccountOnTheSameInstanceUsesItsOwnCacheKey() = runTest(UnconfinedTestDispatcher()) {
        val fetchedAt = FakeFetchedAtRepository()
        val repository = repo(fetchedAt = fetchedAt)

        repository.scheduledPaymentsStream(ACCOUNT_ID, backgroundScope).state.first { it is ScreenState.Content }
        repository.scheduledPaymentsStream(OTHER_ACCOUNT_ID, backgroundScope).state.first { it is ScreenState.Content }

        assertTrue("scheduledPayments:list:$ACCOUNT_ID" in fetchedAt.readKeys)
        assertTrue("scheduledPayments:list:$OTHER_ACCOUNT_ID" in fetchedAt.readKeys)
    }

    @Test
    fun forbiddenSurfacesAsAnErrorCarryingTheForbiddenNetworkError() = runTest(UnconfinedTestDispatcher()) {
        val state = repo(body = "denied", status = HttpStatusCode.Forbidden)
            .scheduledPaymentsStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Error || it is ScreenState.Unauthenticated }

        val error = assertIs<ScreenState.Error>(state)
        assertIs<NetworkError.Client.Forbidden>(assertIs<RemoteException>(error.error).networkError)
    }

    @Test
    fun rateLimitedSurfacesAsAnErrorCarryingTheRateLimitedNetworkError() = runTest(UnconfinedTestDispatcher()) {
        val state = repo(body = "slow down", status = HttpStatusCode.TooManyRequests)
            .scheduledPaymentsStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Error }

        val error = assertIs<ScreenState.Error>(state)
        assertIs<NetworkError.Client.RateLimited>(assertIs<RemoteException>(error.error).networkError)
    }

    @Test
    fun refreshingAStreamReEmitsContent() = runTest(UnconfinedTestDispatcher()) {
        val stream = repo().scheduledPaymentsStream(ACCOUNT_ID, backgroundScope)
        stream.state.first { it is ScreenState.Content }

        stream.refresh()

        assertIs<ScreenState.Content<List<ScheduledPaymentItem>>>(stream.state.first { it is ScreenState.Content })
    }

    private companion object {
        const val ACCOUNT_ID = "40051512345678"
        const val OTHER_ACCOUNT_ID = "40051599999999"
        const val EXPECTED_COUNT = 2

        @Suppress("LongParameterList")
        fun payment(
            id: String,
            type: String,
            name: String,
            amount: String,
            reference: String,
            identification: String,
        ): String = """
            {"AccountId":"$ACCOUNT_ID","ScheduledPaymentId":"$id",
             "ScheduledPaymentDateTime":"2026-07-31T00:00:00Z","ScheduledType":"$type",
             "Reference":"$reference","InstructedAmount":{"Amount":"$amount","Currency":"GBP"},
             "CreditorAccount":{"SchemeName":"UK.OBIE.SortCodeAccountNumber",
              "Identification":"$identification","Name":"$name"}}
        """.trimIndent()
    }
}
