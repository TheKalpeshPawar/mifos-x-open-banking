/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking.mapper

import org.mifosx.openbanking.core.model.banking.StandingOrderItem
import org.mifosx.openbanking.core.model.banking.StandingOrdersSummary
import org.mifosx.openbanking.core.network.model.ais.standingOrders.Frequency
import org.mifosx.openbanking.core.network.model.ais.standingOrders.StandingOrder
import org.mifosx.openbanking.core.network.model.ais.standingOrders.StandingOrdersResponse

private const val ACTIVE_STATUS = "Active"

/**
 * The OBIE ISO 20022 interval codes this app has labels for, keyed by the composite
 * `Type:PointInTime` form the standard writes them in.
 *
 * Deliberately a closed table rather than a parser. The code grammar is positional and
 * bank-extensible — `IntrvlMnthDay:03:01` means "every third month, on the first" — so a partial
 * parser would confidently render a wrong sentence for a code it half-understood. An unknown key
 * falls through to the raw code instead, which is honest and still useful to a user reading it
 * against their bank statement.
 */
private val FREQUENCY_LABELS: Map<String, String> = mapOf(
    "IntrvlMnthDay:01:01" to "Monthly on the 1st",
    "IntrvlMnthDay:01:15" to "Monthly on the 15th",
    "IntrvlMnthDay:01:28" to "Monthly on the 28th",
    "IntrvlMnthDay:03:01" to "Quarterly on the 1st",
    "IntrvlMnthDay:06:01" to "Every 6 months on the 1st",
    "IntrvlWkDay:01:1" to "Weekly every Monday",
    "IntrvlWkDay:01:2" to "Weekly every Tuesday",
    "IntrvlWkDay:01:3" to "Weekly every Wednesday",
    "IntrvlWkDay:01:4" to "Weekly every Thursday",
    "IntrvlWkDay:01:5" to "Weekly every Friday",
    "IntrvlWkDay:02:1" to "Every 2 weeks on Monday",
    "IntrvlDay:14" to "Every 14 days",
    "IntrvlDay:28" to "Every 28 days",
    "IntrvlYear:01:01:01" to "Annually on 1 January",
)

/**
 * Maps the OBIE `OBReadStandingOrder6` payload into the order list, sorted active-first.
 *
 * The sort is stable, so within each status group the bank's own ordering survives — orders do not
 * shuffle between refreshes for reasons the user cannot see.
 *
 * Orders without a creditor `Name` are dropped: the payee is the only thing identifying a row to a
 * human, and a card headed by a blank line is worse than an absent one. Every other field degrades
 * to an empty string instead, so a cancelled order with no next payment still renders.
 */
fun StandingOrdersResponse.toStandingOrderItems(): List<StandingOrderItem> =
    data?.standingOrder.orEmpty()
        .mapNotNull { it.toStandingOrderItemOrNull() }
        .sortedByDescending { it.isActive }

/**
 * Wraps [toStandingOrderItems] with the active/inactive tallies the summary row renders, so the
 * counts and the list are computed from one traversal of one payload and cannot drift apart.
 */
fun StandingOrdersResponse.toStandingOrdersSummary(): StandingOrdersSummary {
    val items = toStandingOrderItems()
    val activeCount = items.count { it.isActive }
    return StandingOrdersSummary(
        items = items,
        activeCount = activeCount,
        inactiveCount = items.size - activeCount,
    )
}

private fun StandingOrder.toStandingOrderItemOrNull(): StandingOrderItem? {
    val payee = creditorAccount?.name?.takeIf { it.isNotBlank() } ?: return null
    val status = standingOrderStatusCode.orEmpty()
    val finalPayment = mandateRelatedInformation?.finalPaymentDateTime.orEmpty()
    return StandingOrderItem(
        standingOrderId = standingOrderId.orEmpty(),
        payeeName = payee,
        statusCode = status,
        isActive = status.equals(ACTIVE_STATUS, ignoreCase = true),
        nextPaymentAmount = nextPaymentAmount?.amount.orEmpty(),
        currency = nextPaymentAmount?.currency.orEmpty(),
        frequencyLabel = mandateRelatedInformation?.frequency.toFrequencyLabel(),
        nextPaymentDateTime = nextPaymentDateTime.orEmpty(),
        finalPaymentDateTime = finalPayment,
        hasFinalPayment = finalPayment.isNotBlank(),
        creditorIdentification = creditorAccount?.identification.orEmpty(),
        reference = reference.orEmpty(),
    )
}

/**
 * Decodes an OBIE [Frequency] to a human-readable interval.
 *
 * The wire format splits one composite code across two fields — `IntrvlWkDay:01:5` arrives as
 * `Type=IntrvlWkDay` plus `PointInTime=01:5` — so the key is recomposed before lookup rather than
 * matching on either half alone, which would collapse every weekly variant onto one label.
 *
 * An unrecognised combination returns the recomposed code verbatim. Showing the user the code their
 * bank sent beats showing them nothing, and it never throws: a frequency this app has no wording
 * for is a gap in a lookup table, not a broken standing order.
 */
internal fun Frequency?.toFrequencyLabel(): String {
    val code = this?.compositeCode().orEmpty()
    return FREQUENCY_LABELS[code] ?: code
}

/** Rejoins `Type` and `PointInTime` into the single `Type:PointInTime` code OBIE documents. */
private fun Frequency.compositeCode(): String {
    val interval = type.orEmpty()
    val point = pointInTime.orEmpty()
    return when {
        interval.isBlank() -> point
        point.isBlank() -> interval
        else -> "$interval:$point"
    }
}
