/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking.mapper

import org.mifosx.openbanking.core.model.banking.BankAccount
import org.mifosx.openbanking.core.model.banking.BeneficiaryScheme
import org.mifosx.openbanking.core.model.banking.payment.CreditorSelection
import org.mifosx.openbanking.core.model.banking.payment.PaymentDraft
import org.mifosx.openbanking.core.model.banking.payment.PaymentReceipt
import org.mifosx.openbanking.core.model.banking.payment.PaymentStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PaymentHistoryMapperTest {

    private fun draft(
        amountMinorUnits: Long = 10_000L,
        reference: String? = "Dinner payment",
    ) = PaymentDraft(
        debtorAccount = BankAccount(
            accountId = "123456791",
            nickname = "",
            accountSubType = "CurrentAccount",
            currency = "GBP",
            sortCode = "802001",
            accountNumber = "10203349",
            rawIdentification = "80200110203349",
        ),
        creditor = CreditorSelection(
            name = "Mr Dharani C",
            scheme = BeneficiaryScheme.SortCode,
            identification = "80200110203350",
            beneficiaryId = "BEN-001",
            isOwnAccount = false,
        ),
        amountMinorUnits = amountMinorUnits,
        currency = "GBP",
        reference = reference,
        instructionIdentification = "MFXa1b2c3d4",
        endToEndIdentification = "E2Ea1b2c3d4",
        consentIdempotencyKey = "key-consent-a1b2",
        paymentIdempotencyKey = "key-payment-a1b2",
    )

    private fun receipt(status: PaymentStatus = PaymentStatus.AcceptedSettlementCompleted) =
        PaymentReceipt(
            domesticPaymentId = "19901",
            consentId = "812774903",
            status = status,
            creationDateTime = "2026-08-07T15:30:00Z",
            statusUpdateDateTime = "2026-08-07T15:30:05Z",
            amountLabel = "£100.00",
            creditorName = "Mr Dharani C",
            settlementDateTime = "2026-08-07T16:30:00Z",
            reference = "Dinner payment",
            debtorIdentification = "80200110203349",
        )

    @Test
    fun mapsSubmittedReceiptToEntityWithAllFields() {
        val entity = receipt().toEntity(draft())

        assertEquals("19901", entity.id)
        assertEquals("19901", entity.domesticPaymentId)
        assertNull(entity.errorKind)
        assertNull(entity.errorDescription)
        assertEquals("AcceptedSettlementCompleted", entity.status)
        assertEquals("123456791", entity.debtorAccountId)
        assertEquals("CurrentAccount", entity.debtorName)
        assertEquals("80200110203349", entity.debtorIdentification)
        assertEquals("Mr Dharani C", entity.creditorName)
        assertEquals("80200110203350", entity.creditorIdentification)
        assertEquals(10_000L, entity.amountMinorUnits)
        assertEquals("GBP", entity.currency)
        assertEquals("Dinner payment", entity.reference)
        assertEquals("2026-08-07T15:30:00Z", entity.creationDateTime)
        assertEquals("2026-08-07T16:30:00Z", entity.settlementDateTime)
        assertEquals("domestic_payment", entity.paymentType)
    }

    @Test
    fun mapsPreSubmissionFailureToEntityWithNullFields() {
        val entity = draft().toFailureEntity("InsufficientFunds", "Not enough money")

        assertNull(entity.domesticPaymentId)
        assertEquals("InsufficientFunds", entity.errorKind)
        assertEquals("Not enough money", entity.errorDescription)
        assertNull(entity.status)
        assertNull(entity.settlementDateTime)
        assertNull(entity.syncedAt)
    }

    @Test
    fun mapsEntityToPaymentHistoryItemForTerminalSuccess() {
        val entity = receipt(PaymentStatus.AcceptedSettlementCompleted).toEntity(draft())
        val item = entity.toPaymentHistoryItem()

        assertEquals("19901", item.id)
        assertEquals("19901", item.domesticPaymentId)
        assertEquals("Mr Dharani C", item.creditorName)
        assertEquals(10_000L, item.amountMinorUnits)
        assertEquals("Sent", item.statusLabel)
        assertFalse(item.isFailure)
        assertFalse(item.isInFlight)
    }

    @Test
    fun mapsEntityToPaymentHistoryItemForInFlight() {
        val entity = receipt(PaymentStatus.AcceptedSettlementInProcess).toEntity(draft())
        val item = entity.toPaymentHistoryItem()

        assertEquals("Processing", item.statusLabel)
        assertFalse(item.isFailure)
        assertTrue(item.isInFlight)
    }

    @Test
    fun mapsEntityToPaymentHistoryItemForTerminalFailure() {
        val entity = receipt(PaymentStatus.Rejected).toEntity(draft())
        val item = entity.toPaymentHistoryItem()

        assertEquals("Failed", item.statusLabel)
        assertTrue(item.isFailure)
        assertFalse(item.isInFlight)
    }

    @Test
    fun mapsFailureEntityToPaymentHistoryItem() {
        val entity = draft().toFailureEntity("InsufficientFunds", "Not enough money")
        val item = entity.toPaymentHistoryItem()

        assertNull(item.domesticPaymentId)
        assertTrue(item.isFailure)
        assertFalse(item.isInFlight)
        assertEquals("Not enough money", item.statusLabel)
        assertEquals("Not enough money", item.errorDescription)
    }

    @Test
    fun usesNicknameWhenAvailable() {
        val d = draft().copy(
            debtorAccount = draft().debtorAccount.copy(nickname = "My Current"),
        )
        val entity = receipt().toEntity(d)

        assertEquals("My Current", entity.debtorName)
    }

    @Test
    fun fallsBackToAccountSubTypeWhenNicknameIsBlank() {
        val entity = receipt().toEntity(draft())

        assertEquals("CurrentAccount", entity.debtorName)
    }

    @Test
    fun omitsSettlementDateTimeWhenBlank() {
        val r = receipt().copy(settlementDateTime = "")
        val entity = r.toEntity(draft())

        assertNull(entity.settlementDateTime)
    }

    @Test
    fun omitsReferenceWhenNull() {
        val entity = receipt().copy(reference = "").toEntity(draft(reference = null))

        assertNull(entity.reference)
    }

    @Test
    fun generatesUniqueIdForEachFailureEntity() {
        val e1 = draft().toFailureEntity("NetworkError", "Connection failed")
        val e2 = draft().toFailureEntity("NetworkError", "Connection failed")

        assertTrue(e1.id != e2.id)
    }
}
