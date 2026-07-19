/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.model.ais.statements

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatementsSerializationTest {

    private val json = Json { prettyPrint = false }

    private fun sampleStatement() = Statement(
        accountId = "acc-1",
        statementId = "stmt-1",
        statementReference = "ref-1",
        type = "RegularPeriodic",
        startDateTime = "2026-01-01T00:00:00Z",
        endDateTime = "2026-01-31T23:59:59Z",
        creationDateTime = "2026-02-01T00:00:00Z",
        statementDateTime = listOf(
            StatementDateTime(dateTime = "2026-02-01T00:00:00Z", type = "IssuedDate"),
        ),
        statementValue = listOf(
            StatementValue(type = "AirMiles", value = "1200"),
        ),
        statementAmount = listOf(
            StatementAmount(
                creditDebitIndicator = "Credit",
                type = "ClosingBalance",
                amount = Amount(amount = "1500.00", currency = "GBP"),
            ),
        ),
        statementDescription = listOf("Monthly statement"),
        statementBenefit = listOf(
            StatementBenefit(type = "Cashback", amount = Amount(amount = "5.00", currency = "GBP")),
        ),
        statementFee = listOf(
            StatementFee(
                description = "Overdraft fee",
                creditDebitIndicator = "Debit",
                type = "ServiceCharge",
                rateType = "Gross",
                frequency = "Monthly",
                rate = JsonPrimitive("1.5"),
                amount = Amount(amount = "2.00", currency = "GBP"),
            ),
        ),
        statementInterest = listOf(
            StatementInterest(
                description = "Credit interest",
                creditDebitIndicator = "Credit",
                type = "InterestPaid",
                rateType = "Gross",
                frequency = "Monthly",
                rate = JsonPrimitive("0.25"),
                amount = Amount(amount = "3.00", currency = "GBP"),
            ),
        ),
        statementRate = listOf(
            StatementRate(rate = JsonPrimitive("2.0"), type = "AnnualBorrowingRate"),
        ),
    )

    private fun sampleResponse() = StatementsResponse(
        data = Data(statement = listOf(sampleStatement())),
        links = Links(self = "/self"),
        meta = Meta(totalPages = 4),
    )

    @Test
    fun `full statements response round-trips through JSON`() {
        val original = sampleResponse()
        val encoded = json.encodeToString(StatementsResponse.serializer(), original)
        val decoded = json.decodeFromString(StatementsResponse.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `statement fields survive the round-trip`() {
        val original = sampleResponse()
        val encoded = json.encodeToString(StatementsResponse.serializer(), original)
        val decoded = json.decodeFromString(StatementsResponse.serializer(), encoded)
        val statement = decoded.data?.statement?.first()
        assertEquals("stmt-1", statement?.statementId)
        assertEquals("ClosingBalance", statement?.statementAmount?.first()?.type)
        assertEquals("1500.00", statement?.statementAmount?.first()?.amount?.amount)
        assertEquals("Cashback", statement?.statementBenefit?.first()?.type)
        assertEquals("Overdraft fee", statement?.statementFee?.first()?.description)
        assertEquals("AnnualBorrowingRate", statement?.statementRate?.first()?.type)
        assertEquals("/self", decoded.links?.self)
        assertEquals(4, decoded.meta?.totalPages)
    }

    @Test
    fun `data class helpers are exercised`() {
        val statement = sampleStatement()
        assertEquals(statement, statement.copy())
        assertEquals(statement.hashCode(), statement.copy().hashCode())
        assertTrue(statement.toString().contains("stmt-1"))
        val fee = statement.statementFee?.first()
        assertEquals(fee, fee?.copy())
        assertTrue(fee.toString().contains("Overdraft fee"))
        val defaults = StatementsResponse()
        assertEquals(StatementsResponse(), defaults)
    }
}
