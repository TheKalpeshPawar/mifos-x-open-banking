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

/**
 * Month-to-date spending summary shown on the home "Spending this month" card.
 *
 * Derived locally by aggregating debit [TransactionItem]s in the current calendar month — there
 * is no server endpoint for this. [totalMinorUnits] keeps the summed value as an integer count of
 * minor units (pence) so the presentation layer formats it without re-parsing.
 *
 * @property totalMinorUnits Total spent this month, in minor currency units (pence).
 * @property currency ISO-4217 currency code of the aggregated transactions.
 * @property topCategory Best-effort label for the largest spend bucket, or empty when unknown.
 * @property hasData False when no debit transactions fell in the current month.
 */
data class SpendingSnapshot(
    val totalMinorUnits: Long,
    val currency: String,
    val topCategory: String,
    val hasData: Boolean,
)
