/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking.mapper

import org.mifosx.openbanking.core.network.model.ais.statements.Amount
import org.mifosx.openbanking.core.network.model.ais.statements.Data
import org.mifosx.openbanking.core.network.model.ais.statements.Statement
import org.mifosx.openbanking.core.network.model.ais.statements.StatementAmount
import org.mifosx.openbanking.core.network.model.ais.statements.StatementsResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers [StatementsResponse.toStatementPeriods]: the closing-balance pick, the newest-first sort
 * and the drop of any statement missing a `StatementId`.
 */
class StatementMapperTest {

    @Test
    fun statementsAreSortedNewestFirstByStartDateTime() {
        val periods = response(
            statement("s-mar", "2026-03-01T00:00:00Z", closing = "3.00"),
            statement("s-may", "2026-05-01T00:00:00Z", closing = "5.00"),
            statement("s-jan", "2026-01-01T00:00:00Z", closing = "1.00"),
        ).toStatementPeriods()

        assertEquals(listOf("s-may", "s-mar", "s-jan"), periods.map { it.statementId })
    }

    @Test
    fun theClosingBalanceIsPickedAndTheOpeningBalanceIgnored() {
        val period = response(
            Statement(
                statementId = "s-1",
                statementReference = "REF-1",
                startDateTime = "2026-05-01T00:00:00Z",
                endDateTime = "2026-05-31T23:59:59Z",
                statementAmount = listOf(
                    StatementAmount(type = "OpeningBalance", amount = Amount("100.00", "GBP")),
                    StatementAmount(type = "ClosingBalance", amount = Amount("2847.63", "GBP")),
                ),
            ),
        ).toStatementPeriods().single()

        assertEquals("2847.63", period.closingBalanceAmount)
        assertEquals("GBP", period.closingBalanceCurrency)
        assertEquals("REF-1", period.statementReference)
        assertEquals("2026-05-01T00:00:00Z", period.startDateTime)
        assertEquals("2026-05-31T23:59:59Z", period.endDateTime)
    }

    @Test
    fun aStatementWithoutAStatementIdIsDropped() {
        val periods = response(
            statement("s-keep", "2026-05-01T00:00:00Z", closing = "5.00"),
            Statement(statementId = null, startDateTime = "2026-04-01T00:00:00Z"),
            Statement(statementId = "  ", startDateTime = "2026-03-01T00:00:00Z"),
        ).toStatementPeriods()

        assertEquals(listOf("s-keep"), periods.map { it.statementId })
    }

    @Test
    fun aStatementWithNoClosingBalanceDegradesToEmptyStringsRatherThanDropping() {
        val period = response(
            Statement(
                statementId = "s-2",
                startDateTime = "2026-05-01T00:00:00Z",
                statementAmount = listOf(
                    StatementAmount(type = "OpeningBalance", amount = Amount("100.00", "GBP")),
                ),
            ),
        ).toStatementPeriods().single()

        assertEquals("", period.closingBalanceAmount)
        assertEquals("", period.closingBalanceCurrency)
        assertEquals("", period.statementReference)
    }

    @Test
    fun anEmptyPayloadMapsToAnEmptyList() {
        assertTrue(StatementsResponse(data = Data(statement = emptyList())).toStatementPeriods().isEmpty())
        assertTrue(StatementsResponse(data = null).toStatementPeriods().isEmpty())
    }

    private fun response(vararg statements: Statement): StatementsResponse =
        StatementsResponse(data = Data(statement = statements.toList()))

    private fun statement(id: String, startDateTime: String, closing: String): Statement = Statement(
        statementId = id,
        statementReference = "$id-ref",
        startDateTime = startDateTime,
        endDateTime = startDateTime,
        statementAmount = listOf(
            StatementAmount(type = "ClosingBalance", amount = Amount(closing, "GBP")),
        ),
    )
}
