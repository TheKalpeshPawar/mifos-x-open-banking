/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.model.pfm

/** Reporting window for the insights dashboards. */
enum class PfmPeriod(val label: String) {
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    LAST_3_MONTHS("Last 3 Months"),
    CUSTOM("Custom"),
}

/** One spend category with its period total and share of total spend. */
data class CategorySpend(
    val id: String,
    val label: String,
    val amount: Double,
    val percent: Int,
)

/** One merchant (counterparty) with its period debit total. */
data class MerchantSpend(
    val name: String,
    val count: Int,
    val amount: Double,
)

/** Period totals over booked transactions. */
data class PfmSummary(
    val spent: Double,
    val received: Double,
) {
    val net: Double get() = received - spent
}

/** Everything a dashboard derives from a transaction set: totals, categories, merchants. */
data class PfmInsights(
    val summary: PfmSummary,
    val categories: List<CategorySpend>,
    val topMerchants: List<MerchantSpend>,
    val currency: String,
)
