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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.mifosx.openbanking.core.data.banking.store.BankingStores
import org.mifosx.openbanking.core.data.banking.store.FakeFetchedAtRepository
import org.mifosx.openbanking.core.data.banking.store.FakeNetworkMonitor
import org.mifosx.openbanking.core.data.util.RemoteException
import org.mifosx.openbanking.core.model.banking.PartyProfile
import org.mifosx.openbanking.core.network.api.Aisp
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * End-to-end over [ProfileRepositoryImpl] and [BankingStores.partyStore]: a real Store5 store fed by
 * a Ktor [MockEngine], so the OBIE payload travels the same path it does in the app —
 * deserialization, mapping and [ScreenState] classification included.
 */
class ProfileRepositoryImplTest {

    private val onlineInfo = NetworkInfo(type = NetworkType.WiFi, isMetered = false)
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun partyBody(name: String, email: String, partyId: String) = """
        {"Data":{"Party":{
          "PartyId":"$partyId","PartyNumber":"7458","PartyType":"Sole",
          "Name":"$name","FullLegalName":"$name","LegalStructure":"UK.OBIE.Individual",
          "AccountRole":"UK.OBIE.Principal","EmailAddress":"$email",
          "Phone":"+44 20 7946 0000","Mobile":"+44 7700 900482"
        }},"Meta":{"TotalPages":1}}
    """.trimIndent()

    private val priya = partyBody("Priya Sharma", "priya.sharma@example.co.uk", "55786146")
    private val daniel = partyBody("Daniel Okafor", "daniel.okafor@example.co.uk", "55786147")
    private val noParty = """{"Data":{},"Meta":{"TotalPages":1}}"""

    private fun mockClient(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = HttpClient(MockEngine(handler)) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    private fun aisp(body: String, status: HttpStatusCode = HttpStatusCode.OK): Aisp =
        Aisp(mockClient { respond(body, status, jsonHeaders) })

    /** Serves a different party per account so a mis-keyed store is visible in the CONTENT. */
    private fun perAccountAisp(): Aisp = Aisp(
        mockClient { request ->
            val body = if (OTHER_ACCOUNT_ID in request.url.encodedPath) daniel else priya
            respond(body, HttpStatusCode.OK, jsonHeaders)
        },
    )

    private fun repo(
        body: String = priya,
        status: HttpStatusCode = HttpStatusCode.OK,
        fetchedAt: FakeFetchedAtRepository = FakeFetchedAtRepository(),
        aisp: Aisp = aisp(body, status),
    ): ProfileRepositoryImpl = ProfileRepositoryImpl(
        store = BankingStores.partyStore(aisp = aisp),
        networkMonitor = FakeNetworkMonitor(NetworkStatus.Available(onlineInfo)),
        fetchedAtRepository = fetchedAt,
    )

    @Test
    fun profileStateEmitsContentCarryingTheMappedParty() = runTest(UnconfinedTestDispatcher()) {
        val state = repo()
            .profileStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Content }

        val profile = assertIs<ScreenState.Content<PartyProfile>>(state).data
        assertEquals("Priya Sharma", profile.displayName)
        assertEquals("PS", profile.initials)
        assertEquals("Personal Account Holder", profile.roleLabel)
    }

    @Test
    fun contentCarriesTheIdentityFields() = runTest(UnconfinedTestDispatcher()) {
        val state = repo()
            .profileStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Content }

        val profile = assertIs<ScreenState.Content<PartyProfile>>(state).data
        assertEquals("priya.sharma@example.co.uk", profile.email)
        assertEquals("+44 7700 900482", profile.mobile)
        assertEquals("55786146", profile.partyId)
    }

    /** The sandbox returns no Address, so the row has nothing to render. */
    @Test
    fun theSandboxPayloadCarriesNoAddressLine() = runTest(UnconfinedTestDispatcher()) {
        val state = repo()
            .profileStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Content }

        assertEquals("", assertIs<ScreenState.Content<PartyProfile>>(state).data.addressLine)
    }

    @Test
    fun profileStateUsesThePerAccountCacheKey() = runTest(UnconfinedTestDispatcher()) {
        val fetchedAt = FakeFetchedAtRepository()
        repo(fetchedAt = fetchedAt)
            .profileStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Content }

        assertTrue("party:profile:$ACCOUNT_ID" in fetchedAt.readKeys)
    }

    /**
     * A 200 with no Party block is the consent-does-not-cover-identity case. The stream reports it
     * as Content carrying an empty profile; turning that into an empty screen is the view model's
     * `emptyIfContent` step, asserted there.
     */
    @Test
    fun anAccountWithNoPartyDataIsContentCarryingAnEmptyProfile() = runTest(UnconfinedTestDispatcher()) {
        val state = repo(body = noParty)
            .profileStream(ACCOUNT_ID, backgroundScope).state
            .first { it !is ScreenState.Loading }

        assertTrue(assertIs<ScreenState.Content<PartyProfile>>(state).data.isEmpty)
    }

    @Test
    fun unauthorizedSurfacesAsAnErrorOrUnauthenticated() = runTest(UnconfinedTestDispatcher()) {
        val state = repo(body = "expired", status = HttpStatusCode.Unauthorized)
            .profileStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Error || it is ScreenState.Unauthenticated }

        assertTrue(state is ScreenState.Error || state is ScreenState.Unauthenticated)
    }

    @Test
    fun forbiddenSurfacesAsAnErrorCarryingTheForbiddenNetworkError() = runTest(UnconfinedTestDispatcher()) {
        val state = repo(body = "denied", status = HttpStatusCode.Forbidden)
            .profileStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Error || it is ScreenState.Unauthenticated }

        val error = assertIs<ScreenState.Error>(state)
        assertIs<NetworkError.Client.Forbidden>(assertIs<RemoteException>(error.error).networkError)
    }

    @Test
    fun rateLimitedSurfacesAsAnErrorCarryingTheRateLimitedNetworkError() = runTest(UnconfinedTestDispatcher()) {
        val state = repo(body = "slow down", status = HttpStatusCode.TooManyRequests)
            .profileStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Error }

        val error = assertIs<ScreenState.Error>(state)
        assertIs<NetworkError.Client.RateLimited>(assertIs<RemoteException>(error.error).networkError)
    }

    @Test
    fun serverFailureSurfacesAsAnError() = runTest(UnconfinedTestDispatcher()) {
        val state = repo(body = "boom", status = HttpStatusCode.InternalServerError)
            .profileStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Error }

        assertIs<ScreenState.Error>(state)
    }

    @Test
    fun refreshingAStreamReEmitsContent() = runTest(UnconfinedTestDispatcher()) {
        val stream = repo().profileStream(ACCOUNT_ID, backgroundScope)
        stream.state.first { it is ScreenState.Content }

        stream.refresh()

        val state = stream.state.first { it is ScreenState.Content }
        assertEquals("Priya Sharma", assertIs<ScreenState.Content<PartyProfile>>(state).data.displayName)
    }

    /**
     * One repository instance is shared across screens, so a second account must receive its own
     * account's data.
     *
     * Asserts the CONTENT, not the cache key: a store that ignored its key would still read both
     * keys and pass a key-only assertion, which is how a wrong-account defect stayed invisible.
     */
    @Test
    fun aSecondAccountOnTheSameInstanceReceivesItsOwnContent() = runTest(UnconfinedTestDispatcher()) {
        val repository = repo(aisp = perAccountAisp())

        val first = repository.profileStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Content }
        val second = repository.profileStream(OTHER_ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Content }

        assertEquals("Priya Sharma", assertIs<ScreenState.Content<PartyProfile>>(first).data.displayName)
        assertEquals("Daniel Okafor", assertIs<ScreenState.Content<PartyProfile>>(second).data.displayName)
    }

    /**
     * A repository holds no stream of its own, so one built on a scope that has since died must not
     * be handed to the next caller. A memoised `private var stream` fails exactly here.
     */
    @Test
    fun aStreamOnACancelledScopeIsReplacedByALiveOne() = runTest(UnconfinedTestDispatcher()) {
        val repository = repo()
        val deadScope = CoroutineScope(Dispatchers.Unconfined)

        repository.profileStream(ACCOUNT_ID, deadScope).state.first { it is ScreenState.Content }
        deadScope.cancel()

        val revived = repository.profileStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Content }

        assertEquals("Priya Sharma", assertIs<ScreenState.Content<PartyProfile>>(revived).data.displayName)
    }

    private companion object {
        const val ACCOUNT_ID = "40051512345678"
        const val OTHER_ACCOUNT_ID = "40051599999999"
    }
}
