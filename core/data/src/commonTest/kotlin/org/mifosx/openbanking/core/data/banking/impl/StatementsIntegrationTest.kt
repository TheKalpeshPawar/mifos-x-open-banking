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
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.mifosx.openbanking.core.data.banking.store.BankingStores
import org.mifosx.openbanking.core.data.banking.store.FakeFetchedAtRepository
import org.mifosx.openbanking.core.data.banking.store.FakeNetworkMonitor
import org.mifosx.openbanking.core.model.banking.StatementPeriod
import org.mifosx.openbanking.core.network.api.Aisp
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Drives the statements read AND file-download seams over one Ktor [MockEngine] routing on
 * `request.url.encodedPath`, wiring the real [StatementsRepositoryImpl] (over the real
 * [BankingStores.statementsStore]) and the real [StatementFileRepositoryImpl] against the same
 * [Aisp] the app uses. Only the network is faked.
 */
class StatementsIntegrationTest {

    private val onlineInfo = NetworkInfo(type = NetworkType.WiFi, isMetered = false)
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    private val pdfBytes = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D)

    private val statementsJson = """
        {"Data":{"Statement":[
          ${stmt("2026-04", "APR-2026-STMT", "2026-04-01T00:00:00Z", "2610.40")},
          ${stmt("2026-05", "MAY-2026-STMT", "2026-05-01T00:00:00Z", "2847.63")},
          ${stmt("2025-12", "DEC-2025-STMT", "2025-12-01T00:00:00Z", "1502.88")}
        ]},"Meta":{"TotalPages":1}}
    """.trimIndent()

    private fun aisp(fileStatus: HttpStatusCode): Aisp = Aisp(
        HttpClient(
            MockEngine { request ->
                val path = request.url.encodedPath
                when {
                    path.endsWith("/file") -> if (fileStatus == HttpStatusCode.OK) {
                        respond(
                            ByteReadChannel(pdfBytes),
                            HttpStatusCode.OK,
                            headersOf(HttpHeaders.ContentType, "application/pdf"),
                        )
                    } else {
                        respond("no file", fileStatus)
                    }

                    path.endsWith("/statements") -> respond(statementsJson, HttpStatusCode.OK, jsonHeaders)

                    else -> respond("{}", HttpStatusCode.OK, jsonHeaders)
                }
            },
        ) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        },
    )

    private fun statementsRepo(aisp: Aisp): StatementsRepositoryImpl = StatementsRepositoryImpl(
        store = BankingStores.statementsStore(aisp),
        networkMonitor = FakeNetworkMonitor(NetworkStatus.Available(onlineInfo)),
        fetchedAtRepository = FakeFetchedAtRepository(),
    )

    @Test
    fun theStreamReachesContentWithThreeSortedPeriods() = runTest(UnconfinedTestDispatcher()) {
        val state = statementsRepo(aisp(HttpStatusCode.OK))
            .statementsStream(ACCOUNT_ID, backgroundScope).state
            .first { it is ScreenState.Content }

        val periods = assertIs<ScreenState.Content<List<StatementPeriod>>>(state).data
        assertEquals(
            listOf("MAY-2026-STMT", "APR-2026-STMT", "DEC-2025-STMT"),
            periods.map { it.statementReference },
        )
        assertEquals("2847.63", periods.first().closingBalanceAmount)
    }

    @Test
    fun theDownloadPathReturnsTheBankBytes() = runTest(UnconfinedTestDispatcher()) {
        val fileRepo = StatementFileRepositoryImpl(aisp(HttpStatusCode.OK))

        val result = fileRepo.downloadStatementFile(ACCOUNT_ID, STATEMENT_ID)

        assertEquals(pdfBytes.toList(), assertIs<NetworkResult.Success<ByteArray>>(result).data.toList())
    }

    @Test
    fun a501OnTheDownloadPathSurfacesAsServer501() = runTest(UnconfinedTestDispatcher()) {
        val fileRepo = StatementFileRepositoryImpl(aisp(HttpStatusCode.NotImplemented))

        val result = fileRepo.downloadStatementFile(ACCOUNT_ID, STATEMENT_ID)

        val server = assertIs<NetworkError.Server>(assertIs<NetworkResult.Error<NetworkError>>(result).error)
        assertEquals(501, server.statusCode)
    }

    private companion object {
        const val ACCOUNT_ID = "40051512345678"
        const val STATEMENT_ID = "STMT-2026-05-40051512345678"

        fun stmt(month: String, reference: String, startDateTime: String, closing: String): String = """
            {"AccountId":"$ACCOUNT_ID","StatementId":"STMT-$month-$ACCOUNT_ID",
             "StatementReference":"$reference","StartDateTime":"$startDateTime","EndDateTime":"$startDateTime",
             "StatementAmount":[{"Type":"ClosingBalance","Amount":{"Amount":"$closing","Currency":"GBP"}}]}
        """.trimIndent()
    }
}
