/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking

import org.mifosx.openbanking.core.model.banking.TransactionItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpendingCalculatorTest {

    private fun tx(
        amount: String,
        isCredit: Boolean,
        date: String,
        description: String = "Merchant",
    ) = TransactionItem(
        transactionId = "t-$amount-$date",
        accountId = "acc-1",
        description = description,
        bookingDateTime = date,
        amount = amount,
        currency = "GBP",
        isCredit = isCredit,
    )

    @Test
    fun sumsOnlyDebitsInTheCurrentMonth() {
        val transactions = listOf(
            tx("31.99", isCredit = false, date = "2026-06-27T10:00:00Z"),
            tx("8.45", isCredit = false, date = "2026-06-26T10:00:00Z"),
            tx("2400.00", isCredit = true, date = "2026-06-25T10:00:00Z"),
            tx("100.00", isCredit = false, date = "2026-05-30T10:00:00Z"),
        )

        val snapshot = computeSpendingSnapshot(transactions, "2026-06")

        assertEquals(4044, snapshot.totalMinorUnits)
        assertEquals("GBP", snapshot.currency)
        assertTrue(snapshot.hasData)
    }

    @Test
    fun reportsTopCategoryByHighestSpend() {
        val transactions = listOf(
            tx("10.00", isCredit = false, date = "2026-06-01T10:00:00Z", description = "Coffee"),
            tx("50.00", isCredit = false, date = "2026-06-02T10:00:00Z", description = "Groceries"),
            tx("5.00", isCredit = false, date = "2026-06-03T10:00:00Z", description = "Groceries"),
        )

        val snapshot = computeSpendingSnapshot(transactions, "2026-06")

        assertEquals("Groceries", snapshot.topCategory)
    }

    @Test
    fun emptyWhenNoDebitsThisMonth() {
        val transactions = listOf(
            tx("2400.00", isCredit = true, date = "2026-06-25T10:00:00Z"),
            tx("100.00", isCredit = false, date = "2026-05-30T10:00:00Z"),
        )

        val snapshot = computeSpendingSnapshot(transactions, "2026-06")

        assertEquals(0, snapshot.totalMinorUnits)
        assertFalse(snapshot.hasData)
        assertEquals("", snapshot.topCategory)
    }

    @Test
    fun skipsMalformedAmounts() {
        val transactions = listOf(
            tx("abc", isCredit = false, date = "2026-06-10T10:00:00Z"),
            tx("12.00", isCredit = false, date = "2026-06-11T10:00:00Z"),
        )

        val snapshot = computeSpendingSnapshot(transactions, "2026-06")

        assertEquals(1200, snapshot.totalMinorUnits)
        assertEquals("GBP", snapshot.currency)
        assertTrue(snapshot.hasData)
    }

    @Test
    fun usesMagnitudeForNegativeDebitAmounts() {
        val transactions = listOf(tx("-31.99", isCredit = false, date = "2026-06-27T10:00:00Z"))

        val snapshot = computeSpendingSnapshot(transactions, "2026-06")

        assertEquals(3199, snapshot.totalMinorUnits)
    }

    @Test
    fun blankDescriptionBucketsAsOther() {
        val transactions = listOf(
            tx("10.00", isCredit = false, date = "2026-06-01T10:00:00Z", description = ""),
        )

        val snapshot = computeSpendingSnapshot(transactions, "2026-06")

        assertEquals("Other", snapshot.topCategory)
    }

    @Test
    fun emptyTransactionListYieldsEmptyCurrency() {
        val snapshot = computeSpendingSnapshot(emptyList(), "2026-06")

        assertEquals(0, snapshot.totalMinorUnits)
        assertEquals("", snapshot.currency)
        assertFalse(snapshot.hasData)
    }
}
