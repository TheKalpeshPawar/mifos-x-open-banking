/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.standingorders

import kotlinx.datetime.LocalDate
import org.mifosx.openbanking.core.model.obp.AmountOfMoney
import org.mifosx.openbanking.core.model.obp.Transaction
import org.mifosx.openbanking.core.model.obp.TransactionAttribute
import org.mifosx.openbanking.core.model.obp.TransactionDetails
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StandingOrderDerivationTest {

    private val today = LocalDate(2026, 6, 5)

    private fun soTxn(description: String, amount: String, completed: String, type: String = "SO") = Transaction(
        transactionId = "$description-$completed",
        details = TransactionDetails(
            description = description,
            completed = "${completed}T09:00:00Z",
            posted = "${completed}T09:00:00Z",
            value = AmountOfMoney(currency = "EUR", amount = amount),
        ),
        transactionAttributes = listOf(TransactionAttribute(name = "TXN_TYPE", type = "STRING", value = type)),
    )

    @Test
    fun groupsSoTransactionsByDescriptionIntoOneRowPerSeries() {
        val rows = deriveStandingOrders(
            transactions = listOf(
                soTxn("Rent", "-450.00", "2026-04-01"),
                soTxn("Rent", "-450.00", "2026-05-01"),
                soTxn("Rent", "-450.00", "2026-06-01"),
                soTxn("Netflix", "-15.99", "2026-05-07"),
                soTxn("Netflix", "-15.99", "2026-06-04"),
            ),
            today = today,
        )
        assertEquals(2, rows.size)
        assertEquals(setOf("Rent", "Netflix"), rows.map { it.name }.toSet())
    }

    @Test
    fun ignoresNonSoAndIncomingTransactions() {
        val rows = deriveStandingOrders(
            transactions = listOf(
                soTxn("Coffee", "-5.23", "2026-06-01", type = "POS"),
                soTxn("Salary", "5000.00", "2026-06-01"),
                soTxn("Rent", "-450.00", "2026-06-01"),
            ),
            today = today,
        )
        assertEquals(listOf("Rent"), rows.map { it.name })
    }

    @Test
    fun usesLatestAmountAndAbsoluteValue() {
        val rows = deriveStandingOrders(
            transactions = listOf(
                soTxn("Gym", "-40.00", "2026-04-15"),
                soTxn("Gym", "-45.00", "2026-05-15"),
            ),
            today = today,
        )
        assertEquals("45.00", rows.single().amountValue)
        assertEquals("EUR", rows.single().amountCurrency)
    }

    @Test
    fun infersMonthlyFrequencyAndNextDate() {
        val rows = deriveStandingOrders(
            transactions = listOf(
                soTxn("Rent", "-450.00", "2026-04-01"),
                soTxn("Rent", "-450.00", "2026-05-01"),
                soTxn("Rent", "-450.00", "2026-06-01"),
            ),
            today = today,
        )
        val rent = rows.single()
        assertEquals("MONTHLY", rent.frequency)
        assertEquals("2026-06-01", rent.lastPaymentDate)
        assertEquals("2026-07-01", rent.nextPaymentDate)
        assertTrue(rent.isActive)
    }

    @Test
    fun infersWeeklyFrequency() {
        val rows = deriveStandingOrders(
            transactions = listOf(
                soTxn("Cleaner", "-30.00", "2026-05-22"),
                soTxn("Cleaner", "-30.00", "2026-05-29"),
                soTxn("Cleaner", "-30.00", "2026-06-05"),
            ),
            today = today,
        )
        assertEquals("WEEKLY", rows.single().frequency)
        assertEquals("2026-06-12", rows.single().nextPaymentDate)
    }

    @Test
    fun marksSeriesPausedWhenLastPaymentOlderThanGracePeriod() {
        val rows = deriveStandingOrders(
            transactions = listOf(
                soTxn("Gym", "-45.00", "2026-03-15"),
                soTxn("Gym", "-45.00", "2026-04-15"),
            ),
            today = today,
        )
        assertTrue(rows.single().isPaused)
    }

    @Test
    fun marksSeriesCancelledWhenSilentBeyondThreePeriods() {
        val rows = deriveStandingOrders(
            transactions = listOf(
                soTxn("Old loan", "-99.00", "2026-01-10"),
                soTxn("Old loan", "-99.00", "2026-02-09"),
            ),
            today = today,
        )
        assertTrue(rows.single().isCancelled)
    }

    @Test
    fun singleOccurrenceDefaultsToMonthlyAndStaysActiveWithinGrace() {
        val rows = deriveStandingOrders(
            transactions = listOf(soTxn("Insurance", "-89.50", "2026-05-20")),
            today = today,
        )
        val row = rows.single()
        assertEquals("MONTHLY", row.frequency)
        assertEquals("2026-06-20", row.nextPaymentDate)
        assertTrue(row.isActive)
    }

    @Test
    fun rowsSortedActiveFirstThenByNextDate() {
        val rows = deriveStandingOrders(
            transactions = listOf(
                soTxn("Gym", "-45.00", "2026-04-01"),
                soTxn("Netflix", "-15.99", "2026-06-04"),
                soTxn("Rent", "-450.00", "2026-06-01"),
            ),
            today = today,
        )
        assertEquals(listOf("Rent", "Netflix", "Gym"), rows.map { it.name })
    }
}
