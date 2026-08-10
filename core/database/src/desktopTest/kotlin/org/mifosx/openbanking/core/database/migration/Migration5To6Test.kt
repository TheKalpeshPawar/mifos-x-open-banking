/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.database.migration

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Drives [MIGRATION_5_6] against a real v5 table.
 *
 * `accounts` is a re-fetchable cache, so unlike [MIGRATION_4_5] this migration is not protecting
 * data that exists nowhere else. It exists because of what a *missing* description means: HSBC
 * reports a Global Money wallet as `AccountTypeCode: CACC`, identical to a current account, so the
 * free-text description is the only thing telling them apart. An empty description reads as "not a
 * wallet", which is why dropping the table would silently make every cached wallet payable again
 * until the next successful fetch.
 */
class Migration5To6Test {

    private lateinit var connection: SQLiteConnection

    /** The v5 table, exactly as 5.json declares it. */
    private val createV5 = """
        CREATE TABLE IF NOT EXISTS `accounts` (
            `accountId` TEXT NOT NULL,
            `nickname` TEXT NOT NULL,
            `accountSubType` TEXT NOT NULL,
            `currency` TEXT NOT NULL,
            `sortCode` TEXT NOT NULL,
            `accountNumber` TEXT NOT NULL,
            `rawIdentification` TEXT NOT NULL,
            PRIMARY KEY(`accountId`)
        )
    """.trimIndent()

    @BeforeTest
    fun setUp() {
        connection = BundledSQLiteDriver().open(":memory:")
        connection.execSQL(createV5)
    }

    @AfterTest
    fun tearDown() {
        connection.close()
    }

    /** The wallet from the sandbox, which reports `CACC` and is told apart only by its description. */
    private fun insertV5Wallet() {
        connection.execSQL(
            """
            INSERT INTO accounts VALUES (
                '1123456843', '', 'CACC', 'GBP', '801197', '70009652', '80119770009652'
            )
            """.trimIndent(),
        )
    }

    private fun columns(): List<String> = buildList {
        connection.prepare("PRAGMA table_info(accounts)").use { stmt ->
            while (stmt.step()) add(stmt.getText(1))
        }
    }

    @Test
    fun addsTheDescriptionColumn() = runTest {
        assertFalse("description" in columns(), "v5 should not have it yet")

        MIGRATION_5_6.migrate(connection)

        assertTrue("description" in columns(), "expected description, got ${columns()}")
    }

    @Test
    fun keepsExistingRowsAndTheirValues() = runTest {
        insertV5Wallet()

        MIGRATION_5_6.migrate(connection)

        connection.prepare(
            "SELECT accountId, accountSubType, rawIdentification FROM accounts",
        ).use { stmt ->
            assertTrue(stmt.step(), "the migrated table lost its row")
            assertEquals("1123456843", stmt.getText(0))
            assertEquals("CACC", stmt.getText(1))
            assertEquals("80119770009652", stmt.getText(2))
        }
    }

    /**
     * Empty rather than NULL, because the model's field is a non-null `String`.
     *
     * A NULL default would round-trip into a null in a field Kotlin says can never be null, which
     * fails at the mapper rather than here — a long way from the cause.
     */
    @Test
    fun defaultsExistingRowsToAnEmptyDescriptionRatherThanNull() = runTest {
        insertV5Wallet()

        MIGRATION_5_6.migrate(connection)

        connection.prepare("SELECT description FROM accounts").use { stmt ->
            assertTrue(stmt.step())
            assertFalse(stmt.isNull(0), "description must not be null")
            assertEquals("", stmt.getText(0))
        }
    }

    /**
     * A migrated row reads as "not a wallet" until the next fetch fills the description in.
     *
     * Worth stating outright rather than leaving implicit: the migration cannot recover a value the
     * old schema never stored, so correctness here depends on the accounts fetch that follows. What
     * it does buy is that the row survives to *be* refreshed, instead of the table being dropped.
     */
    @Test
    fun migratesAnEmptyTableWithoutError() = runTest {
        MIGRATION_5_6.migrate(connection)

        connection.prepare("SELECT COUNT(*) FROM accounts").use { stmt ->
            assertTrue(stmt.step())
            assertEquals(0L, stmt.getLong(0))
        }
    }

    @Test
    fun declaresTheVersionsItMovesBetween() {
        assertEquals(5, MIGRATION_5_6.startVersion)
        assertEquals(6, MIGRATION_5_6.endVersion)
    }
}
