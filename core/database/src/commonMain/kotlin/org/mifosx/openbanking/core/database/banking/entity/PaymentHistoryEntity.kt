/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.database.banking.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Local snapshot of a submitted or failed payment — at most 5 rows, ordered by recency.
 *
 * Submitted payments carry the bank's [domesticPaymentId] and [status]; pre-submission failures
 * carry an [errorKind] and [errorDescription] instead. Only in-flight submitted payments are
 * refreshed via the API; everything else is terminal.
 *
 * @property domesticPaymentId The bank's id, or null when the payment never reached the bank.
 * @property errorKind The failure reason label (e.g. "InsufficientFunds"), or null on success.
 * @property errorDescription Human-readable failure message.
 * @property status The OBIE status code (ACSP, ACSC, RJCT…), or null for pre-submission failures.
 * @property syncedAt When the status was last refreshed from the API; null for failures.
 */
@Entity(tableName = "payment_history")
data class PaymentHistoryEntity(
    @PrimaryKey val id: String,
    val domesticPaymentId: String?,
    val errorKind: String?,
    val errorDescription: String?,
    val status: String?,
    val debtorAccountId: String,
    val debtorName: String,
    val debtorIdentification: String,
    val creditorName: String,
    val creditorIdentification: String,
    val amountMinorUnits: Long,
    val currency: String,
    val reference: String?,
    val creationDateTime: String,
    val settlementDateTime: String?,
    val paymentType: String,
    val syncedAt: String?,
)
