/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.model.banking

import kotlinx.serialization.Serializable

/**
 * UI-facing transaction row derived from the OBIE `OBTransaction6` payload.
 *
 * @property transactionId OBIE `TransactionId` (may be blank for pending entries).
 * @property accountId OBIE `AccountId` the transaction belongs to.
 * @property description Merchant or narrative text (OBIE `TransactionInformation`).
 * @property bookingDateTime Raw ISO-8601 booking timestamp (OBIE `BookingDateTime`).
 * @property amount Raw decimal amount string.
 * @property currency ISO-4217 currency code.
 * @property isCredit True when OBIE `CreditDebitIndicator` is `Credit` (money in), false for debits.
 */
@Serializable
data class TransactionItem(
    val transactionId: String,
    val accountId: String,
    val description: String,
    val bookingDateTime: String,
    val amount: String,
    val currency: String,
    val isCredit: Boolean,
)
