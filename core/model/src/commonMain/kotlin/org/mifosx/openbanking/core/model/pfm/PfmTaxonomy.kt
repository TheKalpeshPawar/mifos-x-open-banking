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

/** A spend category; [keywords] token-prefix-match against description + counterparty name. */
data class PfmCategory(
    val id: String,
    val label: String,
    val keywords: List<String> = emptyList(),
)

/**
 * Category set the analysis engine is parameterized by. [fallback] maps a transaction's
 * TXN_TYPE code to a category id when no keyword matches; [otherId] catches everything else.
 * [budgetExcludedIds] lists categories that may appear in insights but never carry a budget.
 */
data class PfmTaxonomy(
    val categories: List<PfmCategory>,
    val fallback: Map<String, String>,
    val otherId: String = "other",
    val budgetExcludedIds: Set<String> = emptySet(),
)

/**
 * Personal spend taxonomy. Keyword matches win over TXN_TYPE fallbacks because the sandbox
 * tags most card payments POS regardless of what they were for. Transfers and cash appear
 * in insights but are excluded from budgets.
 */
val PERSONAL_TAXONOMY = PfmTaxonomy(
    categories = listOf(
        PfmCategory(
            id = "food-dining",
            label = "Food & Dining",
            keywords = listOf(
                "grocer", "tesco", "supermarket", "restaurant", "coffee", "cafe",
                "takeaway", "food", "dining", "lunch", "dinner", "bakery",
            ),
        ),
        PfmCategory(
            id = "transport",
            label = "Transport",
            keywords = listOf(
                "tfl", "transport", "uber", "taxi", "fuel", "petrol", "train",
                "bus", "parking", "rail", "metro",
            ),
        ),
        PfmCategory(
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
        PfmCategory(
            id = "bills",
            label = "Bills",
            keywords = listOf(
                "rent", "mortgage", "electric", "gas", "water", "utility", "bill",
                "insurance", "council", "broadband", "internet", "mobile", "phone",
            ),
        ),
        PfmCategory(id = "shopping", label = "Shopping"),
        PfmCategory(id = "cash", label = "Cash"),
        PfmCategory(id = "transfers", label = "Transfers"),
        PfmCategory(id = "other", label = "Other"),
    ),
    fallback = mapOf(
        "DD" to "bills",
        "SO" to "bills",
        "FEE" to "bills",
        "CHG" to "bills",
        "ATM" to "cash",
        "TFR" to "transfers",
        "POS" to "shopping",
        "ECOM" to "shopping",
    ),
    budgetExcludedIds = setOf("cash", "transfers", "other"),
)

/**
 * Business expense taxonomy, grounded in observed sandbox business transactions (payroll,
 * VAT, office/studio rent, SaaS subscriptions, insurance, treasury sweeps, client invoices).
 * Business accounts carry no budgets, so every category is budget-excluded.
 */
val BUSINESS_TAXONOMY = PfmTaxonomy(
    categories = listOf(
        PfmCategory(
            id = "payroll-contractors",
            label = "Payroll & Contractors",
            keywords = listOf("payroll", "contractor", "salary", "freelance"),
        ),
        PfmCategory(id = "tax", label = "Tax", keywords = listOf("vat", "tax", "hmrc")),
        PfmCategory(
            id = "rent-facilities",
            label = "Rent & Facilities",
            keywords = listOf("rent", "office", "studio", "utilities"),
        ),
        PfmCategory(
            id = "software-subscriptions",
            label = "Software & Subscriptions",
            keywords = listOf("saas", "software", "adobe", "subscription", "cloud", "hosting"),
        ),
        PfmCategory(id = "insurance", label = "Insurance", keywords = listOf("insurance")),
        PfmCategory(
            id = "treasury-transfers",
            label = "Treasury & Transfers",
            keywords = listOf("treasury", "sweep", "transfer"),
        ),
        PfmCategory(
            id = "income",
            label = "Income",
            keywords = listOf("invoice", "client", "receipts", "gig"),
        ),
        PfmCategory(id = "other", label = "Other"),
    ),
    fallback = mapOf("TFR" to "treasury-transfers"),
    budgetExcludedIds = setOf(
        "payroll-contractors",
        "tax",
        "rent-facilities",
        "software-subscriptions",
        "insurance",
        "treasury-transfers",
        "income",
        "other",
    ),
)
