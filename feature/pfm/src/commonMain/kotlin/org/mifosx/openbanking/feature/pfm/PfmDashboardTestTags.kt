/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.pfm

object PfmDashboardTestTags {
    const val ACCOUNT_SELECTOR = "pfm_account_selector"
    const val SUMMARY_CARD = "pfm_summary_card"
    const val OVERALL_BUDGET_CARD = "pfm_overall_budget_card"
    const val NO_BUDGET_BANNER = "pfm_no_budget_banner"
    const val DONUT_CHART = "pfm_donut_chart"
    const val MERCHANTS_CARD = "pfm_merchants_card"
    const val VIEW_ALL_TRANSACTIONS = "pfm_view_all_transactions"
    fun periodChip(period: String) = "pfm_period_$period"
    fun categoryRow(id: String) = "pfm_category_$id"
    fun budgetRow(id: String) = "pfm_budget_$id"
    fun merchantRow(name: String) = "pfm_merchant_$name"
}
