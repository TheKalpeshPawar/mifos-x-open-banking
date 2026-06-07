/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.directdebits

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import org.mifosx.openbanking.core.model.obp.DirectDebitMandate
import org.mifosx.openbanking.core.model.obp.Transaction

/** TXN_TYPE code marking a direct-debit collection in transaction history. */
private const val DD_CODE = "DD"

/** A mandate renders CANCELLED once no collection arrived within 1.5x its period. */
private const val INACTIVE_FACTOR = 1.5

/**
 * Derives direct-debit mandates from transaction history. OBP has no read endpoint for
 * direct debits (only POST create exists), so outgoing transactions tagged `TXN_TYPE=DD`
 * are grouped by description into collection series: latest amount wins, frequency is
 * inferred from the median gap between collections (monthly when the series has a single
 * collection), and a series silent for over 1.5x its period renders as cancelled. The
 * merchant name comes from the description — the OBP holder field carries the login
 * username placeholder for these rows and must never be displayed.
 */
internal fun deriveDirectDebits(transactions: List<Transaction>, today: LocalDate): List<DirectDebitMandate> =
    transactions
        .filter { it.isDirectDebitCollection() }
        .groupBy { it.details.description.trim() }
        .filterKeys { it.isNotBlank() }
        .map { (description, series) -> deriveMandate(description, series, today) }
        .sortedWith(compareByDescending<DirectDebitMandate> { it.isActive }.thenBy { it.merchantName })

private fun Transaction.isDirectDebitCollection(): Boolean =
    txnTypeCode == DD_CODE && (details.value.amount.toDoubleOrNull() ?: 0.0) < 0

private fun mandateId(description: String): String =
    "dd-derived-${description.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')}"

private fun deriveMandate(description: String, series: List<Transaction>, today: LocalDate): DirectDebitMandate {
    val dates = series.mapNotNull { it.completedDate() }.sorted()
    val latest = series.maxBy { it.details.completed.ifBlank { it.details.posted } }
    val gapDays = dates.zipWithNext { a, b -> a.daysUntil(b) }.filter { it > 0 }
    val frequency = frequencyForGap(gapDays.median())
    val lastDate = dates.lastOrNull()
    val merchant = merchantNameOf(description)
    return DirectDebitMandate(
        id = mandateId(description),
        merchantName = merchant,
        amountValue = latest.details.value.amount.trimStart('-'),
        amountCurrency = latest.details.value.currency,
        frequency = frequency,
        lastCollectionDate = lastDate?.toString().orEmpty(),
        nextCollectionDate = lastDate?.plus(periodFor(frequency))?.toString().orEmpty(),
        status = statusFor(lastDate, frequency, today),
        mandateReference = mandateReference(merchant, dates.firstOrNull()),
    )
}

/** "British Gas — energy" → "British Gas"; descriptions without a dash pass through. */
private fun merchantNameOf(description: String): String =
    description.substringBefore('—').trim().ifBlank { description }

/** "Netflix Subscription" + 2026-06-04 → "DD-NS-20260604" (single word → first two letters). */
private fun mandateReference(merchant: String, firstCollection: LocalDate?): String {
    val words = merchant.split(' ').filter { it.isNotBlank() }
    val initials = if (words.size >= 2) {
        words.take(2).map { it.first().uppercaseChar() }.joinToString("")
    } else {
        merchant.take(2).uppercase()
    }
    val datePart = firstCollection?.toString()?.replace("-", "").orEmpty()
    return "DD-$initials-$datePart".trimEnd('-')
}

private fun statusFor(lastDate: LocalDate?, frequency: String, today: LocalDate): String {
    if (lastDate == null) return DirectDebitMandate.STATUS_CANCELLED
    val silentDays = lastDate.daysUntil(today)
    val period = periodDays(frequency)
    return if (silentDays <= (period * INACTIVE_FACTOR).toInt()) {
        DirectDebitMandate.STATUS_ACTIVE
    } else {
        DirectDebitMandate.STATUS_CANCELLED
    }
}

private fun Transaction.completedDate(): LocalDate? {
    val raw = details.completed.ifBlank { details.posted }
    return runCatching { LocalDate.parse(raw.substringBefore('T')) }.getOrNull()
}

/** Median collection gap in days; null when the series has a single collection. */
private fun List<Int>.median(): Int? {
    if (isEmpty()) return null
    val sorted = sorted()
    return sorted[size / 2]
}

private fun frequencyForGap(medianGapDays: Int?): String = when {
    medianGapDays == null -> "MONTHLY"
    medianGapDays <= 2 -> "DAILY"
    medianGapDays <= 10 -> "WEEKLY"
    medianGapDays <= 20 -> "BI-WEEKLY"
    medianGapDays <= 45 -> "MONTHLY"
    else -> "YEARLY"
}

private fun periodFor(frequency: String): DatePeriod = when (frequency) {
    "DAILY" -> DatePeriod(days = 1)
    "WEEKLY" -> DatePeriod(days = 7)
    "BI-WEEKLY" -> DatePeriod(days = 14)
    "YEARLY" -> DatePeriod(years = 1)
    else -> DatePeriod(months = 1)
}

private fun periodDays(frequency: String): Int = when (frequency) {
    "DAILY" -> 1
    "WEEKLY" -> 7
    "BI-WEEKLY" -> 14
    "YEARLY" -> 365
    else -> 30
}
