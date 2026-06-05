/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.cards

import org.mifosx.openbanking.core.model.obp.Transaction
import org.mifosx.openbanking.core.model.obp.TransactionType

/**
 * Pure presentation helpers for the My Cards screen. Kept out of the Composable file so the
 * screen stays focused on layout (and under Detekt's per-file function budget), mirroring the
 * `HomeFormat` convention — every function here is deterministic and unit-testable.
 */

/** Primary line for a transaction row — its description, else the counterparty, else a generic label. */
internal fun transactionLabel(tx: Transaction): String =
    tx.details.description.ifBlank { tx.otherAccount.holder.name.ifBlank { "Transaction" } }

/** "Card payment · 2026-05-09" — the standard transaction-type label (TXN_TYPE) plus the posted date. */
internal fun transactionSubtitle(tx: Transaction): String {
    val type = TransactionType.fromCode(tx.txnTypeCode)
    val typeLabel = if (type == TransactionType.UNKNOWN) "" else type.label
    val date = tx.details.posted.take(10)
    return listOf(typeLabel, date).filter { it.isNotBlank() }.joinToString(" · ")
}

/** Masks all but the last four digits of an OBP card number for display. */
internal fun maskedNumber(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    val last4 = digits.takeLast(4)
    return if (last4.isBlank()) raw else "•••• •••• •••• $last4"
}

/** Renders an OBP amount string with a leading minus and trailing currency code (e.g. "−3.5 GBP"). */
internal fun formatAmount(amount: String, currency: String): String {
    val value = amount.toDoubleOrNull()
    val prefix = if (value != null && value < 0) "−" else ""
    val abs = value?.let { if (it < 0) -it else it }
    val number = abs?.let { it.toString() } ?: amount.removePrefix("-")
    val code = if (currency.isBlank()) "" else " $currency"
    return "$prefix$number$code".trim()
}
