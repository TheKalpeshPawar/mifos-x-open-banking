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
import org.mifosx.openbanking.core.network.model.ais.transactions.Balance
import org.mifosx.openbanking.core.network.model.ais.transactions.Data
import org.mifosx.openbanking.core.network.model.ais.transactions.MerchantDetails
import org.mifosx.openbanking.core.network.model.ais.transactions.ProprietaryBankTransactionCode
import org.mifosx.openbanking.core.network.model.ais.transactions.Transaction
import org.mifosx.openbanking.core.network.model.ais.transactions.TransactionsResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val ACCOUNT_ID = "40051512345678"

/**
 * Covers [TransactionsResponse.toTransactionDetails]: the full field projection, the credit/debit
 * indicator, the blank-MCC-and-merchant normalisation to null, the client-side category derivation and
 * the empty-payload short-circuit.
 */
class TransactionDetailMapperTest {

    @Test
    fun everyFieldOfARichTransactionIsProjected() {
        val detail = response(
            Transaction(
                transactionId = "TX-1",
                accountId = ACCOUNT_ID,
                creditDebitIndicator = "Debit",
                status = "Booked",
                bookingDateTime = "2026-06-26T11:22:00Z",
                valueDateTime = "2026-06-26T11:23:00Z",
                transactionInformation = "TESCO STORES 3476 LONDON",
                amount = Amount("42.17", "GBP"),
                balance = Balance(creditDebitIndicator = "Credit", amount = Amount("447.63", "GBP")),
                merchantDetails = MerchantDetails(merchantName = "Tesco Stores", merchantCategoryCode = "5411"),
                proprietaryBankTransactionCode = ProprietaryBankTransactionCode(code = "DR", issuer = "HSBC"),
            ),
        ).single()

        assertEquals("TX-1", detail.transactionId)
        assertEquals(ACCOUNT_ID, detail.accountId)
        assertEquals("42.17", detail.amount)
        assertEquals("GBP", detail.currency)
        assertFalse(detail.isCredit)
        assertEquals("Tesco Stores", detail.merchantName)
        assertEquals("Booked", detail.status)
        assertEquals("2026-06-26T11:22:00Z", detail.bookingDateTime)
        assertEquals("2026-06-26T11:23:00Z", detail.valueDateTime)
        assertEquals(TransactionCategory.GROCERIES, detail.category)
        assertEquals("5411", detail.merchantCategoryCode)
        assertEquals("447.63", detail.balanceAmount)
        assertEquals("GBP", detail.balanceCurrency)
        assertTrue(detail.balanceIsCredit)
        assertEquals("TESCO STORES 3476 LONDON", detail.transactionInformation)
        assertEquals("DR", detail.proprietaryCode)
        assertEquals("HSBC", detail.proprietaryIssuer)
    }

    @Test
    fun theCreditDebitIndicatorDrivesIsCreditCaseInsensitively() {
        val credit = response(Transaction(transactionId = "c", creditDebitIndicator = "Credit")).single()
        val debit = response(Transaction(transactionId = "d", creditDebitIndicator = "DEBIT")).single()
        val missing = response(Transaction(transactionId = "m", creditDebitIndicator = null)).single()

        assertTrue(credit.isCredit)
        assertFalse(debit.isCredit)
        assertFalse(missing.isCredit)
    }

    @Test
    fun aBlankMerchantAndMccNormaliseToNull() {
        val detail = response(
            Transaction(
                transactionId = "TX-2",
                merchantDetails = MerchantDetails(merchantName = "   ", merchantCategoryCode = "  "),
            ),
        ).single()

        assertNull(detail.merchantName)
        assertNull(detail.merchantCategoryCode)
    }

    @Test
    fun theSandboxPlaceholderMerchantNormalisesToNullSoTheScreenUsesTheNarrative() {
        val detail = response(
            Transaction(
                transactionId = "TX-7",
                transactionInformation = "SALARY ACME LTD",
                merchantDetails = MerchantDetails(merchantName = "HSBC Sample merchant"),
            ),
        ).single()

        assertNull(detail.merchantName)
        assertEquals("SALARY ACME LTD", detail.transactionInformation)
    }

    @Test
    fun theCategoryIsDerivedFromTheMccThenTheProprietaryCode() {
        val dining = response(
            Transaction(
                transactionId = "TX-3",
                merchantDetails = MerchantDetails(merchantCategoryCode = "5812"),
            ),
        ).single()
        val transfer = response(
            Transaction(
                transactionId = "TX-4",
                proprietaryBankTransactionCode = ProprietaryBankTransactionCode(code = "TRF"),
            ),
        ).single()
        val other = response(
            Transaction(
                transactionId = "TX-5",
                proprietaryBankTransactionCode = ProprietaryBankTransactionCode(code = "CR"),
            ),
        ).single()

        assertEquals(TransactionCategory.DINING, dining.category)
        assertEquals(TransactionCategory.TRANSFER, transfer.category)
        assertEquals(TransactionCategory.OTHER, other.category)
    }

    @Test
    fun theAccountIdFallsBackToTheArgumentWhenAbsentFromThePayload() {
        val detail = response(Transaction(transactionId = "TX-6", accountId = null)).single()

        assertEquals(ACCOUNT_ID, detail.accountId)
    }

    @Test
    fun anEmptyPayloadMapsToAnEmptyList() {
        assertTrue(
            TransactionsResponse(data = Data(transaction = emptyList())).toTransactionDetails(ACCOUNT_ID).isEmpty(),
        )
        assertTrue(TransactionsResponse(data = null).toTransactionDetails(ACCOUNT_ID).isEmpty())
    }

    private fun response(vararg transactions: Transaction) =
        TransactionsResponse(data = Data(transaction = transactions.toList())).toTransactionDetails(ACCOUNT_ID)
}
