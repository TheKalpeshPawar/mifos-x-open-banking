/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.mifosx.openbanking.core.data.banking.impl.PaymentHistoryRepositoryImpl
import org.mifosx.openbanking.core.data.callback.SettingsPaymentAuthSession
import org.mifosx.openbanking.core.database.banking.dao.PaymentHistoryDao
import org.mifosx.openbanking.core.database.banking.entity.PaymentHistoryEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The six party columns as the status screen reads them back.
 *
 * Every value below is distinct, because the failure this guards against is a transposition — six
 * fields of the same type copied in a row, where swapping two compiles and reads plausibly.
 */
class PaymentHistoryPartiesTest {

    private val storedRow = PaymentHistoryEntity(
        id = "19901",
        paymentId = "19901",
        errorKind = null,
        errorDescription = null,
        status = "AcceptedSettlementInProcess",
        debtorAccountId = "acc-1",
        debtorName = "debtor-name",
        debtorIdentification = "debtor-identification",
        debtorScheme = "debtor-scheme",
        creditorName = "creditor-name",
        creditorIdentification = "creditor-identification",
        creditorScheme = "creditor-scheme",
        amountMinorUnits = 2_50,
        currency = "GBP",
        reference = null,
        creationDateTime = "2026-08-15T18:00:00Z",
        settlementDateTime = null,
        paymentType = "domestic_payment",
        syncedAt = null,
    )

    private fun repository(row: PaymentHistoryEntity?) = PaymentHistoryRepositoryImpl(
        dao = FakeDao(row),
        paymentAuthSession = SettingsPaymentAuthSession(MapSettings()),
    )

    @Test
    fun everyPartyColumnIsReadIntoItsOwnField() = runTest {
        val parties = repository(storedRow).partiesOf("19901")

        assertEquals("debtor-name", parties?.debtorName)
        assertEquals("debtor-identification", parties?.debtorIdentification)
        assertEquals("debtor-scheme", parties?.debtorScheme)
        assertEquals("creditor-name", parties?.creditorName)
        assertEquals("creditor-identification", parties?.creditorIdentification)
        assertEquals("creditor-scheme", parties?.creditorScheme)
    }

    /** No row is the honest case of a payment made on another device, and must not read as blanks. */
    @Test
    fun noStoredRowReadsAsNullRatherThanEmptyParties() = runTest {
        assertNull(repository(row = null).partiesOf("19901"))
    }

    @Test
    fun aRowThatNeverRecordedAPartyReadsItAsBlank() = runTest {
        val row = storedRow.copy(debtorName = "", debtorScheme = "")

        val parties = repository(row).partiesOf("19901")

        assertEquals("", parties?.debtorName)
        assertEquals("", parties?.debtorScheme)
        assertEquals("creditor-name", parties?.creditorName)
    }
}

private class FakeDao(private val row: PaymentHistoryEntity?) : PaymentHistoryDao {

    override fun observeById(paymentId: String): Flow<PaymentHistoryEntity?> = MutableStateFlow(row)

    override fun observeByType(
        types: List<String>,
        limit: Int,
    ): Flow<List<PaymentHistoryEntity>> = MutableStateFlow(emptyList())

    @Suppress("LongParameterList")
    override suspend fun updateFromReceipt(
        paymentId: String,
        status: String,
        settledAt: String?,
        syncedAt: String,
        debtorName: String,
        debtorIdentification: String,
        debtorScheme: String,
        creditorName: String,
        creditorIdentification: String,
        creditorScheme: String,
    ) = Unit

    override suspend fun upsert(entity: PaymentHistoryEntity) = Unit

    override suspend fun clear() = Unit
}
