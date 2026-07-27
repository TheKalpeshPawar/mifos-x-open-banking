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
 * Richer transaction row used by the paged Transactions screen.
 *
 * Distinct from [TransactionItem] (the Room-cached home model): it additionally carries a
 * client-derived [category] and a [isPending] flag, and is held in memory as the user pages
 * through the account's history via the OBIE `Links.Next` cursor — it is not persisted.
 *
 * @property transactionId OBIE `TransactionId` (may be blank for pending entries).
 * @property accountId OBIE `AccountId` the transaction belongs to.
 * @property description Merchant or narrative text (merchant name, else `TransactionInformation`).
 * @property bookingDateTime Raw ISO-8601 booking timestamp (OBIE `BookingDateTime`).
 * @property amount Raw decimal amount string.
 * @property currency ISO-4217 currency code.
 * @property isCredit True when OBIE `CreditDebitIndicator` is `Credit` (money in).
 * @property category Client-derived personal-finance category.
 * @property isPending True when the OBIE `Status` is a pending code (`PDNG`/`Pending`).
 */
@Serializable
data class TransactionListItem(
    val transactionId: String,
    val accountId: String,
    val description: String,
    val bookingDateTime: String,
    val amount: String,
    val currency: String,
    val isCredit: Boolean,
    val category: TransactionCategory,
    val isPending: Boolean,
)
