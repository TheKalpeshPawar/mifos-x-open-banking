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
import kotlin.test.assertTrue

class Migration11To12Test {

    private lateinit var connection: SQLiteConnection

    /** The v11 table, exactly as 11.json declares it. */
    private val createV11 = """
        CREATE TABLE IF NOT EXISTS `payment_history` (
            `id` TEXT NOT NULL, `paymentId` TEXT, `errorKind` TEXT, `errorDescription` TEXT,
            `status` TEXT, `debtorAccountId` TEXT NOT NULL, `debtorName` TEXT NOT NULL,
            `debtorIdentification` TEXT NOT NULL, `creditorName` TEXT NOT NULL,
            `creditorIdentification` TEXT NOT NULL, `amountMinorUnits` INTEGER NOT NULL,
            `currency` TEXT NOT NULL, `reference` TEXT, `creationDateTime` TEXT NOT NULL,
            `approvedAt` TEXT, `submittedAt` TEXT, `settlementDateTime` TEXT, `chargeBearer` TEXT,
            `currencyOfTransfer` TEXT, `requestedExecutionDateTime` TEXT, `frequency` TEXT,
            `finalPaymentDateTime` TEXT, `paymentType` TEXT NOT NULL, `syncedAt` TEXT,
            PRIMARY KEY(`id`)
        )
    """.trimIndent()

    @BeforeTest
    fun setUp() {
        connection = BundledSQLiteDriver().open(":memory:")
        connection.execSQL(createV11)
    }

    @AfterTest
    fun tearDown() {
        connection.close()
    }

    private fun columns(): List<String> = buildList {
        connection.prepare("PRAGMA table_info(payment_history)").use { stmt ->
            while (stmt.step()) add(stmt.getText(1))
        }
    }

    @Test
    fun addsBothSchemeColumns() = runTest {
        MIGRATION_11_12.migrate(connection)

        val cols = columns()
        assertTrue("debtorScheme" in cols, "expected debtorScheme, got $cols")
        assertTrue("creditorScheme" in cols, "expected creditorScheme, got $cols")
    }

    @Test
    fun keepsExistingRowsAndDefaultsTheirSchemesToBlank() = runTest {
        connection.execSQL(
            """
            INSERT INTO payment_history (
                id, paymentId, status, debtorAccountId, debtorName, debtorIdentification,
                creditorName, creditorIdentification, amountMinorUnits, currency,
                creationDateTime, paymentType
            ) VALUES (
                '20123', '20123', 'AcceptedCreditSettlementCompleted', '1123456841', 'Mr Nico',
                '80200110203349', 'Mr Dharani C', '80200110203350', 600, 'GBP',
                '2026-08-25T18:05:03+00:00', 'domestic_payment'
            )
            """.trimIndent(),
        )

        MIGRATION_11_12.migrate(connection)

        connection.prepare(
            "SELECT paymentId, debtorName, debtorScheme, creditorScheme FROM payment_history",
        ).use { stmt ->
            assertTrue(stmt.step(), "the migrated table lost its row")
            assertEquals("20123", stmt.getText(0))
            assertEquals("Mr Nico", stmt.getText(1))
            assertEquals("", stmt.getText(2))
            assertEquals("", stmt.getText(3))
        }
    }

    @Test
    fun migratesAnEmptyTableWithoutError() = runTest {
        MIGRATION_11_12.migrate(connection)

        connection.prepare("SELECT COUNT(*) FROM payment_history").use { stmt ->
            assertTrue(stmt.step())
            assertEquals(0L, stmt.getLong(0))
        }
    }

    @Test
    fun declaresTheVersionsItMovesBetween() {
        assertEquals(11, MIGRATION_11_12.startVersion)
        assertEquals(12, MIGRATION_11_12.endVersion)
    }
}
