/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.transactions

import kotlinx.datetime.LocalDate
import org.mifosx.openbanking.core.model.obp.Transaction
import org.mifosx.openbanking.feature.transactions.ui.TransactionTypeFilter

internal fun TransactionTypeFilter.label(): String = when (this) {
    TransactionTypeFilter.ALL -> "All"
    TransactionTypeFilter.DEBIT -> "Debit"
    TransactionTypeFilter.CREDIT -> "Credit"
    TransactionTypeFilter.PENDING -> "Pending"
}

/**
 * Counterparty display name for a transaction. HARD RULE: the OBP placeholder holder (the
 * LOGIN USERNAME, stamped on transfers between the user's own accounts) is NEVER rendered —
 * a resolved destination holder name wins, then a real (non-placeholder) holder, then the
 * transaction description. May return "" when nothing is known; callers pick a final default.
 */
internal fun counterpartyDisplayName(
    txn: Transaction,
    resolvedNames: Map<String, String>,
    placeholder: String,
): String = resolvedNames[txn.otherAccount.id]
    ?: txn.otherAccount.holder.name.takeIf { it.isNotBlank() && it != placeholder }
    ?: txn.details.description

/** Human label for the TXN_TYPE attribute short code (falls back to the OBP detail type). */
internal fun categoryLabel(txn: Transaction): String = when (txn.txnTypeCode) {
    "POS" -> "Card payment"
    "ECOM" -> "Online"
    "ATM" -> "Cash"
    "DD" -> "Direct Debit"
    "SO" -> "Standing Order"
    "TFR" -> "Transfer"
    "SAL" -> "Salary"
    "DEP" -> "Deposit"
    "FEE" -> "Fee"
    "CHG" -> "Charge"
    "INT" -> "Interest"
    "REF" -> "Refund"
    else -> txn.details.type.ifBlank { "Payment" }
}

internal fun formatSigned(amount: Double, currency: String): String {
    val sign = if (amount < 0) "-" else "+"
    val abs = if (amount < 0) -amount else amount
    val cents = kotlin.math.round(abs * 100).toLong()
    return "$sign${symbol(currency)}${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"
}

/**
 * Amount without a sign prefix, for money that has not moved yet — pending
 * transaction-requests must not read as debited from the account.
 */
internal fun formatUnsigned(amount: Double, currency: String): String {
    val abs = if (amount < 0) -amount else amount
    val cents = kotlin.math.round(abs * 100).toLong()
    return "${symbol(currency)}${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"
}

internal fun symbol(currency: String): String = when (currency.uppercase()) {
    "GBP" -> "£"
    "EUR" -> "€"
    "USD" -> "$"
    else -> if (currency.isBlank()) "" else "$currency "
}

internal fun formatDate(date: LocalDate): String {
    val month = date.month.name.lowercase().replaceFirstChar { it.uppercase() }
    return "${date.day} $month ${date.year}"
}
