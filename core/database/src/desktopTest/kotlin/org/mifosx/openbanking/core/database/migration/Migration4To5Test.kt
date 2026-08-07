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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Drives [MIGRATION_4_5] against a real v4 table.
 *
 * This is the first migration in the database, and it exists because `payment_history` is the only
 * table that cannot be re-fetched — a payment that failed before reaching the bank is recorded
 * nowhere else. So the assertion that matters is not that the columns changed shape but that the
 * rows are still there afterwards, with their values intact.
 */
class Migration4To5Test {

    private lateinit var connection: SQLiteConnection

    /** The v4 table, exactly as 4.json declares it. */
    private val createV4 = """
        CREATE TABLE IF NOT EXISTS `payment_history` (
            `id` TEXT NOT NULL,
            `domesticPaymentId` TEXT,
            `errorKind` TEXT,
            `errorDescription` TEXT,
            `status` TEXT,
            `debtorAccountId` TEXT NOT NULL,
            `debtorName` TEXT NOT NULL,
            `debtorIdentification` TEXT NOT NULL,
            `creditorName` TEXT NOT NULL,
            `creditorIdentification` TEXT NOT NULL,
            `amountMinorUnits` INTEGER NOT NULL,
            `currency` TEXT NOT NULL,
            `reference` TEXT,
            `creationDateTime` TEXT NOT NULL,
            `settlementDateTime` TEXT,
            `paymentType` TEXT NOT NULL,
            `syncedAt` TEXT,
            PRIMARY KEY(`id`)
        )
    """.trimIndent()

    @BeforeTest
    fun setUp() {
        connection = BundledSQLiteDriver().open(":memory:")
        connection.execSQL(createV4)
    }

    @AfterTest
    fun tearDown() {
        connection.close()
    }

    private fun insertV4Row() {
        connection.execSQL(
            """
            INSERT INTO payment_history VALUES (
                '19909', '19909', NULL, NULL, 'AcceptedSettlementCompleted',
                '123456791', 'Current account', '80200110203349',
                'Mr Dharani C', '80200110203350',
                25000, 'GBP', 'Dinner payment',
                '2026-08-07T09:15:00Z', '2026-08-07T10:00:00Z',
                'domestic_payment', NULL
            )
            """.trimIndent(),
        )
    }

    private fun columns(): List<String> = buildList {
        connection.prepare("PRAGMA table_info(payment_history)").use { stmt ->
            while (stmt.step()) add(stmt.getText(1))
        }
    }

    @Test
    fun renamesTheDomesticPaymentIdColumn() = runTest {
        MIGRATION_4_5.migrate(connection)

        val columns = columns()
        assertTrue("paymentId" in columns, "expected paymentId, got $columns")
        assertTrue("domesticPaymentId" !in columns, "domesticPaymentId should be gone, got $columns")
    }

    @Test
    fun addsTheTimelineAndInternationalColumns() = runTest {
        MIGRATION_4_5.migrate(connection)

        val columns = columns()
        listOf("approvedAt", "submittedAt", "chargeBearer", "currencyOfTransfer").forEach {
            assertTrue(it in columns, "expected $it, got $columns")
        }
    }

    /** The whole reason this migration exists rather than another destructive drop. */
    @Test
    fun keepsExistingRowsAndTheirValues() = runTest {
        insertV4Row()

        MIGRATION_4_5.migrate(connection)

        connection.prepare(
            "SELECT paymentId, creditorName, amountMinorUnits, reference FROM payment_history",
        ).use { stmt ->
            assertTrue(stmt.step(), "the migrated table lost its row")
            assertEquals("19909", stmt.getText(0))
            assertEquals("Mr Dharani C", stmt.getText(1))
            assertEquals(25_000L, stmt.getLong(2))
            assertEquals("Dinner payment", stmt.getText(3))
        }
    }

    /**
     * A row written before v5 has no stage timestamps, and must not acquire invented ones — the
     * timeline reads NULL as "not observed", which is the honest answer for a payment that
     * completed before anything was recording.
     */
    @Test
    fun leavesTheNewColumnsNullOnExistingRows() = runTest {
        insertV4Row()

        MIGRATION_4_5.migrate(connection)

        connection.prepare(
            "SELECT approvedAt, submittedAt, chargeBearer, currencyOfTransfer FROM payment_history",
        ).use { stmt ->
            assertTrue(stmt.step())
            for (column in 0..3) {
                assertTrue(stmt.isNull(column), "column $column should be null")
            }
        }
    }

    @Test
    fun migratesAnEmptyTableWithoutError() = runTest {
        MIGRATION_4_5.migrate(connection)

        connection.prepare("SELECT COUNT(*) FROM payment_history").use { stmt ->
            assertTrue(stmt.step())
            assertEquals(0L, stmt.getLong(0))
        }
    }

    @Test
    fun declaresTheVersionsItMovesBetween() {
        assertEquals(4, MIGRATION_4_5.startVersion)
        assertEquals(5, MIGRATION_4_5.endVersion)
        assertNull(ALL_MIGRATIONS.firstOrNull { it.startVersion == 5 })
    }
}
