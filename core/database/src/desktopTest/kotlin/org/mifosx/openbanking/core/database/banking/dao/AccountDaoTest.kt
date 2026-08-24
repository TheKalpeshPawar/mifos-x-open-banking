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
import org.mifosx.openbanking.core.database.banking.entity.AccountEntity
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AccountDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var accountDao: AccountDao

    private fun account(id: String, accountHolderName: String) = AccountEntity(
        accountId = id,
        accountHolderName = accountHolderName,
        accountSubType = "CurrentAccount",
        currency = "GBP",
        sortCode = "400515",
        accountNumber = "12345678",
    )

    @BeforeTest
    fun setup() {
        database = Room.inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        accountDao = database.accountDao
    }

    @AfterTest
    fun teardown() {
        database.close()
    }

    @Test
    fun observeAllFromEmptyDatabaseReturnsEmptyList() = runTest {
        accountDao.observeAll().test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun upsertAllPersistsAndIsObserved() = runTest {
        accountDao.upsertAll(listOf(account("acc-1", "Everyday Current"), account("acc-2", "ISA Saver")))

        accountDao.observeAll().test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals("Everyday Current", result.first { it.accountId == "acc-1" }.accountHolderName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun upsertSameAccountIdReplacesExisting() = runTest {
        accountDao.upsertAll(listOf(account("acc-1", "Original")))
        accountDao.upsertAll(listOf(account("acc-1", "Renamed")))

        accountDao.observeAll().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Renamed", result.first().accountHolderName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun clearRemovesAllRows() = runTest {
        accountDao.upsertAll(listOf(account("acc-1", "Everyday Current")))
        accountDao.clear()

        accountDao.observeAll().test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
