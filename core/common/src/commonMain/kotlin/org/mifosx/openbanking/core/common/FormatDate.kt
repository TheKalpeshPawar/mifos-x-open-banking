/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.common

import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private const val ISO_DATE_LENGTH = 10
private val MONTH_ABBREVIATIONS =
    listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/**
 * Renders the leading `yyyy-MM-dd` of an ISO-8601 timestamp as a short day/month label, e.g.
 * `"27 Jun"`. Falls back to the raw input when it is not a parseable ISO date.
 */
fun formatShortMonthDay(isoDateTime: String): String {
    val parts = isoDateTime.take(ISO_DATE_LENGTH).split('-')
    val day = parts.getOrNull(2)?.toIntOrNull()
    val month = parts.getOrNull(1)?.toIntOrNull()?.minus(1)?.let { MONTH_ABBREVIATIONS.getOrNull(it) }
    return if (isoDateTime.length >= ISO_DATE_LENGTH && day != null && month != null) {
        "$day $month"
    } else {
        isoDateTime
    }
}

fun formatDate(millis: Long): String {
    val dateTime = Instant
        .fromEpochMilliseconds(millis)
        .toLocalDateTime(TimeZone.currentSystemDefault())

    val day = dateTime.day.toString().padStart(2, '0')
    val month = dateTime.month.number.toString().padStart(2, '0')
    val year = dateTime.year
    return "$day/$month/$year"
}
