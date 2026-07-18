/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.pfm

import kotlinx.datetime.LocalDate
import org.mifosx.openbanking.core.model.obp.AmountOfMoney
import org.mifosx.openbanking.core.model.obp.CounterpartyHolder
import org.mifosx.openbanking.core.model.obp.Transaction
import org.mifosx.openbanking.core.model.obp.TransactionAttribute
import org.mifosx.openbanking.core.model.obp.TransactionCounterparty
import org.mifosx.openbanking.core.model.obp.TransactionDetails
import org.mifosx.openbanking.core.model.pfm.BUSINESS_TAXONOMY
import org.mifosx.openbanking.core.model.pfm.PfmPeriod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val TODAY = LocalDate(2026, 6, 6)

private fun txn(
    amount: String,
    date: String = "2026-06-03",
    description: String = "Payment",
    holder: String = "Acme Ltd",
    typeCode: String? = "POS",
    currency: String = "EUR",
) = Transaction(
    id = "$holder-$date-$amount",
    otherAccount = TransactionCounterparty(holder = CounterpartyHolder(name = holder)),
    details = TransactionDetails(
        description = description,
        completed = "${date}T10:00:00Z",
        value = AmountOfMoney(currency = currency, amount = amount),
    ),
    transactionAttributes = typeCode?.let { listOf(TransactionAttribute("TXN_TYPE", "STRING", it)) }.orEmpty(),
)

class PfmAnalysisCoreTest {

    @Test
    fun periodRange_thisMonth() {
        val (start, end) = periodRange(PfmPeriod.THIS_MONTH, TODAY)
        assertEquals(LocalDate(2026, 6, 1), start)
        assertEquals(TODAY, end)
    }

    @Test
    fun periodRange_lastMonth() {
        val (start, end) = periodRange(PfmPeriod.LAST_MONTH, TODAY)
        assertEquals(LocalDate(2026, 5, 1), start)
        assertEquals(LocalDate(2026, 5, 31), end)
    }

    @Test
    fun periodRange_lastThreeMonths() {
        val (start, end) = periodRange(PfmPeriod.LAST_3_MONTHS, TODAY)
        assertEquals(LocalDate(2026, 4, 1), start)
        assertEquals(TODAY, end)
    }

    @Test
    fun categorize_keywordsBeatTxnType() {
        assertEquals("food-dining", categorize(txn("-10.00", description = "Tesco groceries")).id)
        assertEquals("transport", categorize(txn("-5.00", holder = "Transport for London")).id)
        assertEquals("entertainment", categorize(txn("-9.99", holder = "Netflix")).id)
        assertEquals("bills", categorize(txn("-50.00", description = "Rent deposit", typeCode = "POS")).id)
    }

    @Test
    fun categorize_txnTypeFallbacks() {
        assertEquals("bills", categorize(txn("-30.00", typeCode = "DD")).id)
        assertEquals("bills", categorize(txn("-30.00", typeCode = "SO")).id)
        assertEquals("cash", categorize(txn("-20.00", typeCode = "ATM")).id)
        assertEquals("transfers", categorize(txn("-20.00", typeCode = "TFR")).id)
        assertEquals("shopping", categorize(txn("-20.00", typeCode = "POS")).id)
        assertEquals("other", categorize(txn("-20.00", typeCode = null)).id)
    }

    @Test
    fun categorize_businessTaxonomyKeywords() {
        fun bizCategory(description: String, holder: String = "Acme Ltd", typeCode: String = "SANDBOX_TAN"): String {
            val transaction = txn("-100.00", description = description, holder = holder, typeCode = typeCode)
            return categorize(transaction, BUSINESS_TAXONOMY).id
        }

        assertEquals("payroll-contractors", bizCategory("Payroll April"))
        assertEquals("tax", bizCategory("VAT payment Q1"))
        assertEquals("rent-facilities", bizCategory("Office rent April"))
        assertEquals("software-subscriptions", bizCategory("SaaS subscriptions"))
        assertEquals("other", bizCategory("Card payment", holder = "x", typeCode = "COUNTERPARTY"))
    }

    @Test
    fun analyze_summarySpentReceivedNet() {
        val insights = analyze(
            transactions = listOf(
                txn("-100.00"),
                txn("-50.00"),
                txn("3200.00", typeCode = "SAL"),
            ),
            start = LocalDate(2026, 6, 1),
            end = TODAY,
        )
        assertEquals(150.0, insights.summary.spent)
        assertEquals(3200.0, insights.summary.received)
        assertEquals(3050.0, insights.summary.net)
        assertEquals("EUR", insights.currency)
    }

    @Test
    fun analyze_filtersOutsidePeriod() {
        val insights = analyze(
            transactions = listOf(
                txn("-100.00", date = "2026-06-03"),
                txn("-999.00", date = "2026-04-30"),
            ),
            start = LocalDate(2026, 6, 1),
            end = TODAY,
        )
        assertEquals(100.0, insights.summary.spent)
    }

    @Test
    fun analyze_categoryBreakdownSortedWithPercents() {
        val insights = analyze(
            transactions = listOf(
                txn("-75.00", description = "Tesco groceries"),
                txn("-25.00", holder = "Netflix"),
            ),
            start = LocalDate(2026, 6, 1),
            end = TODAY,
        )
        assertEquals(listOf("food-dining", "entertainment"), insights.categories.map { it.id })
        assertEquals(75.0, insights.categories[0].amount)
        assertEquals(75, insights.categories[0].percent)
        assertEquals(25, insights.categories[1].percent)
    }

    @Test
    fun analyze_topMerchantsGroupedAndCapped() {
        val transactions = (1..6).map { index ->
            txn("-10.00", holder = "Merchant $index")
        } + txn("-30.00", holder = "Merchant 1") + txn("500.00", holder = "Employer", typeCode = "SAL")
        val insights = analyze(transactions, LocalDate(2026, 6, 1), TODAY)
        assertEquals(5, insights.topMerchants.size)
        assertEquals("Merchant 1", insights.topMerchants.first().name)
        assertEquals(40.0, insights.topMerchants.first().amount)
        assertEquals(2, insights.topMerchants.first().count)
        assertTrue(insights.topMerchants.none { it.name == "Employer" })
    }

    @Test
    fun analyze_amountOfHookConvertsCurrency() {
        val rate = 1.16278
        val insights = analyze(
            transactions = listOf(
                txn("-100.00", currency = "GBP"),
                txn("-50.00", currency = "EUR"),
            ),
            start = LocalDate(2026, 6, 1),
            end = TODAY,
            amountOf = { txn ->
                val native = txn.details.value.amount.toDoubleOrNull() ?: 0.0
                if (txn.details.value.currency == "GBP") native * rate else native
            },
        )
        assertEquals(100.0 * rate + 50.0, insights.summary.spent, absoluteTolerance = 0.001)
    }

    @Test
    fun analyze_displayNameHookKeepsPlaceholderOutOfMerchants() {
        val placeholder = "afternooncoffee"
        val transactions = listOf(
            txn("-40.00", holder = placeholder),
            txn("-15.00", holder = "Tesco"),
        )
        val insights = analyze(
            transactions = transactions,
            start = LocalDate(2026, 6, 1),
            end = TODAY,
            displayName = { txn ->
                if (txn.otherAccount.holder.name == placeholder) "Jane Doe" else txn.otherAccount.holder.name
            },
        )
        assertTrue(insights.topMerchants.none { it.name == placeholder })
        assertEquals("Jane Doe", insights.topMerchants.first().name)
    }

    @Test
    fun categorize_usesResolvedCounterpartyNameForKeywords() {
        val txn = txn("-9.99", description = "Card payment", holder = "afternooncoffee", typeCode = "POS")
        assertEquals("shopping", categorize(txn).id)
        assertEquals("entertainment", categorize(txn, counterpartyName = "Netflix").id)
    }
}
