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

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import org.mifosx.openbanking.core.network.api.Aisp
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Covers [StatementFileRepositoryImpl] over [Aisp.getStatementFile] with a Ktor [MockEngine]: a
 * success returns the raw bytes untouched, a 501 is surfaced as [NetworkError.Server], and the
 * request asks the bank for a PDF or CSV via its `Accept` header.
 */
class StatementFileRepositoryImplTest {

    private val pdfBytes = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D) // "%PDF-"

    @Test
    fun aSuccessfulDownloadReturnsTheRawBytes() = runTest {
        val repository = StatementFileRepositoryImpl(
            aisp = Aisp(
                HttpClient(
                    MockEngine {
                        respond(
                            content = ByteReadChannel(pdfBytes),
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/pdf"),
                        )
                    },
                ),
            ),
        )

        val result = repository.downloadStatementFile(ACCOUNT_ID, STATEMENT_ID)

        val success = assertIs<NetworkResult.Success<ByteArray>>(result)
        assertEquals(pdfBytes.toList(), success.data.toList())
    }

    @Test
    fun theRequestAsksTheBankForAPdfOrCsv() = runTest {
        var acceptHeader: String? = null
        val repository = StatementFileRepositoryImpl(
            aisp = Aisp(
                HttpClient(
                    MockEngine { request ->
                        acceptHeader = request.headers.getAll(HttpHeaders.Accept)?.joinToString(",")
                        respond(
                            content = ByteReadChannel(pdfBytes),
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/pdf"),
                        )
                    },
                ),
            ),
        )

        repository.downloadStatementFile(ACCOUNT_ID, STATEMENT_ID)

        val accept = requireNotNull(acceptHeader)
        assertContains(accept, "application/pdf")
        assertContains(accept, "csv")
    }

    @Test
    fun aStatementWithNoDownloadableFileSurfacesAs501Server() = runTest {
        val repository = StatementFileRepositoryImpl(
            aisp = Aisp(
                HttpClient(
                    MockEngine {
                        respond(
                            content = "not implemented",
                            status = HttpStatusCode.NotImplemented,
                        )
                    },
                ),
            ),
        )

        val result = repository.downloadStatementFile(ACCOUNT_ID, STATEMENT_ID)

        val error = assertIs<NetworkResult.Error<NetworkError>>(result)
        val server = assertIs<NetworkError.Server>(error.error)
        assertEquals(501, server.statusCode)
    }

    private companion object {
        const val ACCOUNT_ID = "40051512345678"
        const val STATEMENT_ID = "STMT-2026-05-40051512345678"
    }
}
