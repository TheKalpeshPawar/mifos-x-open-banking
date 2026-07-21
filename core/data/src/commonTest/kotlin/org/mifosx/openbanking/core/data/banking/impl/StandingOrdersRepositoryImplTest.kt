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
import org.mifosx.openbanking.core.model.banking.StandingOrdersSummary
import org.mifosx.openbanking.core.model.hsbcProduct.AccountEndpoint
import org.mifosx.openbanking.core.network.api.Aisp
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * End-to-end over [StandingOrdersRepositoryImpl] and [BankingStores.standingOrdersStore]: a real
 * Store5 store fed by a Ktor [MockEngine], so the OBIE payload travels the same path it does in the
 * app — deserialization, frequency decode, sort, tallies and [ScreenState] classification included.
 */
class StandingOrdersRepositoryImplTest {

    private val onlineInfo = NetworkInfo(type = NetworkType.WiFi, isMetered = false)
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private val fiveOrders = """
        {"Data":{"StandingOrder":[
          {"AccountId":"40051512345678","StandingOrderId":"SO-004","StandingOrderStatusCode":"Inactive",
           "Reference":"CHARITY-DON","NextPaymentAmount":{"Amount":"10.00","Currency":"GBP"},
           "CreditorAccount":{"SchemeName":"UK.OBIE.SortCodeAccountNumber",
             "Identification":"08-60-01 20321982","Name":"Oxfam GB"},
           "MandateRelatedInformation":{"FinalPaymentDateTime":"2025-12-28T00:00:00Z",
             "Frequency":{"Type":"IntrvlMnthDay","PointInTime":"01:28"}}},
          {"AccountId":"40051512345678","StandingOrderId":"SO-001","StandingOrderStatusCode":"Active",
           "Reference":"RENT-FLAT12","NextPaymentDateTime":"2026-07-01T00:00:00Z",
           "NextPaymentAmount":{"Amount":"1200.00","Currency":"GBP"},
           "CreditorAccount":{"SchemeName":"UK.OBIE.SortCodeAccountNumber",
             "Identification":"40-12-09 65872310","Name":"Jameson Lettings"},
           "MandateRelatedInformation":{"Frequency":{"Type":"IntrvlMnthDay","PointInTime":"01:01"}}},
          {"AccountId":"40051512345678","StandingOrderId":"SO-002","StandingOrderStatusCode":"Active",
           "Reference":"ISA-TOPUP","NextPaymentDateTime":"2026-07-01T00:00:00Z",
           "NextPaymentAmount":{"Amount":"200.00","Currency":"GBP"},
           "CreditorAccount":{"SchemeName":"UK.OBIE.SortCodeAccountNumber",
             "Identification":"60-16-13 31926819","Name":"ISA Saver"},
           "MandateRelatedInformation":{"Frequency":{"Type":"IntrvlMnthDay","PointInTime":"01:01"}}},
          {"AccountId":"40051512345678","StandingOrderId":"SO-003","StandingOrderStatusCode":"Active",
           "Reference":"GYM-MBR","NextPaymentDateTime":"2026-07-15T00:00:00Z",
           "NextPaymentAmount":{"Amount":"24.99","Currency":"GBP"},
           "CreditorAccount":{"SchemeName":"UK.OBIE.SortCodeAccountNumber",
             "Identification":"20-00-00 55512345","Name":"PureGym"},
           "MandateRelatedInformation":{"Frequency":{"Type":"IntrvlMnthDay","PointInTime":"01:15"}}},
          {"AccountId":"40051512345678","StandingOrderId":"SO-005","StandingOrderStatusCode":"Active",
           "Reference":"SAVINGS-SWEEP","NextPaymentDateTime":"2026-07-04T00:00:00Z",
           "NextPaymentAmount":{"Amount":"50.00","Currency":"GBP"},
           "CreditorAccount":{"SchemeName":"UK.OBIE.SortCodeAccountNumber",
             "Identification":"30-96-22 41227714","Name":"Marcus Savings"},
           "MandateRelatedInformation":{"FinalPaymentDateTime":"2026-12-25T00:00:00Z",
             "Frequency":{"Type":"IntrvlWkDay","PointInTime":"01:5"}}}
        ]},"Meta":{"TotalPages":1}}
    """.trimIndent()

    private val noOrders = """{"Data":{"StandingOrder":[]},"Meta":{"TotalPages":1}}"""

    private fun mockClient(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = HttpClient(MockEngine(handler)) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    private fun aisp(body: String, status: HttpStatusCode = HttpStatusCode.OK): Aisp =
        Aisp(mockClient { respond(body, status, jsonHeaders) })

    private fun repo(
        body: String = fiveOrders,
        status: HttpStatusCode = HttpStatusCode.OK,
        fetchedAt: FakeFetchedAtRepository = FakeFetchedAtRepository(),
        capabilityRegistry: FakeAccountCapabilityRegistry = FakeAccountCapabilityRegistry(),
    ): StandingOrdersRepositoryImpl = StandingOrdersRepositoryImpl(
        store = BankingStores.standingOrdersStore(
            aisp = aisp(body, status),
            capabilityRegistry = capabilityRegistry,
        ),
        networkMonitor = FakeNetworkMonitor(NetworkStatus.Available(onlineInfo)),
        fetchedAtRepository = fetchedAt,
    )

    private suspend fun contentSummary(
        repository: StandingOrdersRepositoryImpl,
        scope: kotlinx.coroutines.CoroutineScope,
    ): StandingOrdersSummary {
        val state = repository
            .standingOrdersStream(ACCOUNT_ID, scope).state
            .first { it is ScreenState.Content }
        return assertIs<ScreenState.Content<StandingOrdersSummary>>(state).data
    }

    @Test
    fun standingOrdersStateEmitsContentSortedActiveFirstWithTallies() = runTest(UnconfinedTestDispatcher()) {
        val summary = contentSummary(repo(), backgroundScope)

        assertEquals(
            listOf("Jameson Lettings", "ISA Saver", "PureGym", "Marcus Savings", "Oxfam GB"),
            summary.items.map { it.payeeName },
        )
        assertEquals(4, summary.activeCount)
        assertEquals(1, summary.inactiveCount)
    }

    @Test
    fun contentCarriesTheMappedOrderFields() = runTest(UnconfinedTestDispatcher()) {
        val first = contentSummary(repo(), backgroundScope).items.first()

        assertEquals("SO-001", first.standingOrderId)
        assertEquals("1200.00", first.nextPaymentAmount)
        assertEquals("GBP", first.currency)
        assertEquals("2026-07-01T00:00:00Z", first.nextPaymentDateTime)
        assertEquals("40-12-09 65872310", first.creditorIdentification)
        assertEquals("RENT-FLAT12", first.reference)
        assertTrue(first.isActive)
    }

    @Test
    fun frequencyCodesAreDecodedOnTheWayThroughTheStore() = runTest(UnconfinedTestDispatcher()) {
        val items = contentSummary(repo(), backgroundScope).items

        assertEquals("Monthly on the 1st", items.first().frequencyLabel)
        assertEquals("Weekly every Friday", items.single { it.standingOrderId == "SO-005" }.frequencyLabel)
        assertEquals("Monthly on the 28th", items.last().frequencyLabel)
    }

    @Test
    fun finalPaymentFlagSurvivesTheStoreRoundTrip() = runTest(UnconfinedTestDispatcher()) {
        val items = contentSummary(repo(), backgroundScope).items

        assertTrue(items.single { it.standingOrderId == "SO-005" }.hasFinalPayment)
        assertTrue(!items.first().hasFinalPayment)
    }

    @Test
    fun standingOrdersStateUsesThePerAccountCacheKey() = runTest(UnconfinedTestDispatcher()) {
        val fetchedAt = FakeFetchedAtRepository()
        contentSummary(repo(fetchedAt = fetchedAt), backgroundScope)

        assertTrue("standingOrders:orders:$ACCOUNT_ID" in fetchedAt.readKeys)
    }

    /**
     * The empty-list case the `emptyIfContent` decision guards: a zero-order account must report
     * Empty, not spin on Loading forever.
     */
    @Test
    fun anAccountWithNoOrdersIsContentCarryingAnEmptySummary() = runTest(UnconfinedTestDispatcher()) {
        val state = repo(body = noOrders)
            .standingOrdersStream(ACCOUNT_ID, backgroundScope).state
            .first { it !is ScreenState.Loading }

        val summary = assertIs<ScreenState.Content<StandingOrdersSummary>>(state).data
        assertTrue(summary.isEmpty)
        assertTrue(summary.items.isEmpty())
    }

    @Test
    fun forbiddenSurfacesAsAnErrorCarryingTheForbiddenNetworkError() = runTest(UnconfinedTestDispatcher()) {
        val state = repo(body = "denied", status = HttpStatusCode.Forbidden)
            .standingOrdersStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Error || it is ScreenState.Unauthenticated }

        val error = assertIs<ScreenState.Error>(state)
        assertIs<NetworkError.Client.Forbidden>(assertIs<RemoteException>(error.error).networkError)
    }

    @Test
    fun rateLimitedSurfacesAsAnErrorCarryingTheRateLimitedNetworkError() = runTest(UnconfinedTestDispatcher()) {
        val state = repo(body = "slow down", status = HttpStatusCode.TooManyRequests)
            .standingOrdersStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Error }

        val error = assertIs<ScreenState.Error>(state)
        assertIs<NetworkError.Client.RateLimited>(assertIs<RemoteException>(error.error).networkError)
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
            .standingOrdersStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Error }

        assertEquals(listOf(ACCOUNT_ID to AccountEndpoint.StandingOrders), registry.marked)
    }

    /** A 400 for any other reason must not permanently hide a feature that works. */
    @Test
    fun aBadRequestWithoutU000IsNotRecordedInTheCapabilityRegistry() = runTest(UnconfinedTestDispatcher()) {
        val registry = FakeAccountCapabilityRegistry()
        val body = """{"Code":"400","Errors":[{"ErrorCode":"UK.OBIE.Field.Invalid"}]}"""

        repo(body = body, status = HttpStatusCode.BadRequest, capabilityRegistry = registry)
            .standingOrdersStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Error }

        assertTrue(registry.marked.isEmpty(), "expected nothing recorded, got ${registry.marked}")
    }

    @Test
    fun serverFailureSurfacesAsAnError() = runTest(UnconfinedTestDispatcher()) {
        val state = repo(body = "boom", status = HttpStatusCode.InternalServerError)
            .standingOrdersStream(ACCOUNT_ID, backgroundScope).state
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

        repository.standingOrdersStream(ACCOUNT_ID, backgroundScope).state.first { it is ScreenState.Content }
        repository.standingOrdersStream(OTHER_ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Content }

        assertTrue("standingOrders:orders:$ACCOUNT_ID" in fetchedAt.readKeys)
        assertTrue("standingOrders:orders:$OTHER_ACCOUNT_ID" in fetchedAt.readKeys)
    }

    @Test
    fun refreshingAStreamReEmitsContent() = runTest(UnconfinedTestDispatcher()) {
        val repository = repo()
        val stream = repository.standingOrdersStream(ACCOUNT_ID, backgroundScope)
        stream.state.first { it is ScreenState.Content }

        stream.refresh()

        val state = stream.state.first { it is ScreenState.Content }
        assertEquals(5, assertIs<ScreenState.Content<StandingOrdersSummary>>(state).data.items.size)
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
