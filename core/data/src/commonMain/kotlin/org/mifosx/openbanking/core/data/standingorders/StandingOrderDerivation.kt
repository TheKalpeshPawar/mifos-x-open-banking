/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.standingorders

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import org.mifosx.openbanking.core.model.obp.StandingOrder
import org.mifosx.openbanking.core.model.obp.StandingOrderExecution
import org.mifosx.openbanking.core.model.obp.Transaction

/** TXN_TYPE code marking a standing-order payment in transaction history. */
private const val SO_CODE = "SO"

/** A series is PAUSED once no payment arrived within 1.5x its period... */
private const val PAUSED_FACTOR = 1.5

/** ...and CANCELLED once it has been silent for 3x its period. */
private const val CANCELLED_FACTOR = 3.0

/**
 * Derives standing-order rows from transaction history. OBP has no read endpoint for
 * standing orders, so outgoing transactions tagged `TXN_TYPE=SO` are grouped by
 * description into recurring series: latest amount wins, frequency is inferred from the
 * median gap between payments, and a series with no payment within 1.5x / 3x its period
 * renders as paused / cancelled.
 */
internal fun deriveStandingOrders(transactions: List<Transaction>, today: LocalDate): List<StandingOrder> =
    transactions
        .filter { it.isStandingOrderPayment() }
        .groupBy { it.details.description.trim() }
        .filterKeys { it.isNotBlank() }
        .map { (description, series) -> deriveSeries(description, series, today) }
        .sortedWith(compareBy<StandingOrder> { statusRank(it.status) }.thenBy { it.nextPaymentDate })

/**
 * The observed payments of one derived series, newest first. [standingOrderId] is the
 * derived series id (`so-derived-…`); created-on-device orders have no booked payments
 * yet, so an unknown id simply yields an empty history.
 */
internal fun deriveExecutions(transactions: List<Transaction>, standingOrderId: String): List<StandingOrderExecution> =
    transactions
        .filter { it.isStandingOrderPayment() && seriesId(it.details.description.trim()) == standingOrderId }
        .sortedByDescending { it.details.completed.ifBlank { it.details.posted } }
        .map { txn ->
            StandingOrderExecution(
                transactionId = txn.txId,
                date = txn.completedDate()?.toString().orEmpty(),
                amount = txn.details.value.amount.trimStart('-'),
                currency = txn.details.value.currency,
            )
        }

private fun Transaction.isStandingOrderPayment(): Boolean =
    txnTypeCode == SO_CODE && (details.value.amount.toDoubleOrNull() ?: 0.0) < 0

private fun seriesId(description: String): String =
    "so-derived-${description.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')}"

private fun deriveSeries(description: String, series: List<Transaction>, today: LocalDate): StandingOrder {
    val dates = series.mapNotNull { it.completedDate() }.sorted()
    val latest = series.maxBy { it.details.completed.ifBlank { it.details.posted } }
    val gapDays = dates.zipWithNext { a, b -> a.daysUntil(b) }.filter { it > 0 }
    val frequency = frequencyForGap(gapDays.median())
    val lastDate = dates.lastOrNull()
    val nextDate = lastDate?.plus(periodFor(frequency))
    val amount = latest.details.value.amount.trimStart('-')
    return StandingOrder(
        id = seriesId(description),
        name = description,
        counterpartyName = latest.otherAccount.holder.name,
        counterpartyAccount = latest.otherAccount.id,
        amountValue = amount,
        amountCurrency = latest.details.value.currency,
        frequency = frequency,
        lastPaymentDate = lastDate?.toString().orEmpty(),
        nextPaymentDate = nextDate?.toString().orEmpty(),
        status = statusFor(lastDate, frequency, today),
        created = false,
    )
}

private fun statusFor(lastDate: LocalDate?, frequency: String, today: LocalDate): String {
    if (lastDate == null) return StandingOrder.STATUS_CANCELLED
    val silentDays = lastDate.daysUntil(today)
    val period = periodDays(frequency)
    return when {
        silentDays <= (period * PAUSED_FACTOR).toInt() -> StandingOrder.STATUS_ACTIVE
        silentDays <= (period * CANCELLED_FACTOR).toInt() -> StandingOrder.STATUS_PAUSED
        else -> StandingOrder.STATUS_CANCELLED
    }
}

internal fun statusRank(status: String): Int = when (status) {
    StandingOrder.STATUS_ACTIVE -> 0
    StandingOrder.STATUS_PAUSED -> 1
    else -> 2
}

private fun Transaction.completedDate(): LocalDate? {
    val raw = details.completed.ifBlank { details.posted }
    return runCatching { LocalDate.parse(raw.substringBefore('T')) }.getOrNull()
}

/** Median payment gap in days; null when the series has a single payment. */
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

internal fun periodDays(frequency: String): Int = when (frequency) {
    "DAILY" -> 1
    "WEEKLY" -> 7
    "BI-WEEKLY" -> 14
    "YEARLY" -> 365
    else -> 30
}
