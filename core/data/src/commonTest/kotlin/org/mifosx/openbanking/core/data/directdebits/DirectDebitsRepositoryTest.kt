/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.directdebits

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.mifosx.openbanking.core.data.testutil.FakeObpCacheDao
import org.mifosx.openbanking.core.data.testutil.testJson
import org.mifosx.openbanking.core.data.transactions.TransactionsRepository
import org.mifosx.openbanking.core.model.obp.AmountOfMoney
import org.mifosx.openbanking.core.model.obp.CounterpartyHolder
import org.mifosx.openbanking.core.model.obp.DirectDebitMandate
import org.mifosx.openbanking.core.model.obp.Transaction
import org.mifosx.openbanking.core.model.obp.TransactionAttribute
import org.mifosx.openbanking.core.model.obp.TransactionCounterparty
import org.mifosx.openbanking.core.model.obp.TransactionDetails
import template.core.base.store.screen.ScreenDataStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class DirectDebitsRepositoryTest {

    private val today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

    private fun txn(
        description: String,
        amount: String,
        completed: String,
        typeCode: String = "DD",
    ) = Transaction(
        transactionId = "$description-$completed",
        otherAccount = TransactionCounterparty(holder = CounterpartyHolder(name = "afternooncoffee")),
        details = TransactionDetails(
            description = description,
            completed = "${completed}T09:00:00Z",
            value = AmountOfMoney(currency = "EUR", amount = amount),
        ),
        transactionAttributes = listOf(TransactionAttribute("TXN_TYPE", "STRING", typeCode)),
    )

    private fun recentDate(daysAgo: Int): String = today.let {
        LocalDate.fromEpochDays(it.toEpochDays() - daysAgo).toString()
    }

    private val seededTransactions = listOf(
        txn("Netflix Subscription", "-15.99", recentDate(3)),
        txn("EE Mobile — airtime", "-25.00", recentDate(10)),
        txn("British Gas — energy", "-68.90", recentDate(18)),
        txn("Old Paper — print", "-9.99", recentDate(200)),
        txn("Card payment", "-42.00", recentDate(2), typeCode = "POS"),
        txn("DD refund", "15.99", recentDate(2)),
    )

    private class DdFakeTransactionsRepository(
        private val transactions: List<Transaction>,
    ) : TransactionsRepository {
        override fun transactionsStream(
            bankId: String,
            accountId: String,
            scope: CoroutineScope,
        ): ScreenDataStream<List<Transaction>> = TODO("not used")

        override suspend fun listTransactions(bankId: String, accountId: String, limit: Int?) =
            Result.success(transactions)

        override suspend fun listTransactionsWithAttributes(bankId: String, accountId: String, limit: Int?) =
            Result.success(transactions)

        override suspend fun getTransaction(bankId: String, accountId: String, transactionId: String) =
            TODO("not used")
    }

    private fun repository(
        transactions: List<Transaction> = seededTransactions,
        dao: FakeObpCacheDao = FakeObpCacheDao(),
    ) = DirectDebitsRepositoryImpl(
        transactionsRepository = DdFakeTransactionsRepository(transactions),
        dao = dao,
        json = testJson(),
    )

    @Test
    fun derivesMandatesFromDdDebitsOnly() = runTest {
        val mandates = repository().listMandates("ac.bank.uk", "acc-1").getOrThrow()
        assertEquals(4, mandates.size)
        assertTrue(mandates.none { it.merchantName == "Card payment" })
        assertTrue(mandates.none { it.merchantName == "DD refund" })
    }

    @Test
    fun merchantNameComesFromDescriptionNeverHolder() = runTest {
        val mandates = repository().listMandates("ac.bank.uk", "acc-1").getOrThrow()
        val merchants = mandates.map { it.merchantName }
        assertTrue("British Gas" in merchants)
        assertTrue("EE Mobile" in merchants)
        assertTrue("Netflix Subscription" in merchants)
        assertTrue(merchants.none { it == "afternooncoffee" })
    }

    @Test
    fun singleCollectionDefaultsToMonthlyWithNextDate() = runTest {
        val netflix = repository().listMandates("ac.bank.uk", "acc-1").getOrThrow()
            .first { it.merchantName == "Netflix Subscription" }
        assertEquals("MONTHLY", netflix.frequency)
        assertEquals(recentDate(3), netflix.lastCollectionDate)
        assertTrue(netflix.nextCollectionDate > netflix.lastCollectionDate)
        assertEquals("15.99", netflix.amountValue)
        assertTrue(netflix.isActive)
    }

    @Test
    fun silentSeriesRendersCancelled() = runTest {
        val old = repository().listMandates("ac.bank.uk", "acc-1").getOrThrow()
            .first { it.merchantName == "Old Paper" }
        assertEquals(DirectDebitMandate.STATUS_CANCELLED, old.status)
    }

    @Test
    fun mandateReferenceDerivedFromMerchantAndFirstCollection() = runTest {
        val gas = repository().listMandates("ac.bank.uk", "acc-1").getOrThrow()
            .first { it.merchantName == "British Gas" }
        assertTrue(gas.mandateReference.startsWith("DD-BG-"))
        assertEquals("DD-BG-${recentDate(18).replace("-", "")}", gas.mandateReference)
    }

    @Test
    fun firstCollectionDateAndRecentHistoryDerived() = runTest {
        val gas = repository(
            transactions = listOf(
                txn("British Gas — energy", "-68.90", recentDate(48)),
                txn("British Gas — energy", "-70.10", recentDate(18)),
            ),
        ).listMandates("ac.bank.uk", "acc-1").getOrThrow()
            .first { it.merchantName == "British Gas" }
        assertEquals(recentDate(48), gas.firstCollectionDate)
        assertEquals(2, gas.recentCollections.size)
        assertEquals(recentDate(18), gas.recentCollections.first().date)
        assertEquals("70.10", gas.recentCollections.first().amountValue)
        assertEquals("EUR", gas.recentCollections.first().amountCurrency)
        assertEquals(recentDate(48), gas.recentCollections.last().date)
    }

    @Test
    fun recentCollectionsCappedAtSixMostRecent() = runTest {
        val series = (0 until 8).map { i -> txn("Acme Cloud", "-9.99", recentDate(i * 30)) }
        val acme = repository(transactions = series).listMandates("ac.bank.uk", "acc-1").getOrThrow()
            .first { it.merchantName == "Acme Cloud" }
        assertEquals(6, acme.recentCollections.size)
        assertEquals(recentDate(0), acme.recentCollections.first().date)
        assertEquals(recentDate(150), acme.recentCollections.last().date)
        assertEquals(recentDate(210), acme.firstCollectionDate)
    }

    @Test
    fun cancelPersistsAcrossRepositoryInstances() = runTest {
        val dao = FakeObpCacheDao()
        val first = repository(dao = dao)
        val netflixId = first.listMandates("ac.bank.uk", "acc-1").getOrThrow()
            .first { it.merchantName == "Netflix Subscription" }.id
        first.cancel("acc-1", netflixId).getOrThrow()

        val fresh = repository(dao = dao)
        val netflix = fresh.listMandates("ac.bank.uk", "acc-1").getOrThrow()
            .first { it.merchantName == "Netflix Subscription" }
        assertFalse(netflix.isActive)
        assertEquals("", netflix.nextCollectionDate)
    }

    @Test
    fun activeMandatesSortBeforeCancelled() = runTest {
        val mandates = repository().listMandates("ac.bank.uk", "acc-1").getOrThrow()
        val firstCancelledIndex = mandates.indexOfFirst { !it.isActive }
        assertTrue(mandates.take(firstCancelledIndex).all { it.isActive })
    }
}
