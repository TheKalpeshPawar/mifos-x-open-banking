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
 * Drives [MIGRATION_10_11] against a real v10 table.
 *
 * `sortCode` and `accountNumber` were only ever recombined for display, and `accountSubType` was never
 * populated by HSBC — the values that exist are the 14-digit identification and the `accountTypeCode`.
 * This migration recreates the table so the dead columns are dropped and the surviving values move
 * across, defaulting the scheme to `Other` until the next accounts fetch resolves it.
 */
class Migration10To11Test {

    private lateinit var connection: SQLiteConnection

    /** The v10 table, exactly as 10.json declares it. */
    private val createV10 = """
        CREATE TABLE IF NOT EXISTS `accounts` (
            `accountId` TEXT NOT NULL,
            `accountHolderName` TEXT NOT NULL,
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
        connection.execSQL(createV10)
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
    fun dropsTheSplitColumnsAndAddsTheCollapsedOnes() = runTest {
        MIGRATION_10_11.migrate(connection)

        val cols = columns()
        assertFalse("sortCode" in cols, "expected sortCode to be gone, got $cols")
        assertFalse("accountNumber" in cols, "expected accountNumber to be gone, got $cols")
        assertFalse("accountSubType" in cols, "expected accountSubType to be gone, got $cols")
        assertFalse("rawIdentification" in cols, "expected rawIdentification to be gone, got $cols")
        assertTrue("accountTypeCode" in cols, "expected accountTypeCode, got $cols")
        assertTrue("identification" in cols, "expected identification, got $cols")
        assertTrue("scheme" in cols, "expected scheme, got $cols")
    }

    @Test
    fun keepsExistingRowsAndCarriesTheTypeCodeAndIdentificationAcross() = runTest {
        connection.execSQL(
            """
            INSERT INTO accounts VALUES (
                '1123456843', 'Mr Robert', 'CACC', 'GBP', '801197', '70009652', '80119770009652', ''
            )
            """.trimIndent(),
        )

        MIGRATION_10_11.migrate(connection)

        connection.prepare(
            "SELECT accountId, accountTypeCode, identification, scheme, accountHolderName FROM accounts",
        ).use { stmt ->
            assertTrue(stmt.step(), "the migrated table lost its row")
            assertEquals("1123456843", stmt.getText(0))
            assertEquals("CACC", stmt.getText(1))
            assertEquals("80119770009652", stmt.getText(2))
            assertEquals("Other", stmt.getText(3))
            assertEquals("Mr Robert", stmt.getText(4))
        }
    }

    @Test
    fun migratesAnEmptyTableWithoutError() = runTest {
        MIGRATION_10_11.migrate(connection)

        connection.prepare("SELECT COUNT(*) FROM accounts").use { stmt ->
            assertTrue(stmt.step())
            assertEquals(0L, stmt.getLong(0))
        }
    }

    @Test
    fun declaresTheVersionsItMovesBetween() {
        assertEquals(10, MIGRATION_10_11.startVersion)
        assertEquals(11, MIGRATION_10_11.endVersion)
    }
}
