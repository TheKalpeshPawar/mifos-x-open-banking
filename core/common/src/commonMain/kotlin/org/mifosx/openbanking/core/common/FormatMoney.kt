/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.common

private const val MINOR_UNITS_PER_MAJOR = 100L
private const val FRACTION_DIGITS = 2

/**
 * Parses a decimal amount string (e.g. `"2847.63"`, `"1234"`, `"-8.4"`) into an integer count of
 * minor units (pence). Returns `null` when the string is not a well-formed decimal.
 *
 * String-based rather than floating point so summed totals stay exact.
 */
fun parseMinorUnits(amount: String): Long? {
    val trimmed = amount.trim()
    val negative = trimmed.startsWith("-")
    val unsigned = trimmed.removePrefix("-").removePrefix("+")
    val parts = unsigned.split('.')
    val whole = parts.getOrNull(0).orEmpty().ifEmpty { "0" }
    val fraction = parts.getOrNull(1).orEmpty().padEnd(FRACTION_DIGITS, '0').take(FRACTION_DIGITS)

    val wellFormed = trimmed.isNotEmpty() &&
        parts.size <= 2 &&
        whole.all { it.isDigit() } &&
        fraction.all { it.isDigit() }
    if (!wellFormed) return null

    val magnitude = whole.toLong() * MINOR_UNITS_PER_MAJOR + fraction.toLong()
    return if (negative) -magnitude else magnitude
}

/** Formats an integer count of minor units (pence) into a display string, e.g. `"£2,847.63"`. */
fun formatMinorUnits(minorUnits: Long, currency: String): String {
    val negative = minorUnits < 0
    val magnitude = if (negative) -minorUnits else minorUnits
    val major = (magnitude / MINOR_UNITS_PER_MAJOR).formatGrouped()
    val fraction = (magnitude % MINOR_UNITS_PER_MAJOR).toString().padStart(FRACTION_DIGITS, '0')
    val sign = if (negative) "-" else ""
    return "$sign${currencySymbol(currency)}$major.$fraction"
}

/** Formats a decimal amount string into a display string, falling back to the raw input. */
fun formatMoney(amount: String, currency: String): String =
    parseMinorUnits(amount)?.let { formatMinorUnits(it, currency) } ?: amount

/** Formats a transaction amount with a leading `+`/`-` for credits/debits. */
fun formatSignedMoney(amount: String, currency: String, isCredit: Boolean): String {
    val formatted = formatMoney(amount, currency)
    return if (isCredit) "+ $formatted" else "- $formatted"
}

/** Maps an ISO-4217 currency code to its symbol, falling back to the code plus a space. */
fun currencySymbol(currency: String): String = when (currency.uppercase()) {
    "GBP" -> "£"
    "EUR" -> "€"
    "USD" -> "$"
    else -> if (currency.isBlank()) "" else "$currency "
}
