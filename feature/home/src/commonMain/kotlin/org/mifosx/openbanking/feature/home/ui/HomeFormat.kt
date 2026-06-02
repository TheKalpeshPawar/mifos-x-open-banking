/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.home.ui

/**
 * Pure presentation helpers for the Home Dashboard. No clock / datetime / Compose imports here —
 * the Composable supplies the date parts and hour, so every function is deterministic and unit
 * testable on the JVM (desktopTest), matching the project's `Account.matchesQuery` convention.
 */

/** "Good morning / afternoon / evening" from a 24h hour. */
internal fun timeOfDayGreeting(hour: Int): String = when (hour) {
    in 0..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

private val WEEKDAYS = listOf(
    "Monday",
    "Tuesday",
    "Wednesday",
    "Thursday",
    "Friday",
    "Saturday",
    "Sunday",
)
private val MONTHS = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

/**
 * "Wednesday, 28 May 2026".
 * @param isoWeekday 1=Monday … 7=Sunday (kotlinx.datetime DayOfWeek.isoDayNumber).
 * @param month 1..12 (kotlinx.datetime LocalDate.month.number).
 */
internal fun formatDashboardDate(isoWeekday: Int, day: Int, month: Int, year: Int): String {
    val wd = WEEKDAYS.getOrNull(isoWeekday - 1) ?: ""
    val mo = MONTHS.getOrNull(month - 1) ?: ""
    val prefix = if (wd.isBlank()) "" else "$wd, "
    return "$prefix$day $mo $year".trim()
}

/**
 * Currency-prefixed, thousands-grouped, 2-decimal money — e.g. "GHS 4,250.00".
 * Keeps the server amount string (no float parsing of the magnitude beyond grouping).
 * @param signed when true, force a leading "+"/"-" before the currency (transaction rows);
 *   when false, a negative is shown as "CUR -1,234.00" and positives carry no sign (balances).
 */
internal fun formatMoney(amount: String, currency: String, signed: Boolean = false): String {
    val raw = amount.trim()
    val negative = raw.startsWith("-")
    val digits = raw.trimStart('+', '-').trim()
    val dot = digits.indexOf('.')
    val intPart = (if (dot >= 0) digits.substring(0, dot) else digits).ifBlank { "0" }
    val fracRaw = if (dot >= 0) digits.substring(dot + 1) else ""
    val frac = (fracRaw + "00").take(2)
    val grouped = groupThousands(intPart.filter { it.isDigit() }.ifBlank { "0" })
    val cur = currency.trim()
    val body = "$grouped.$frac"
    return when {
        signed -> "${if (negative) "-" else "+"}${prefixCur(cur)}$body"
        negative -> "${prefixCur(cur)}-$body"
        else -> "${prefixCur(cur)}$body"
    }
}

private fun prefixCur(cur: String): String = if (cur.isBlank()) "" else "$cur "

/** Inserts a comma every three digits from the right. */
private fun groupThousands(intDigits: String): String {
    val sb = StringBuilder()
    val n = intDigits.length
    for (i in 0 until n) {
        if (i > 0 && (n - i) % 3 == 0) sb.append(',')
        sb.append(intDigits[i])
    }
    return sb.toString()
}

/** Up to two uppercase initials from a display name. "Alex Owusu" -> "AO", "Alex" -> "A". */
internal fun initials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> ""
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}

/** A transaction amount is a credit (incoming) unless its server string is negative. */
internal fun isCredit(amount: String): Boolean = !amount.trim().startsWith("-")
