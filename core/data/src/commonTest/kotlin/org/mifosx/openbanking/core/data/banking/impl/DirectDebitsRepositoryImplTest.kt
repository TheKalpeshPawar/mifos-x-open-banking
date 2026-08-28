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
import org.mifosx.openbanking.core.model.banking.DirectDebitsSummary
import org.mifosx.openbanking.core.model.hsbcProduct.AccountEndpoint
import org.mifosx.openbanking.core.network.api.Aisp
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * End-to-end over [DirectDebitsRepositoryImpl] and [BankingStores.directDebitsStore]: a real Store5
 * store fed by a Ktor [MockEngine], so the OBIE payload travels the same path it does in the app —
 * deserialization, mapping, sort, tallies and [ScreenState] classification included.
 */
class DirectDebitsRepositoryImplTest {

    private val onlineInfo = NetworkInfo(type = NetworkType.WiFi, isMetered = false)
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private val fourMandates = """
        {"Data":{"DirectDebit":[
          {"AccountId":"40051512345678","DirectDebitId":"DD-004","DirectDebitStatusCode":"Inactive",
           "Name":"TV Licensing","PreviousPaymentDateTime":"2026-03-01T00:00:00Z",
           "PreviousPaymentAmount":{"Amount":"13.25","Currency":"GBP"},
           "MandateRelatedInformation":{"MandateIdentification":"DD-TVL-55667"}},
          {"AccountId":"40051512345678","DirectDebitId":"DD-001","DirectDebitStatusCode":"ACTV",
           "Name":"British Gas","PreviousPaymentDateTime":"2026-06-15T00:00:00Z",
           "PreviousPaymentAmount":{"Amount":"78.00","Currency":"GBP"},
           "MandateRelatedInformation":{"MandateIdentification":"DD-BG-44120"}},
          {"AccountId":"40051512345678","DirectDebitId":"DD-002","DirectDebitStatusCode":"ACTV",
           "Name":"Vodafone","PreviousPaymentDateTime":"2026-06-20T00:00:00Z",
           "PreviousPaymentAmount":{"Amount":"29.00","Currency":"GBP"},
           "MandateRelatedInformation":{"MandateIdentification":"DD-VF-88301"}},
          {"AccountId":"40051512345678","DirectDebitId":"DD-003","DirectDebitStatusCode":"ACTV",
           "Name":"Aviva Insurance","PreviousPaymentDateTime":"2026-06-05T00:00:00Z",
           "PreviousPaymentAmount":{"Amount":"41.50","Currency":"GBP"},
           "MandateRelatedInformation":{"MandateIdentification":"DD-AV-10293"}}
        ]},"Meta":{"TotalPages":1}}
    """.trimIndent()

    private val noMandates = """{"Data":{"DirectDebit":[]},"Meta":{"TotalPages":1}}"""

    private fun mockClient(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = HttpClient(MockEngine(handler)) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    private fun aisp(body: String, status: HttpStatusCode = HttpStatusCode.OK): Aisp =
        Aisp(mockClient { respond(body, status, jsonHeaders) })

    private fun repo(
        body: String = fourMandates,
        status: HttpStatusCode = HttpStatusCode.OK,
        fetchedAt: FakeFetchedAtRepository = FakeFetchedAtRepository(),
        capabilityRegistry: FakeAccountCapabilityRegistry = FakeAccountCapabilityRegistry(),
    ): DirectDebitsRepositoryImpl = DirectDebitsRepositoryImpl(
        store = BankingStores.directDebitsStore(
            aisp = aisp(body, status),
            capabilityRegistry = capabilityRegistry,
        ),
        networkMonitor = FakeNetworkMonitor(NetworkStatus.Available(onlineInfo)),
        fetchedAtRepository = fetchedAt,
    )

    @Test
    fun directDebitsStateEmitsContentSortedActiveFirstWithTallies() = runTest(UnconfinedTestDispatcher()) {
        val state = repo()
            .directDebitsStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Content }

        val summary = assertIs<ScreenState.Content<DirectDebitsSummary>>(state).data
        assertEquals(
            listOf("British Gas", "Vodafone", "Aviva Insurance", "TV Licensing"),
            summary.items.map { it.name },
        )
        assertEquals(3, summary.activeCount)
        assertEquals(1, summary.inactiveCount)
    }

    @Test
    fun contentCarriesTheMappedMandateFields() = runTest(UnconfinedTestDispatcher()) {
        val state = repo()
            .directDebitsStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Content }

        val first = assertIs<ScreenState.Content<DirectDebitsSummary>>(state).data.items.first()
        assertEquals("DD-BG-44120", first.mandateId)
        assertEquals("78.00", first.previousPaymentAmount)
        assertEquals("GBP", first.currency)
        assertEquals("2026-06-15T00:00:00Z", first.previousPaymentDateTime)
        assertTrue(first.isActive)
    }

    @Test
    fun directDebitsStateUsesThePerAccountCacheKey() = runTest(UnconfinedTestDispatcher()) {
        val fetchedAt = FakeFetchedAtRepository()
        repo(fetchedAt = fetchedAt)
            .directDebitsStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Content }

        assertTrue("directDebits:mandates:$ACCOUNT_ID" in fetchedAt.readKeys)
    }

    /**
     * The stream reports an account with no mandates as `Content` carrying an empty summary. Turning
     * that into an empty screen state is the view model's `emptyIfContent` step, asserted there.
     */
    @Test
    fun anAccountWithNoMandatesIsContentCarryingAnEmptySummary() = runTest(UnconfinedTestDispatcher()) {
        val state = repo(body = noMandates)
            .directDebitsStream(ACCOUNT_ID, backgroundScope).state
            .first { it !is ScreenState.Loading }

        val summary = assertIs<ScreenState.Content<DirectDebitsSummary>>(state).data
        assertTrue(summary.isEmpty)
        assertTrue(summary.items.isEmpty())
    }

    @Test
    fun forbiddenSurfacesAsAnErrorCarryingTheForbiddenNetworkError() = runTest(UnconfinedTestDispatcher()) {
        val state = repo(body = "denied", status = HttpStatusCode.Forbidden)
            .directDebitsStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Error || it is ScreenState.Unauthenticated }

        val error = assertIs<ScreenState.Error>(state)
        assertIs<NetworkError.Client.Forbidden>(assertIs<RemoteException>(error.error).networkError)
    }

    @Test
    fun rateLimitedSurfacesAsAnErrorCarryingTheRateLimitedNetworkError() = runTest(UnconfinedTestDispatcher()) {
        val state = repo(body = "slow down", status = HttpStatusCode.TooManyRequests)
            .directDebitsStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Error }

        val error = assertIs<ScreenState.Error>(state)
        assertIs<NetworkError.Client.RateLimited>(assertIs<RemoteException>(error.error).networkError)
    }

    @Test
    fun serverFailureSurfacesAsAnError() = runTest(UnconfinedTestDispatcher()) {
        val state = repo(body = "boom", status = HttpStatusCode.InternalServerError)
            .directDebitsStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Error }

        assertIs<ScreenState.Error>(state)
    }

    /**
     * One repository instance is shared across screens, so a second account must be given its own
     * stream and its own cache key rather than the first account's.
     */
    @Test
    fun aSecondAccountOnTheSameInstanceUsesItsOwnCacheKey() = runTest(UnconfinedTestDispatcher()) {
        val fetchedAt = FakeFetchedAtRepository()
        val repository = repo(fetchedAt = fetchedAt)

        repository.directDebitsStream(ACCOUNT_ID, backgroundScope).state.first { it is ScreenState.Content }
        repository.directDebitsStream(OTHER_ACCOUNT_ID, backgroundScope).state.first { it is ScreenState.Content }

        assertTrue("directDebits:mandates:$ACCOUNT_ID" in fetchedAt.readKeys)
        assertTrue("directDebits:mandates:$OTHER_ACCOUNT_ID" in fetchedAt.readKeys)
    }

    @Test
    fun refreshingAStreamReEmitsContent() = runTest(UnconfinedTestDispatcher()) {
        val repository = repo()
        val stream = repository.directDebitsStream(ACCOUNT_ID, backgroundScope)
        stream.state.first { it is ScreenState.Content }

        stream.refresh()

        val state = stream.state.first { it is ScreenState.Content }
        assertIs<ScreenState.Content<DirectDebitsSummary>>(state)
    }

    /**
     * The runtime half of capability detection.
     *
     * Recorded in the fetcher rather than a ViewModel because this is the one point every call
     * passes through — including a deep link that never touched the account-detail chips — so the
     * refusal is noticed however the screen was reached.
     */
    @Test
    fun aU000RefusalIsRecordedInTheCapabilityRegistry() = runTest(UnconfinedTestDispatcher()) {
        val registry = FakeAccountCapabilityRegistry()

        repo(body = U000_BODY, status = HttpStatusCode.BadRequest, capabilityRegistry = registry)
            .directDebitsStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Error }

        assertEquals(listOf(ACCOUNT_ID to AccountEndpoint.DirectDebits), registry.marked)
    }

    /** A 400 for any other reason must not permanently hide a feature that works. */
    @Test
    fun aBadRequestWithoutU000IsNotRecordedInTheCapabilityRegistry() = runTest(UnconfinedTestDispatcher()) {
        val registry = FakeAccountCapabilityRegistry()
        val body = """{"Code":"400","Errors":[{"ErrorCode":"UK.OBIE.Field.Invalid"}]}"""

        repo(body = body, status = HttpStatusCode.BadRequest, capabilityRegistry = registry)
            .directDebitsStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Error }

        assertTrue(registry.marked.isEmpty(), "expected nothing recorded, got ${registry.marked}")
    }

    private companion object {
        const val ACCOUNT_ID = "40051512345678"
        const val OTHER_ACCOUNT_ID = "40051599999999"

        /** Captured from the HSBC sandbox verbatim. */
        const val U000_BODY = """
            {"Code":"400","Id":"842f0682-ba4a-4f17-9107-a5ce8b98fdbd","Message":"Bad Request",
             "Errors":[{"ErrorCode":"U000",
                        "Message":"This action is not allowed on the account type in the request"}]}
        """
    }
}
