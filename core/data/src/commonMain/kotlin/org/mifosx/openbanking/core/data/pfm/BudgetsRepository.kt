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

import org.mifosx.openbanking.core.data.obp.toResult
import org.mifosx.openbanking.core.model.obp.UserAttributeRequest
import org.mifosx.openbanking.core.network.api.UserAttributesApi

/** The user's PFM budgets: per-category limits plus the optional overall monthly limit. */
data class PfmBudgets(
    /** categoryId → monthly limit. */
    val categoryLimits: Map<String, Double> = emptyMap(),
    val overallLimit: Double? = null,
)

/**
 * PFM budgets persisted as OBP personal-data fields under `pfm_budget_<categoryId>`
 * (overall = `pfm_budget_overall`). The endpoint appends on every POST instead of
 * upserting, so reads keep only the newest row per name (max insert_date; ties broken
 * by response order, which the sandbox returns newest-first).
 */
interface BudgetsRepository {
    suspend fun budgets(): Result<PfmBudgets>
    suspend fun saveBudget(categoryId: String, amount: Double): Result<Unit>

    /** Persisted base currency for the unified personal dashboard, or null when never set. */
    suspend fun baseCurrency(): Result<String?>
    suspend fun saveBaseCurrency(code: String): Result<Unit>
}

class BudgetsRepositoryImpl(
    private val api: UserAttributesApi,
) : BudgetsRepository {

    override suspend fun budgets(): Result<PfmBudgets> =
        api.listPersonalDataFields().toResult().map { response ->
            val newestPerName = response.userAttributes
                .filter { it.name.startsWith(PREFIX) }
                .groupBy { it.name }
                .mapValues { (_, rows) -> rows.maxByOrNull { it.insertDate }?.value }
            val limits = newestPerName.entries.mapNotNull { (name, value) ->
                val amount = value?.toDoubleOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
                name.removePrefix(PREFIX) to amount
            }.toMap()
            PfmBudgets(
                categoryLimits = limits.filterKeys { it != OVERALL },
                overallLimit = limits[OVERALL],
            )
        }

    override suspend fun saveBudget(categoryId: String, amount: Double): Result<Unit> =
        api.createPersonalDataField(
            UserAttributeRequest(name = "$PREFIX$categoryId", value = formatAmount(amount)),
        ).toResult().map { }

    override suspend fun baseCurrency(): Result<String?> =
        api.listPersonalDataFields().toResult().map { response ->
            response.userAttributes
                .filter { it.name == BASE_CURRENCY }
                .maxByOrNull { it.insertDate }
                ?.value?.takeIf { it.isNotBlank() }
        }

    override suspend fun saveBaseCurrency(code: String): Result<Unit> =
        api.createPersonalDataField(
            UserAttributeRequest(name = BASE_CURRENCY, value = code),
        ).toResult().map { }

    private fun formatAmount(amount: Double): String {
        val cents = kotlin.math.round(amount * 100).toLong()
        return "${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"
    }

    companion object {
        const val PREFIX = "pfm_budget_"
        const val OVERALL = "overall"
        const val BASE_CURRENCY = "pfm_base_currency"
    }
}
