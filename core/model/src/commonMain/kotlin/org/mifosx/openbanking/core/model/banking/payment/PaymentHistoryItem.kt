/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.model.banking.payment

/**
 * One activity card on the payments hub screen — a lightweight projection of
 * [PaymentHistoryEntity] carrying only what the UI needs to render a row.
 *
 * @property isFailure True when the payment never reached the bank or was rejected.
 * @property isInFlight True when the bank is still processing the payment; this row is refreshed.
 * @property domesticPaymentId The bank's id, or null for pre-submission failures.
 */
data class PaymentHistoryItem(
    val id: String,
    val domesticPaymentId: String?,
    val debtorName: String,
    val creditorName: String,
    val creditorIdentification: String,
    val amountMinorUnits: Long,
    val currency: String,
    val creationDateTime: String,
    val isFailure: Boolean,
    val isInFlight: Boolean,
    val statusLabel: String,
    val errorDescription: String?,
)
