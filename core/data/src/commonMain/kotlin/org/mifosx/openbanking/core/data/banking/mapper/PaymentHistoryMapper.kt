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

import org.mifosx.openbanking.core.database.banking.entity.PaymentHistoryEntity
import org.mifosx.openbanking.core.model.banking.payment.PaymentDisposition
import org.mifosx.openbanking.core.model.banking.payment.PaymentDraft
import org.mifosx.openbanking.core.model.banking.payment.PaymentHistoryItem
import org.mifosx.openbanking.core.model.banking.payment.PaymentReceipt
import org.mifosx.openbanking.core.model.banking.payment.PaymentStatus
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val PAYMENT_TYPE_DOMESTIC = "domestic_payment"

private fun PaymentStatus.toLabel(): String = when (disposition) {
    PaymentDisposition.TerminalSuccess -> "Sent"
    PaymentDisposition.InProgress -> "Processing"
    PaymentDisposition.TerminalFailure -> "Failed"
}

internal fun PaymentReceipt.toEntity(draft: PaymentDraft): PaymentHistoryEntity =
    PaymentHistoryEntity(
        id = domesticPaymentId,
        domesticPaymentId = domesticPaymentId,
        errorKind = null,
        errorDescription = null,
        status = status.name,
        debtorAccountId = draft.debtorAccount.accountId,
        debtorName = draft.debtorAccount.displayName(),
        debtorIdentification = draft.debtorAccount.rawIdentification.ifBlank {
            draft.debtorAccount.sortCode + draft.debtorAccount.accountNumber
        },
        creditorName = draft.creditor.name,
        creditorIdentification = draft.creditor.identification,
        amountMinorUnits = draft.amountMinorUnits,
        currency = draft.currency,
        reference = draft.reference,
        creationDateTime = creationDateTime,
        settlementDateTime = settlementDateTime.takeIf { it.isNotBlank() },
        paymentType = PAYMENT_TYPE_DOMESTIC,
        syncedAt = null,
    )

internal fun PaymentDraft.toFailureEntity(
    errorKind: String,
    errorDescription: String,
): PaymentHistoryEntity = PaymentHistoryEntity(
    id = errorId(),
    domesticPaymentId = null,
    errorKind = errorKind,
    errorDescription = errorDescription,
    status = null,
    debtorAccountId = debtorAccount.accountId,
    debtorName = debtorAccount.displayName(),
    debtorIdentification = debtorAccount.rawIdentification.ifBlank {
        debtorAccount.sortCode + debtorAccount.accountNumber
    },
    creditorName = creditor.name,
    creditorIdentification = creditor.identification,
    amountMinorUnits = amountMinorUnits,
    currency = currency,
    reference = reference,
    creationDateTime = "",
    settlementDateTime = null,
    paymentType = PAYMENT_TYPE_DOMESTIC,
    syncedAt = null,
)

internal fun PaymentHistoryEntity.toPaymentHistoryItem(): PaymentHistoryItem {
    val resolved = status?.let(PaymentStatus.Companion::fromWire)
    return PaymentHistoryItem(
        id = id,
        domesticPaymentId = domesticPaymentId,
        debtorName = debtorName,
        creditorName = creditorName,
        creditorIdentification = creditorIdentification,
        amountMinorUnits = amountMinorUnits,
        currency = currency,
        creationDateTime = creationDateTime,
        isFailure = errorKind != null ||
            resolved?.disposition == PaymentDisposition.TerminalFailure,
        isInFlight = resolved?.disposition == PaymentDisposition.InProgress,
        statusLabel = resolved?.toLabel()
            ?: errorDescription
            ?: "Failed",
        errorDescription = errorDescription,
    )
}

@OptIn(ExperimentalUuidApi::class)
private fun errorId(): String = Uuid.random().toString()

private fun org.mifosx.openbanking.core.model.banking.BankAccount.displayName(): String =
    nickname.takeIf { it.isNotBlank() }
        ?: accountSubType.ifBlank { "Account" }
