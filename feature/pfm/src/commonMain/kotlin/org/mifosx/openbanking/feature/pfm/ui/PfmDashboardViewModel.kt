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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.todayIn
import org.mifosx.openbanking.core.data.accounts.PfmAccountsService
import org.mifosx.openbanking.core.data.fx.FxConverter
import org.mifosx.openbanking.core.data.pfm.BudgetsRepository
import org.mifosx.openbanking.core.data.pfm.PfmBudgets
import org.mifosx.openbanking.core.data.pfm.analyze
import org.mifosx.openbanking.core.data.pfm.periodRange
import org.mifosx.openbanking.core.data.transactions.TransactionsRepository
import org.mifosx.openbanking.core.datastore.UserPreferencesRepository
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.core.model.obp.Transaction
import org.mifosx.openbanking.core.model.pfm.CategorySpend
import org.mifosx.openbanking.core.model.pfm.MerchantSpend
import org.mifosx.openbanking.core.model.pfm.PERSONAL_TAXONOMY
import org.mifosx.openbanking.core.model.pfm.PfmInsights
import org.mifosx.openbanking.core.model.pfm.PfmPeriod
import org.mifosx.openbanking.core.model.pfm.PfmScope
import org.mifosx.openbanking.core.model.pfm.PfmSummary
import org.mifosx.openbanking.core.model.pfm.pfmScope
import template.core.base.store.screen.DataFreshness
import template.core.base.store.screen.ScreenState
import kotlin.time.Clock

/**
 * Spending Insights unified across ALL personal accounts (current, wallet and savings —
 * business accounts route to Business Insights instead). Each personal account's
 * transactions are fetched in parallel and amounts convert per-transaction into the base
 * currency via OBP FX rates; an unavailable rate degrades to the native amount with a
 * notice. The sandbox returns at most the 50 newest transactions per account and ignores
 * date params, so all period filtering, categorisation, budgets math and merchant grouping
 * run client-side over that window. Budgets persist as OBP personal-data fields
 * (cross-device); a budgets fetch failure degrades to "no budgets" instead of failing
 * the screen.
 */
class PfmDashboardViewModel(
    private val transactionsRepository: TransactionsRepository,
    private val pfmAccountsService: PfmAccountsService,
    private val fxConverter: FxConverter,
    private val budgetsRepository: BudgetsRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val todayProvider: () -> LocalDate = { Clock.System.todayIn(TimeZone.currentSystemDefault()) },
) : ViewModel() {

    private val rawState = MutableStateFlow<RawState>(RawState.Loading)
    private val selection = MutableStateFlow(Selection())

    private val noticeState = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = noticeState.asStateFlow()

    val uiState: StateFlow<ScreenState<PfmContent>> =
        combine(rawState, selection) { raw, sel -> project(raw, sel) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ScreenState.Loading,
            )

    init {
        load()
    }

    /** Retry drops the classified-accounts session cache so a degraded classification heals. */
    fun onRetry() {
        pfmAccountsService.invalidate()
        load()
    }

    fun onNoticeConsumed() {
        noticeState.value = null
    }

    /** CUSTOM arrives via [onCustomRangeSelected]; the chip only opens the picker. */
    fun onPeriodSelected(period: PfmPeriod) {
        if (period == PfmPeriod.CUSTOM) return
        selection.update { it.copy(period = period, customStart = null, customEnd = null) }
    }

    fun onCustomRangeSelected(start: LocalDate, end: LocalDate) {
        selection.update { it.copy(period = PfmPeriod.CUSTOM, customStart = start, customEnd = end) }
    }

    fun onSaveBudget(categoryId: String, amountText: String) {
        val amount = amountText.trim().toDoubleOrNull()
        if (amount == null || amount <= 0) {
            noticeState.value = "Enter a valid budget amount, e.g. 350."
            return
        }
        viewModelScope.launch {
            budgetsRepository.saveBudget(categoryId, amount)
                .onSuccess {
                    val refreshed = budgetsRepository.budgets().getOrNull()
                    rawState.update { raw ->
                        if (raw is RawState.Loaded && refreshed != null) raw.copy(budgets = refreshed) else raw
                    }
                }
                .onFailure { noticeState.value = "Could not save budget. Please try again." }
        }
    }

    private fun load() {
        rawState.value = RawState.Loading
        viewModelScope.launch {
            val accounts = pfmAccountsService.classifiedAccounts()
                .getOrElse {
                    rawState.value = RawState.Failed(it)
                    return@launch
                }
            val personal = accounts.filter { it.pfmScope == PfmScope.PERSONAL }
            if (personal.isEmpty()) {
                rawState.value = RawState.Failed(IllegalStateException("No personal accounts"))
                return@launch
            }
            val perAccount = coroutineScope {
                personal.map { account ->
                    async {
                        transactionsRepository
                            .listTransactionsWithAttributes(account.bankId, account.accountIdOrId)
                            .getOrElse { emptyList() }
                    }
                }.awaitAll()
            }
            val defaultAccount = resolveDefaultAccount(personal)
            val baseCurrency = budgetsRepository.baseCurrency().getOrNull()
                ?: defaultAccount.balance.currency.ifBlank { "EUR" }
            val transactions = perAccount.flatten()
            val rateByCurrency = transactions
                .map { it.details.value.currency }
                .distinct()
                .filter { it.isNotBlank() }
                .associateWith { currency -> fxConverter.rate(currency, baseCurrency) }
            if (rateByCurrency.any { it.value == null }) {
                noticeState.value = "Some amounts could not be converted to $baseCurrency."
            }
            val budgets = budgetsRepository.budgets().getOrElse { PfmBudgets() }
            rawState.value = RawState.Loaded(
                personalAccounts = personal,
                defaultAccount = defaultAccount,
                transactions = transactions,
                budgets = budgets,
                baseCurrency = baseCurrency,
                rateByCurrency = rateByCurrency,
            )
        }
    }

    /** Persisted default account → checking-type → first, over personal accounts only. */
    private fun resolveDefaultAccount(accounts: List<Account>): Account {
        val defaultId = userPreferencesRepository.userData.value.defaultAccountId
        return accounts.firstOrNull { it.accountIdOrId == defaultId }
            ?: accounts.firstOrNull { it.typeOrProduct.contains("checking", ignoreCase = true) }
            ?: accounts.first()
    }

    private fun project(raw: RawState, sel: Selection): ScreenState<PfmContent> = when (raw) {
        is RawState.Loading -> ScreenState.Loading
        is RawState.Failed -> ScreenState.Error(raw.error)
        is RawState.Loaded -> {
            val today = todayProvider()
            val (start, end) = if (sel.period == PfmPeriod.CUSTOM && sel.customStart != null && sel.customEnd != null) {
                sel.customStart to sel.customEnd
            } else {
                periodRange(sel.period, today)
            }
            val amountOf: (Transaction) -> Double = { txn ->
                val native = txn.details.value.amount.toDoubleOrNull() ?: 0.0
                native * (raw.rateByCurrency[txn.details.value.currency] ?: 1.0)
            }
            val insights = analyze(raw.transactions, start, end, PERSONAL_TAXONOMY, amountOf)
            ScreenState.Content(
                data = PfmContent(
                    accountCount = raw.personalAccounts.size,
                    navBankId = raw.defaultAccount.bankId,
                    navAccountId = raw.defaultAccount.accountIdOrId,
                    period = sel.period,
                    periodLabel = periodLabel(sel.period, start, end),
                    currency = raw.baseCurrency,
                    summary = insights.summary,
                    categories = insights.categories,
                    budgetRows = budgetRows(insights, raw.budgets),
                    overallBudget = raw.budgets.overallLimit?.let { limit ->
                        OverallBudget(
                            spent = insights.summary.spent,
                            limit = limit,
                            percent = percentOf(insights.summary.spent, limit),
                        )
                    },
                    topMerchants = insights.topMerchants,
                    hasActivity = insights.summary.spent > 0 || insights.summary.received > 0,
                ),
                freshness = DataFreshness.FRESH,
            )
        }
    }

    private fun budgetRows(insights: PfmInsights, budgets: PfmBudgets): List<BudgetRow> =
        BUDGET_CATEGORIES.map { def ->
            val spent = insights.categories.firstOrNull { it.id == def.id }?.amount ?: 0.0
            val limit = budgets.categoryLimits[def.id]
            BudgetRow(
                categoryId = def.id,
                label = def.label,
                spent = spent,
                limit = limit,
                percent = limit?.let { percentOf(spent, it) },
            )
        }

    private data class Selection(
        val period: PfmPeriod = PfmPeriod.THIS_MONTH,
        val customStart: LocalDate? = null,
        val customEnd: LocalDate? = null,
    )

    private sealed interface RawState {
        data object Loading : RawState
        data class Failed(val error: Throwable) : RawState
        data class Loaded(
            val personalAccounts: List<Account>,
            val defaultAccount: Account,
            val transactions: List<Transaction>,
            val budgets: PfmBudgets,
            val baseCurrency: String,
            val rateByCurrency: Map<String, Double?>,
        ) : RawState
    }

    companion object {
        /** Categories that can carry a budget — taxonomy-driven exclusion set. */
        internal val BUDGET_CATEGORIES = PERSONAL_TAXONOMY.categories
            .filter { it.id !in PERSONAL_TAXONOMY.budgetExcludedIds }
    }
}

/** Loaded content for the Spending Insights dashboard. */
@Immutable
data class PfmContent(
    val accountCount: Int,
    val navBankId: String,
    val navAccountId: String,
    val period: PfmPeriod,
    val periodLabel: String,
    val currency: String,
    val summary: PfmSummary,
    val categories: List<CategorySpend>,
    val budgetRows: List<BudgetRow>,
    val overallBudget: OverallBudget?,
    val topMerchants: List<MerchantSpend>,
    val hasActivity: Boolean,
)

/** One category's budget state (limit null = not set yet). */
@Immutable
data class BudgetRow(
    val categoryId: String,
    val label: String,
    val spent: Double,
    val limit: Double?,
    val percent: Int?,
)

/** Overall monthly budget vs. the period's spend. */
@Immutable
data class OverallBudget(
    val spent: Double,
    val limit: Double,
    val percent: Int,
) {
    val remaining: Double get() = (limit - spent).coerceAtLeast(0.0)
}

private fun percentOf(spent: Double, limit: Double): Int =
    if (limit <= 0) 0 else ((spent / limit) * 100 + 0.5).toInt()

private val MONTH_NAMES = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

internal fun periodLabel(period: PfmPeriod, start: LocalDate, end: LocalDate): String {
    fun month(date: LocalDate) = MONTH_NAMES[date.month.number - 1]
    return when (period) {
        PfmPeriod.THIS_MONTH, PfmPeriod.LAST_MONTH -> "${month(start)} ${start.year}"
        PfmPeriod.LAST_3_MONTHS -> "${month(start)} – ${month(end)} ${end.year}"
        PfmPeriod.CUSTOM -> "${start.day} ${month(start).take(3)} ${start.year} – " +
            "${end.day} ${month(end).take(3)} ${end.year}"
    }
}
