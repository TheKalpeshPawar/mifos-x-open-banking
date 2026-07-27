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

import org.mifosx.openbanking.core.model.banking.TransactionCategory
import org.mifosx.openbanking.core.network.model.ais.transactions.Amount
import org.mifosx.openbanking.core.network.model.ais.transactions.BankTransactionCode
import org.mifosx.openbanking.core.network.model.ais.transactions.Links
import org.mifosx.openbanking.core.network.model.ais.transactions.MerchantDetails
import org.mifosx.openbanking.core.network.model.ais.transactions.Meta
import org.mifosx.openbanking.core.network.model.ais.transactions.ProprietaryBankTransactionCode
import org.mifosx.openbanking.core.network.model.ais.transactions.Transaction
import org.mifosx.openbanking.core.network.model.ais.transactions.TransactionsResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.mifosx.openbanking.core.network.model.ais.transactions.Data as TransactionsData

class TransactionPageMapperTest {

    @Test
    fun `deriveCategory maps each MCC bucket`() {
        assertEquals(TransactionCategory.GROCERIES, deriveCategory("5411", null, null))
        assertEquals(TransactionCategory.DINING, deriveCategory("5814", null, null))
        assertEquals(TransactionCategory.SUBSCRIPTIONS, deriveCategory("5815", null, null))
        assertEquals(TransactionCategory.TRANSPORT, deriveCategory("4111", null, null))
        assertEquals(TransactionCategory.TRANSPORT, deriveCategory("4511", null, null))
        assertEquals(TransactionCategory.SHOPPING, deriveCategory("5999", null, null))
    }

    @Test
    fun `deriveCategory trims whitespace around the MCC`() {
        assertEquals(TransactionCategory.GROCERIES, deriveCategory(" 5411 ", null, null))
    }

    @Test
    fun `deriveCategory reads the ISO-20022 bank transaction code when no MCC maps`() {
        assertEquals(TransactionCategory.TRANSFER, deriveCategory(null, "ICDT", "OBP"))
        assertEquals(TransactionCategory.TRANSFER, deriveCategory(null, "RCDT", "OBP"))
        assertEquals(TransactionCategory.TRANSFER, deriveCategory(null, "DMCT", null))
        assertEquals(TransactionCategory.SHOPPING, deriveCategory(null, "CCRD", null))
    }

    @Test
    fun `deriveCategory prefers a mapped MCC over the bank transaction code`() {
        // A grocery purchase paid by card: the MCC is the more specific signal.
        assertEquals(TransactionCategory.GROCERIES, deriveCategory("5411", "CCRD", null))
    }

    @Test
    fun `deriveCategory falls back to TRANSFER on a proprietary transfer hint`() {
        assertEquals(TransactionCategory.TRANSFER, deriveCategory(null, null, "TRF"))
        assertEquals(TransactionCategory.TRANSFER, deriveCategory(null, null, "internal-transfer"))
    }

    @Test
    fun `deriveCategory returns OTHER for unknown MCC and non-numeric and null inputs`() {
        assertEquals(TransactionCategory.OTHER, deriveCategory("9999", null, null))
        assertEquals(TransactionCategory.OTHER, deriveCategory("not-a-number", "OTHR", null))
        assertEquals(TransactionCategory.OTHER, deriveCategory(null, null, null))
    }

    @Test
    fun `deriveCategory never infers a category from direction — credits use MCC not income`() {
        // A refund from a supermarket is still Groceries even though it is a credit.
        assertEquals(TransactionCategory.GROCERIES, deriveCategory("5411", null, null))
    }

    @Test
    fun `isPendingStatus recognises pending codes only`() {
        assertTrue(isPendingStatus("PDNG"))
        assertTrue(isPendingStatus("pending"))
        assertFalse(isPendingStatus("BOOK"))
        assertFalse(isPendingStatus(null))
    }

    @Test
    fun `toTransactionsPage maps rows cursor and total pages`() {
        val response = TransactionsResponse(
            data = TransactionsData(
                transaction = listOf(
                    Transaction(
                        transactionId = "t1",
                        creditDebitIndicator = "Debit",
                        status = "BOOK",
                        bookingDateTime = "2026-06-27T10:00:00Z",
                        amount = Amount("42.17", "GBP"),
                        merchantDetails = MerchantDetails(merchantName = "TESCO", merchantCategoryCode = "5411"),
                        proprietaryBankTransactionCode = ProprietaryBankTransactionCode(code = "OTHR"),
                    ),
                ),
            ),
            links = Links(next = "https://host/x?page=1"),
            meta = Meta(totalPages = 2),
        )

        val page = response.toTransactionsPage("acc-1")

        assertEquals(1, page.items.size)
        val row = page.items.first()
        assertEquals("TESCO", row.description)
        assertEquals("acc-1", row.accountId)
        assertEquals(TransactionCategory.GROCERIES, row.category)
        assertFalse(row.isCredit)
        assertFalse(row.isPending)
        assertEquals("https://host/x?page=1", page.nextLink)
        assertEquals(2, page.totalPages)
        assertTrue(page.hasNextPage)
    }

    @Test
    fun `toTransactionsPage treats a blank Next link as no more pages and defaults missing fields`() {
        val response = TransactionsResponse(
            data = TransactionsData(
                transaction = listOf(Transaction(creditDebitIndicator = "Credit")),
            ),
            links = Links(next = "   "),
        )

        val page = response.toTransactionsPage("fallback-acc")

        val row = page.items.first()
        assertEquals("", row.transactionId)
        assertEquals("fallback-acc", row.accountId)
        assertEquals("", row.description)
        assertEquals("0", row.amount)
        assertEquals("", row.currency)
        assertTrue(row.isCredit)
        assertEquals(TransactionCategory.OTHER, row.category)
        assertNull(page.nextLink)
        assertFalse(page.hasNextPage)
        assertNull(page.totalPages)
    }

    @Test
    fun `the sandbox stub merchant category code no longer classifies every transaction as transport`() {
        // The sandbox returns a constant MerchantCategoryCode inside the same stub MerchantDetails that
        // carries the placeholder name; without suppression every row collapses to one category.
        val response = TransactionsResponse(
            data = TransactionsData(
                transaction = listOf(
                    Transaction(
                        transactionId = "t1",
                        merchantDetails = MerchantDetails(
                            merchantName = "HSBC Sample merchant",
                            merchantCategoryCode = "3000",
                        ),
                    ),
                ),
            ),
        )

        assertEquals(TransactionCategory.OTHER, response.toTransactionsPage("acc-1").items.single().category)
    }

    @Test
    fun `a stub merchant code falls through to the bank transaction code`() {
        val response = TransactionsResponse(
            data = TransactionsData(
                transaction = listOf(
                    Transaction(
                        transactionId = "t1",
                        merchantDetails = MerchantDetails(
                            merchantName = "HSBC Sample merchant",
                            merchantCategoryCode = "3000",
                        ),
                        bankTransactionCode = BankTransactionCode(code = "RCDT"),
                    ),
                ),
            ),
        )

        assertEquals(TransactionCategory.TRANSFER, response.toTransactionsPage("acc-1").items.single().category)
    }

    @Test
    fun `a genuine merchant category code still classifies the transaction`() {
        val response = TransactionsResponse(
            data = TransactionsData(
                transaction = listOf(
                    Transaction(
                        transactionId = "t1",
                        merchantDetails = MerchantDetails(merchantName = "TESCO", merchantCategoryCode = "5411"),
                    ),
                ),
            ),
        )

        assertEquals(TransactionCategory.GROCERIES, response.toTransactionsPage("acc-1").items.single().category)
    }

    @Test
    fun `categoryMerchantCode drops the stub merchant code and keeps a genuine one`() {
        val stub = Transaction(
            merchantDetails = MerchantDetails(merchantName = "HSBC Sample merchant", merchantCategoryCode = "3000"),
        )
        val real = Transaction(
            merchantDetails = MerchantDetails(merchantName = "TESCO", merchantCategoryCode = "5411"),
        )

        assertNull(stub.categoryMerchantCode())
        assertEquals("5411", real.categoryMerchantCode())
    }

    @Test
    fun `toTransactionsPage treats the sandbox placeholder merchant as absent and uses the narrative`() {
        val response = TransactionsResponse(
            data = TransactionsData(
                transaction = listOf(
                    Transaction(
                        transactionId = "t1",
                        transactionInformation = "DIRECT DEBIT BRITISH GAS",
                        merchantDetails = MerchantDetails(merchantName = "HSBC Sample merchant"),
                    ),
                ),
            ),
        )

        val row = response.toTransactionsPage("acc-1").items.single()

        assertEquals("DIRECT DEBIT BRITISH GAS", row.description)
    }

    @Test
    fun `toTransactionsPage on an empty payload yields an empty page`() {
        val page = TransactionsResponse().toTransactionsPage("acc-1")
        assertTrue(page.items.isEmpty())
        assertNull(page.nextLink)
    }
}
