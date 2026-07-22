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

import org.mifosx.openbanking.core.network.model.ais.statementTransactions.Amount
import org.mifosx.openbanking.core.network.model.ais.statementTransactions.Data
import org.mifosx.openbanking.core.network.model.ais.statementTransactions.StatementTransactionsResponse
import org.mifosx.openbanking.core.network.model.ais.statementTransactions.Transaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers [StatementTransactionsResponse.toStatementTransactionItems]: the `TransactionInformation`
 * narrative becomes the row description, the credit/debit sense is derived, and missing fields degrade.
 */
class StatementTransactionMapperTest {

    private val account = "40051512345678"

    @Test
    fun everyTransactionIsMappedWithItsInformationAndCreditDebitSense() {
        val items = StatementTransactionsResponse(
            data = Data(
                transaction = listOf(
                    Transaction(
                        transactionId = "TXN-2026-05-002",
                        accountId = account,
                        creditDebitIndicator = "Credit",
                        bookingDateTime = "2026-05-10T00:00:00Z",
                        transactionInformation = "BACS CREDIT ACME CORP PAYROLL",
                        amount = Amount("3200.00", "GBP"),
                    ),
                    Transaction(
                        transactionId = "TXN-2026-05-001",
                        accountId = account,
                        creditDebitIndicator = "Debit",
                        bookingDateTime = "2026-05-03T09:14:22Z",
                        transactionInformation = "TESCO STORES 3225 LONDON",
                        amount = Amount("82.50", "GBP"),
                    ),
                ),
            ),
        ).toStatementTransactionItems(account)

        assertEquals(2, items.size)
        assertEquals("BACS CREDIT ACME CORP PAYROLL", items.first().description)
        assertTrue(items.first().isCredit)
        assertEquals("3200.00", items.first().amount)
        assertEquals("GBP", items.first().currency)
        assertTrue(!items[1].isCredit)
        assertEquals("2026-05-03T09:14:22Z", items[1].bookingDateTime)
    }

    @Test
    fun anEmptyOrNullTransactionArrayMapsToAnEmptyList() {
        val emptyArray = StatementTransactionsResponse(data = Data(transaction = emptyList()))
        val nullData = StatementTransactionsResponse(data = null)
        assertTrue(emptyArray.toStatementTransactionItems(account).isEmpty())
        assertTrue(nullData.toStatementTransactionItems(account).isEmpty())
    }

    @Test
    fun missingFieldsDegradeAndTheAccountIdFallsBack() {
        val item = StatementTransactionsResponse(
            data = Data(transaction = listOf(Transaction(transactionId = "TX-1"))),
        ).toStatementTransactionItems(account).single()

        assertEquals(account, item.accountId)
        assertEquals("", item.description)
        assertEquals("", item.bookingDateTime)
        assertEquals("0", item.amount)
        assertTrue(!item.isCredit)
    }
}
