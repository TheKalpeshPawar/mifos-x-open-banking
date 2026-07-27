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

import org.mifosx.openbanking.core.network.model.ais.statementDetails.Amount
import org.mifosx.openbanking.core.network.model.ais.statementDetails.Data
import org.mifosx.openbanking.core.network.model.ais.statementDetails.Statement
import org.mifosx.openbanking.core.network.model.ais.statementDetails.StatementAmount
import org.mifosx.openbanking.core.network.model.ais.statementDetails.StatementDetailsResponse
import org.mifosx.openbanking.core.network.model.ais.statementDetails.StatementFee
import org.mifosx.openbanking.core.network.model.ais.statementDetails.StatementInterest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers [StatementDetailsResponse.toStatementDetail]: the matching-id pick with first-element fallback,
 * the credit/debit sense, and the null result an empty payload produces (which the store turns into an
 * error).
 */
class StatementDetailMapperTest {

    private val account = "40051512345678"
    private val statementId = "STMT-2026-05-40051512345678"

    @Test
    fun theStatementWhoseIdMatchesIsPicked() {
        val detail = response(
            statement("OTHER", reference = "OTHER-REF"),
            statement(statementId, reference = "MAY-2026-STMT"),
        ).toStatementDetail(account, statementId)

        assertEquals(statementId, detail?.statementId)
        assertEquals("MAY-2026-STMT", detail?.reference)
    }

    @Test
    fun theFirstStatementIsUsedWhenNoIdMatches() {
        val detail = response(
            statement("FIRST", reference = "FIRST-REF"),
            statement("SECOND", reference = "SECOND-REF"),
        ).toStatementDetail(account, statementId)

        assertEquals("FIRST", detail?.statementId)
        assertEquals("FIRST-REF", detail?.reference)
    }

    @Test
    fun anEmptyStatementArrayMapsToNull() {
        val emptyArray = StatementDetailsResponse(data = Data(statement = emptyList()))
        assertNull(emptyArray.toStatementDetail(account, statementId))
        assertNull(StatementDetailsResponse(data = null).toStatementDetail(account, statementId))
    }

    @Test
    fun balancesFeesAndInterestAreMappedWithCreditDebitSense() {
        val detail = response(
            Statement(
                statementId = statementId,
                statementReference = "MAY-2026-STMT",
                type = "RegularPeriodic",
                startDateTime = "2026-05-01T00:00:00Z",
                endDateTime = "2026-05-31T23:59:59Z",
                creationDateTime = "2026-06-01T06:00:00Z",
                statementAmount = listOf(
                    StatementAmount("Credit", "OpeningBalance", Amount("2610.40", "GBP")),
                    StatementAmount("Debit", "ClosingBalance", Amount("12.40", "GBP")),
                ),
                statementFee = listOf(
                    StatementFee(
                        description = "Monthly maintenance fee",
                        creditDebitIndicator = "Debit",
                        amount = Amount("0.00", "GBP"),
                    ),
                ),
                statementInterest = listOf(
                    StatementInterest(
                        description = "In-credit interest",
                        creditDebitIndicator = "Credit",
                        amount = Amount("0.21", "GBP"),
                    ),
                ),
            ),
        ).toStatementDetail(account, statementId)

        requireNotNull(detail)
        assertEquals("RegularPeriodic", detail.type)
        assertEquals("2026-05-01T00:00:00Z", detail.periodStart)
        assertEquals(2, detail.balances.size)
        assertTrue(detail.balances.first().isCredit)
        assertEquals("OpeningBalance", detail.balances.first().type)
        assertEquals("2610.40", detail.balances.first().amount)
        assertTrue(!detail.balances[1].isCredit)
        assertEquals("Monthly maintenance fee", detail.fees.single().description)
        assertTrue(!detail.fees.single().isCredit)
        assertEquals("In-credit interest", detail.interest.single().description)
        assertTrue(detail.interest.single().isCredit)
    }

    @Test
    fun missingFieldsDegradeToEmptyStringsAndDefaultAccountId() {
        val detail = response(Statement(statementId = statementId)).toStatementDetail(account, statementId)

        requireNotNull(detail)
        assertEquals(account, detail.accountId)
        assertEquals("", detail.reference)
        assertEquals("", detail.type)
        assertTrue(detail.balances.isEmpty())
        assertTrue(detail.fees.isEmpty())
        assertTrue(detail.interest.isEmpty())
    }

    private fun response(vararg statements: Statement): StatementDetailsResponse =
        StatementDetailsResponse(data = Data(statement = statements.toList()))

    private fun statement(id: String, reference: String): Statement =
        Statement(statementId = id, statementReference = reference)
}
