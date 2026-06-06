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

import androidx.compose.runtime.Immutable
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.mifosx.openbanking.core.model.obp.Transaction

/** Reporting window for the Spending Insights dashboard. */
enum class PfmPeriod(val label: String) {
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    LAST_3_MONTHS("Last 3 Months"),
    CUSTOM("Custom"),
}

/** One spend category with its period total and share of total spend. */
@Immutable
data class CategorySpend(
    val id: String,
    val label: String,
    val amount: Double,
    val percent: Int,
)

/** One merchant (counterparty) with its period debit total. */
@Immutable
data class MerchantSpend(
    val name: String,
    val count: Int,
    val amount: Double,
)

/** Period totals over booked transactions. */
@Immutable
data class PfmSummary(
    val spent: Double,
    val received: Double,
) {
    val net: Double get() = received - spent
}

/** Everything the dashboard derives from one account's transaction history. */
@Immutable
data class PfmInsights(
    val summary: PfmSummary,
    val categories: List<CategorySpend>,
    val topMerchants: List<MerchantSpend>,
    val currency: String,
)

/** A spend category definition; [keywords] match against description + counterparty name. */
internal data class CategoryDef(val id: String, val label: String, val keywords: List<String>)

/**
 * Spend taxonomy. Keyword matches (description/counterparty) win over TXN_TYPE fallbacks
 * because the sandbox tags most card payments POS regardless of what they were for.
 */
internal val PFM_CATEGORIES = listOf(
    CategoryDef(
        id = "food-dining",
        label = "Food & Dining",
        keywords = listOf(
            "grocer", "tesco", "supermarket", "restaurant", "coffee", "cafe",
            "takeaway", "food", "dining", "lunch", "dinner", "bakery",
        ),
    ),
    CategoryDef(
        id = "transport",
        label = "Transport",
        keywords = listOf(
            "tfl", "transport", "uber", "taxi", "fuel", "petrol", "train",
            "bus", "parking", "rail", "metro",
        ),
    ),
    CategoryDef(
        id = "entertainment",
        label = "Entertainment",
        keywords = listOf(
            "netflix",
            "spotify",
            "cinema",
            "entertainment",
            "subscription",
            "game",
            "music",
            "stream",
        ),
    ),
    CategoryDef(
        id = "bills",
        label = "Bills",
        keywords = listOf(
            "rent", "mortgage", "electric", "gas", "water", "utility", "bill",
            "insurance", "council", "broadband", "internet", "mobile", "phone",
        ),
    ),
)

private val CATEGORY_SHOPPING = CategoryDef("shopping", "Shopping", emptyList())
private val CATEGORY_CASH = CategoryDef("cash", "Cash", emptyList())
private val CATEGORY_TRANSFERS = CategoryDef("transfers", "Transfers", emptyList())
private val CATEGORY_BILLS = PFM_CATEGORIES.first { it.id == "bills" }
private val CATEGORY_OTHER = CategoryDef("other", "Other", emptyList())

/** Every category id the dashboard knows, in display order — drives budget slugs too. */
internal val ALL_CATEGORIES: List<CategoryDef> =
    PFM_CATEGORIES + listOf(CATEGORY_SHOPPING, CATEGORY_CASH, CATEGORY_TRANSFERS, CATEGORY_OTHER)

/** Resolves the spend category for one transaction (keywords first, TXN_TYPE fallback). */
internal fun categorize(txn: Transaction): CategoryDef {
    // Token-prefix matching, not substring — "netflix" must not match the "tfl" keyword.
    val tokens = "${txn.details.description} ${txn.otherAccount.holder.name}"
        .lowercase()
        .split(Regex("[^a-z0-9]+"))
        .filter { it.isNotBlank() }
    PFM_CATEGORIES.firstOrNull { def ->
        def.keywords.any { keyword -> tokens.any { it.startsWith(keyword) } }
    }?.let { return it }
    return when (txn.txnTypeCode) {
        "DD", "SO", "FEE", "CHG" -> CATEGORY_BILLS
        "ATM" -> CATEGORY_CASH
        "TFR" -> CATEGORY_TRANSFERS
        "POS", "ECOM" -> CATEGORY_SHOPPING
        else -> CATEGORY_OTHER
    }
}

/** Inclusive date window for a preset period ([PfmPeriod.CUSTOM] supplies its own). */
internal fun periodRange(period: PfmPeriod, today: LocalDate): Pair<LocalDate, LocalDate> = when (period) {
    PfmPeriod.THIS_MONTH -> LocalDate(today.year, today.month, 1) to today
    PfmPeriod.LAST_MONTH -> {
        val firstOfThis = LocalDate(today.year, today.month, 1)
        val firstOfLast = firstOfThis.minus(DatePeriod(months = 1))
        firstOfLast to firstOfThis.minus(DatePeriod(days = 1))
    }
    PfmPeriod.LAST_3_MONTHS -> LocalDate(today.year, today.month, 1).minus(DatePeriod(months = 2)) to today
    PfmPeriod.CUSTOM -> today to today
}

/** Derives summary, category breakdown and top merchants for transactions in [start]..[end]. */
internal fun analyze(transactions: List<Transaction>, start: LocalDate, end: LocalDate): PfmInsights {
    val inPeriod = transactions.filter { txn ->
        val date = txn.bookedDate() ?: return@filter false
        date >= start && date <= end
    }
    val debits = inPeriod.filter { it.amountValue() < 0 }
    val spent = debits.sumOf { -it.amountValue() }
    val received = inPeriod.filter { it.amountValue() > 0 }.sumOf { it.amountValue() }

    val categories = debits
        .groupBy { categorize(it) }
        .map { (def, txns) -> def to txns.sumOf { -it.amountValue() } }
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
        .groupBy { it.otherAccount.holder.name.ifBlank { it.details.description }.ifBlank { "Unknown" } }
        .map { (name, txns) ->
            MerchantSpend(name = name, count = txns.size, amount = txns.sumOf { -it.amountValue() })
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

private fun Transaction.amountValue(): Double = details.value.amount.toDoubleOrNull() ?: 0.0

private fun Transaction.bookedDate(): LocalDate? {
    val raw = details.completed.ifBlank { details.posted }
    return runCatching { LocalDate.parse(raw.substringBefore('T')) }.getOrNull()
}

private const val TOP_MERCHANTS = 5
