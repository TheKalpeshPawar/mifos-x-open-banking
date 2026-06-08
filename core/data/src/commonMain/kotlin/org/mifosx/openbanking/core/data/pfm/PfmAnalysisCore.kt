/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.pfm

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.mifosx.openbanking.core.model.obp.Transaction
import org.mifosx.openbanking.core.model.pfm.CategorySpend
import org.mifosx.openbanking.core.model.pfm.MerchantSpend
import org.mifosx.openbanking.core.model.pfm.PERSONAL_TAXONOMY
import org.mifosx.openbanking.core.model.pfm.PfmCategory
import org.mifosx.openbanking.core.model.pfm.PfmInsights
import org.mifosx.openbanking.core.model.pfm.PfmPeriod
import org.mifosx.openbanking.core.model.pfm.PfmSummary
import org.mifosx.openbanking.core.model.pfm.PfmTaxonomy

/**
 * Resolves the spend category for one transaction. Keyword matches (description and
 * counterparty name) win over TXN_TYPE fallbacks because the sandbox tags most card
 * payments POS regardless of what they were for. Matching is token-prefix, not
 * substring — "netflix" must not match the "tfl" keyword. [counterpartyName] defaults to the
 * raw holder name; callers pass a resolved counterparty so the login-username placeholder
 * never pollutes the keyword tokens.
 */
fun categorize(
    txn: Transaction,
    taxonomy: PfmTaxonomy = PERSONAL_TAXONOMY,
    counterpartyName: String = txn.otherAccount.holder.name,
): PfmCategory {
    val tokens = "${txn.details.description} $counterpartyName"
        .lowercase()
        .split(Regex("[^a-z0-9]+"))
        .filter { it.isNotBlank() }
    taxonomy.categories.firstOrNull { def ->
        def.keywords.any { keyword -> tokens.any { it.startsWith(keyword) } }
    }?.let { return it }
    val fallbackId = taxonomy.fallback[txn.txnTypeCode] ?: taxonomy.otherId
    return taxonomy.categories.first { it.id == fallbackId }
}

/** Inclusive date window for a preset period ([PfmPeriod.CUSTOM] supplies its own). */
fun periodRange(period: PfmPeriod, today: LocalDate): Pair<LocalDate, LocalDate> = when (period) {
    PfmPeriod.THIS_MONTH -> LocalDate(today.year, today.month, 1) to today
    PfmPeriod.LAST_MONTH -> {
        val firstOfThis = LocalDate(today.year, today.month, 1)
        val firstOfLast = firstOfThis.minus(DatePeriod(months = 1))
        firstOfLast to firstOfThis.minus(DatePeriod(days = 1))
    }
    PfmPeriod.LAST_3_MONTHS -> LocalDate(today.year, today.month, 1).minus(DatePeriod(months = 2)) to today
    PfmPeriod.CUSTOM -> today to today
}

/**
 * Derives summary, category breakdown and top merchants for transactions in [start]..[end].
 * [taxonomy] selects the category set; [amountOf] maps a transaction to its signed amount —
 * callers inject FX conversion here, and the default returns the native amount. [displayName]
 * maps a transaction to its counterparty name for categorisation and merchant grouping — the
 * default returns the raw holder name, while callers inject counterparty resolution so the OBP
 * login-username placeholder is never grouped or rendered as a merchant.
 */
fun analyze(
    transactions: List<Transaction>,
    start: LocalDate,
    end: LocalDate,
    taxonomy: PfmTaxonomy = PERSONAL_TAXONOMY,
    amountOf: (Transaction) -> Double = { it.details.value.amount.toDoubleOrNull() ?: 0.0 },
    displayName: (Transaction) -> String = { it.otherAccount.holder.name },
): PfmInsights {
    val inPeriod = transactions.filter { txn ->
        val date = txn.bookedDate() ?: return@filter false
        date >= start && date <= end
    }
    val debits = inPeriod.filter { amountOf(it) < 0 }
    val spent = debits.sumOf { -amountOf(it) }
    val received = inPeriod.filter { amountOf(it) > 0 }.sumOf { amountOf(it) }

    val categories = debits
        .groupBy { categorize(it, taxonomy, displayName(it)) }
        .map { (def, txns) -> def to txns.sumOf { -amountOf(it) } }
        .sortedByDescending { it.second }
        .map { (def, amount) ->
            CategorySpend(
                id = def.id,
                label = def.label,
                amount = amount,
                percent = if (spent > 0) ((amount / spent) * 100 + 0.5).toInt() else 0,
            )
        }

    val merchants = debits
        .groupBy { displayName(it).ifBlank { it.details.description }.ifBlank { "Unknown" } }
        .map { (name, txns) ->
            MerchantSpend(name = name, count = txns.size, amount = txns.sumOf { -amountOf(it) })
        }
        .sortedByDescending { it.amount }
        .take(TOP_MERCHANTS)

    return PfmInsights(
        summary = PfmSummary(spent = spent, received = received),
        categories = categories,
        topMerchants = merchants,
        currency = inPeriod.firstOrNull()?.details?.value?.currency.orEmpty(),
    )
}

private fun Transaction.bookedDate(): LocalDate? {
    val raw = details.completed.ifBlank { details.posted }
    return runCatching { LocalDate.parse(raw.substringBefore('T')) }.getOrNull()
}

private const val TOP_MERCHANTS = 5
