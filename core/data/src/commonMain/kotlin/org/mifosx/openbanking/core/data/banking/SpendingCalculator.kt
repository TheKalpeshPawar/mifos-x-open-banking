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

import org.mifosx.openbanking.core.common.parseMinorUnits
import org.mifosx.openbanking.core.model.banking.SpendingSnapshot
import org.mifosx.openbanking.core.model.banking.TransactionItem

private const val OTHER_BUCKET = "Other"

/**
 * Aggregates the debit [transactions] booked in [currentYearMonth] (ISO `"YYYY-MM"`) into a
 * [SpendingSnapshot]. Credits are ignored; amounts are summed in minor units for exactness.
 * `topCategory` is the description bucket with the highest total spend — a best-effort label since
 * the OBIE feed carries no reliable category, and empty when there is nothing to show.
 */
fun computeSpendingSnapshot(
    transactions: List<TransactionItem>,
    currentYearMonth: String,
): SpendingSnapshot {
    val debits = transactions.filter { !it.isCredit && it.bookingDateTime.startsWith(currentYearMonth) }
    if (debits.isEmpty()) {
        return SpendingSnapshot(
            totalMinorUnits = 0,
            currency = transactions.firstOrNull()?.currency.orEmpty(),
            topCategory = "",
            hasData = false,
        )
    }
    var total = 0L
    var currency = ""
    val byBucket = mutableMapOf<String, Long>()
    for (tx in debits) {
        val minor = parseMinorUnits(tx.amount) ?: continue
        val magnitude = if (minor < 0) -minor else minor
        total += magnitude
        if (currency.isEmpty()) currency = tx.currency
        val bucket = tx.description.ifBlank { OTHER_BUCKET }
        byBucket[bucket] = (byBucket[bucket] ?: 0L) + magnitude
    }
    return SpendingSnapshot(
        totalMinorUnits = total,
        currency = currency,
        topCategory = byBucket.maxByOrNull { it.value }?.key.orEmpty(),
        hasData = true,
    )
}
