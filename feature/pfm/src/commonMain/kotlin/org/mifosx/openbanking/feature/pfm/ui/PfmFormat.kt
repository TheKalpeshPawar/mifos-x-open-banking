/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.pfm.ui

import kotlinx.datetime.LocalDate

/** "1029.8", "EUR" → "€1029.80". */
internal fun formatMoney(value: Double, currency: String): String {
    val abs = if (value < 0) -value else value
    val cents = kotlin.math.round(abs * 100).toLong()
    val sign = if (value < 0) "-" else ""
    return "$sign${currencySymbol(currency)}${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"
}

internal fun currencySymbol(currency: String): String = when (currency.uppercase()) {
    "GBP" -> "£"
    "EUR" -> "€"
    "USD" -> "$"
    else -> if (currency.isBlank()) "" else "$currency "
}

internal fun Long.toLocalDate(): LocalDate = LocalDate.fromEpochDays((this / MILLIS_PER_DAY).toInt())

private const val MILLIS_PER_DAY = 86_400_000L
