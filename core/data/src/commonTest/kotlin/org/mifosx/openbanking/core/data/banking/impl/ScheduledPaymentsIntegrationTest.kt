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

/**
 * Drives the scheduled-payments read over one Ktor [MockEngine] routing on `request.url.encodedPath`,
 * wiring the real [ScheduledPaymentsRepositoryImpl] over the real
 * [BankingStores.scheduledPaymentsStore] over the same [Aisp] the app uses. Only the network is faked.
 */
class ScheduledPaymentsIntegrationTest {

    private val onlineInfo = NetworkInfo(type = NetworkType.WiFi, isMetered = false)
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private val scheduledPaymentsJson = """
        {"Data":{"ScheduledPayment":[
          ${payment("SP-001", "Execution", "HMRC Self Assessment", "842.00", "08-32-00 12001039")},
          ${payment("SP-003", "Arrival", "Direct Line Insurance", "412.50", "20-00-00 73428901")}
        ]},"Meta":{"TotalPages":1}}
    """.trimIndent()

    private fun aisp(status: HttpStatusCode): Aisp = Aisp(
        HttpClient(
            MockEngine { request ->
                val path = request.url.encodedPath
                when {
                    path.endsWith("/scheduled-payments") -> if (status == HttpStatusCode.OK) {
                        respond(scheduledPaymentsJson, HttpStatusCode.OK, jsonHeaders)
                    } else {
                        respond("denied", status, jsonHeaders)
                    }

                    else -> respond("{}", HttpStatusCode.OK, jsonHeaders)
                }
            },
        ) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        },
    )

    private fun repo(status: HttpStatusCode = HttpStatusCode.OK): ScheduledPaymentsRepositoryImpl =
        ScheduledPaymentsRepositoryImpl(
            store = BankingStores.scheduledPaymentsStore(
                aisp = aisp(status),
                capabilityRegistry = FakeAccountCapabilityRegistry(),
            ),
            networkMonitor = FakeNetworkMonitor(NetworkStatus.Available(onlineInfo)),
            fetchedAtRepository = FakeFetchedAtRepository(),
        )

    @Test
    fun theStreamReachesContentWithBothScheduledTypes() = runTest(UnconfinedTestDispatcher()) {
        val state = repo()
            .scheduledPaymentsStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Content }

        val payments = assertIs<ScreenState.Content<List<ScheduledPaymentItem>>>(state).data
        assertEquals(
            listOf("HMRC Self Assessment", "Direct Line Insurance"),
            payments.map { it.payeeName },
        )
        assertEquals(ScheduledPaymentType.Execution, payments.first().scheduledType)
        assertEquals(ScheduledPaymentType.Arrival, payments[1].scheduledType)
    }

    @Test
    fun aForbiddenSurfacesAsAnErrorCarryingTheForbiddenNetworkError() = runTest(UnconfinedTestDispatcher()) {
        val state = repo(status = HttpStatusCode.Forbidden)
            .scheduledPaymentsStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Error || it is ScreenState.Unauthenticated }

        val error = assertIs<ScreenState.Error>(state)
        assertIs<NetworkError.Client.Forbidden>(assertIs<RemoteException>(error.error).networkError)
    }

    private companion object {
        const val ACCOUNT_ID = "40051512345678"

        fun payment(
            id: String,
            type: String,
            name: String,
            amount: String,
            identification: String,
        ): String = """
            {"AccountId":"$ACCOUNT_ID","ScheduledPaymentId":"$id",
             "ScheduledPaymentDateTime":"2026-07-31T00:00:00Z","ScheduledType":"$type",
             "Reference":"REF-$id","InstructedAmount":{"Amount":"$amount","Currency":"GBP"},
             "CreditorAccount":{"SchemeName":"UK.OBIE.SortCodeAccountNumber",
              "Identification":"$identification","Name":"$name"}}
        """.trimIndent()
    }
}
