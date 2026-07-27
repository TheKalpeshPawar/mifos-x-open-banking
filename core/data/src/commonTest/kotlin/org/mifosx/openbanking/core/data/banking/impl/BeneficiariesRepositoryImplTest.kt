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
import org.mifosx.openbanking.core.model.banking.BeneficiaryItem
import org.mifosx.openbanking.core.model.banking.BeneficiaryScheme
import org.mifosx.openbanking.core.network.api.Aisp
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * End-to-end over [BeneficiariesRepositoryImpl] and [BankingStores.beneficiariesStore]: a real Store5
 * store fed by a Ktor [MockEngine], so the OBIE `OBReadBeneficiary5` payload travels the same path it
 * does in the app — deserialization, `toBeneficiaryItems` mapping, scheme parse and [ScreenState]
 * classification. Only the network is faked.
 *
 * The four HTTP failures the screen distinguishes each get a case, because the beneficiaries screen
 * routes 403 to a different recovery (re-authorise) than the rest (retry).
 */
class BeneficiariesRepositoryImplTest {

    private val onlineInfo = NetworkInfo(type = NetworkType.WiFi, isMetered = false)
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private val threePayees = """
        {"Data":{"Beneficiary":[
          ${payee("BEN-001", "Jameson Lettings", "UK.OBIE.SortCodeAccountNumber", "40-12-09 65872310", "RENT-FLAT12")},
          ${payee("BEN-003", "EDF Energy", "UK.OBIE.SortCodeAccountNumber", "60-00-01 99887766", "ELEC-8841")},
          ${payee("BEN-005", "Priya Rajan N26 GmbH", "UK.OBIE.IBAN", "DE89370400440532013000", "TRAVEL-EUR")}
        ]},"Meta":{"TotalPages":1}}
    """.trimIndent()

    private val noPayees = """{"Data":{"Beneficiary":[]},"Meta":{"TotalPages":1}}"""

    private fun mockClient(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = HttpClient(MockEngine(handler)) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    private fun aisp(body: String, status: HttpStatusCode = HttpStatusCode.OK): Aisp =
        Aisp(mockClient { respond(body, status, jsonHeaders) })

    private fun repo(
        body: String = threePayees,
        status: HttpStatusCode = HttpStatusCode.OK,
        fetchedAt: FakeFetchedAtRepository = FakeFetchedAtRepository(),
    ): BeneficiariesRepositoryImpl = BeneficiariesRepositoryImpl(
        store = BankingStores.beneficiariesStore(aisp = aisp(body, status)),
        networkMonitor = FakeNetworkMonitor(NetworkStatus.Available(onlineInfo)),
        fetchedAtRepository = fetchedAt,
    )

    @Test
    fun streamEmitsContentCarryingTheMappedList() = runTest(UnconfinedTestDispatcher()) {
        val state = repo()
            .beneficiariesStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Content }

        val list = assertIs<ScreenState.Content<List<BeneficiaryItem>>>(state).data
        assertEquals(EXPECTED_COUNT, list.size)

        val first = list.first()
        assertEquals("BEN-001", first.beneficiaryId)
        assertEquals(ACCOUNT_ID, first.accountId)
        assertEquals("Jameson Lettings", first.creditorName)
        assertEquals(BeneficiaryScheme.SortCode, first.scheme)
        assertEquals("40-12-09 65872310", first.identification)
        assertEquals("RENT-FLAT12", first.reference)
    }

    @Test
    fun anIbanPayeeKeepsItsUnspacedIdentification() = runTest(UnconfinedTestDispatcher()) {
        val state = repo().beneficiariesStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Content }

        val iban = assertIs<ScreenState.Content<List<BeneficiaryItem>>>(state).data.last()
        assertEquals(BeneficiaryScheme.Iban, iban.scheme)
        assertEquals("DE89370400440532013000", iban.identification)
    }

    @Test
    fun anAccountWithNoBeneficiariesIsContentCarryingAnEmptyList() = runTest(UnconfinedTestDispatcher()) {
        val state = repo(body = noPayees)
            .beneficiariesStream(ACCOUNT_ID, backgroundScope).state
            .first { it !is ScreenState.Loading }

        assertTrue(assertIs<ScreenState.Content<List<BeneficiaryItem>>>(state).data.isEmpty())
    }

    @Test
    fun theStreamUsesThePerAccountCacheKey() = runTest(UnconfinedTestDispatcher()) {
        val fetchedAt = FakeFetchedAtRepository()
        repo(fetchedAt = fetchedAt)
            .beneficiariesStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Content }

        assertTrue("beneficiaries:list:$ACCOUNT_ID" in fetchedAt.readKeys)
    }

    @Test
    fun aSecondAccountOnTheSameInstanceUsesItsOwnCacheKey() = runTest(UnconfinedTestDispatcher()) {
        val fetchedAt = FakeFetchedAtRepository()
        val repository = repo(fetchedAt = fetchedAt)

        repository.beneficiariesStream(ACCOUNT_ID, backgroundScope).state.first { it is ScreenState.Content }
        repository.beneficiariesStream(OTHER_ACCOUNT_ID, backgroundScope).state.first { it is ScreenState.Content }

        assertTrue("beneficiaries:list:$ACCOUNT_ID" in fetchedAt.readKeys)
        assertTrue("beneficiaries:list:$OTHER_ACCOUNT_ID" in fetchedAt.readKeys)
    }

    @Test
    fun forbiddenSurfacesAsAnErrorCarryingTheForbiddenNetworkError() = runTest(UnconfinedTestDispatcher()) {
        val state = repo(body = "denied", status = HttpStatusCode.Forbidden)
            .beneficiariesStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Error || it is ScreenState.Unauthenticated }

        val error = assertIs<ScreenState.Error>(state)
        assertIs<NetworkError.Client.Forbidden>(assertIs<RemoteException>(error.error).networkError)
    }

    @Test
    fun rateLimitedSurfacesAsAnErrorCarryingTheRateLimitedNetworkError() = runTest(UnconfinedTestDispatcher()) {
        val state = repo(body = "slow down", status = HttpStatusCode.TooManyRequests)
            .beneficiariesStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Error }

        val error = assertIs<ScreenState.Error>(state)
        assertIs<NetworkError.Client.RateLimited>(assertIs<RemoteException>(error.error).networkError)
    }

    @Test
    fun serverErrorSurfacesAsAnErrorCarryingTheServerNetworkError() = runTest(UnconfinedTestDispatcher()) {
        val state = repo(body = "boom", status = HttpStatusCode.InternalServerError)
            .beneficiariesStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Error }

        val error = assertIs<ScreenState.Error>(state)
        assertIs<NetworkError.Server>(assertIs<RemoteException>(error.error).networkError)
    }

    @Test
    fun refreshingAStreamReEmitsContent() = runTest(UnconfinedTestDispatcher()) {
        val stream = repo().beneficiariesStream(ACCOUNT_ID, backgroundScope)
        stream.state.first { it is ScreenState.Content }

        stream.refresh()

        assertIs<ScreenState.Content<List<BeneficiaryItem>>>(stream.state.first { it is ScreenState.Content })
    }

    private companion object {
        const val ACCOUNT_ID = "40051512345678"
        const val OTHER_ACCOUNT_ID = "40051599999999"
        const val EXPECTED_COUNT = 3

        fun payee(
            id: String,
            name: String,
            scheme: String,
            identification: String,
            reference: String,
        ): String = """
            {"AccountId":"$ACCOUNT_ID","BeneficiaryId":"$id","Reference":"$reference",
             "CreditorAccount":{"SchemeName":"$scheme","Identification":"$identification","Name":"$name"}}
        """.trimIndent()
    }
}
