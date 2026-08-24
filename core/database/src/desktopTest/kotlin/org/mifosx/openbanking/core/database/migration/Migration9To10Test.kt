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
 * Drives [MIGRATION_9_10] against a real v9 table.
 *
 * The v9 `nickname` column was always blank in practice — HSBC never returns a `Nickname`/top-level
 * `Name` — while the account holder's name (`Account[].Name`) is carried separately. This migration
 * renames the column in place so the holder name becomes the persisted name field without dropping
 * any rows.
 */
class Migration9To10Test {

    private lateinit var connection: SQLiteConnection

    /** The v9 table, exactly as 9.json declares it. */
    private val createV9 = """
        CREATE TABLE IF NOT EXISTS `accounts` (
            `accountId` TEXT NOT NULL,
            `nickname` TEXT NOT NULL,
            `accountSubType` TEXT NOT NULL,
            `currency` TEXT NOT NULL,
            `sortCode` TEXT NOT NULL,
            `accountNumber` TEXT NOT NULL,
            `rawIdentification` TEXT NOT NULL,
            `description` TEXT NOT NULL DEFAULT '',
            PRIMARY KEY(`accountId`)
        )
    """.trimIndent()

    @BeforeTest
    fun setUp() {
        connection = BundledSQLiteDriver().open(":memory:")
        connection.execSQL(createV9)
    }

    @AfterTest
    fun tearDown() {
        connection.close()
    }

    private fun columns(): List<String> = buildList {
        connection.prepare("PRAGMA table_info(accounts)").use { stmt ->
            while (stmt.step()) add(stmt.getText(1))
        }
    }

    @Test
    fun renamesNicknameToAccountHolderName() = runTest {
        assertTrue("nickname" in columns(), "v9 should have the nickname column")
        assertFalse("accountHolderName" in columns(), "v9 should not have the new column yet")

        MIGRATION_9_10.migrate(connection)

        assertFalse("nickname" in columns(), "expected nickname to be gone, got ${columns()}")
        assertTrue("accountHolderName" in columns(), "expected accountHolderName, got ${columns()}")
    }

    @Test
    fun keepsExistingRowsAndTheirNameValue() = runTest {
        connection.execSQL(
            """
            INSERT INTO accounts VALUES (
                '1123456843', 'Mr Robert', 'CACC', 'GBP', '801197', '70009652', '80119770009652', ''
            )
            """.trimIndent(),
        )

        MIGRATION_9_10.migrate(connection)

        connection.prepare("SELECT accountId, accountHolderName FROM accounts").use { stmt ->
            assertTrue(stmt.step(), "the migrated table lost its row")
            assertEquals("1123456843", stmt.getText(0))
            assertEquals("Mr Robert", stmt.getText(1))
        }
    }

    @Test
    fun migratesAnEmptyTableWithoutError() = runTest {
        MIGRATION_9_10.migrate(connection)

        connection.prepare("SELECT COUNT(*) FROM accounts").use { stmt ->
            assertTrue(stmt.step())
            assertEquals(0L, stmt.getLong(0))
        }
    }

    @Test
    fun declaresTheVersionsItMovesBetween() {
        assertEquals(9, MIGRATION_9_10.startVersion)
        assertEquals(10, MIGRATION_9_10.endVersion)
    }
}
