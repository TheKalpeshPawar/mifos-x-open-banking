/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.database.banking.dao

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.mifosx.openbanking.core.database.AppDatabase
import org.mifosx.openbanking.core.database.banking.entity.TransactionEntity
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TransactionDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var transactionDao: TransactionDao

    private fun transaction(
        accountId: String,
        txId: String,
        bookingDateTime: String,
    ) = TransactionEntity(
        transactionId = txId,
        accountId = accountId,
        description = "Merchant $txId",
        bookingDateTime = bookingDateTime,
        amount = "10.00",
        currency = "GBP",
        isCredit = false,
    )

    @BeforeTest
    fun setup() {
        database = Room.inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        transactionDao = database.transactionDao
    }

    @AfterTest
    fun teardown() {
        database.close()
    }

    @Test
    fun observeByAccountFromEmptyDatabaseReturnsEmptyList() = runTest {
        transactionDao.observeByAccount("acc-1").test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeByAccountReturnsOnlyMatchingAccountNewestFirst() = runTest {
        transactionDao.upsertAll(
            listOf(
                transaction("acc-1", "t1", "2026-06-25T09:00:00Z"),
                transaction("acc-1", "t2", "2026-06-27T09:00:00Z"),
                transaction("acc-2", "t3", "2026-06-28T09:00:00Z"),
            ),
        )

        transactionDao.observeByAccount("acc-1").test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals("t2", result.first().transactionId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun clearAccountRemovesOnlyThatAccount() = runTest {
        transactionDao.upsertAll(
            listOf(
                transaction("acc-1", "t1", "2026-06-25T09:00:00Z"),
                transaction("acc-2", "t2", "2026-06-26T09:00:00Z"),
            ),
        )

        transactionDao.clearAccount("acc-1")

        transactionDao.observeByAccount("acc-1").test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
        transactionDao.observeByAccount("acc-2").test {
            assertEquals(1, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun blankTransactionIdsDoNotCollide() = runTest {
        transactionDao.upsertAll(
            listOf(
                transaction("acc-1", "", "2026-06-25T09:00:00Z"),
                transaction("acc-1", "", "2026-06-26T09:00:00Z"),
            ),
        )

        transactionDao.observeByAccount("acc-1").test {
            assertEquals(2, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
