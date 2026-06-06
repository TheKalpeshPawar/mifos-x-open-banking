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
import org.mifosx.openbanking.core.data.accounts.AccountsRepository
import org.mifosx.openbanking.core.data.pfm.BudgetsRepository
import org.mifosx.openbanking.core.data.pfm.PfmBudgets
import org.mifosx.openbanking.core.data.transactions.TransactionsRepository
import org.mifosx.openbanking.core.datastore.UserPreferencesRepository
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.core.model.obp.Transaction
import template.core.base.store.screen.DataFreshness
import template.core.base.store.screen.ScreenState
import kotlin.time.Clock

/**
 * Spending Insights for one account. The sandbox returns at most the 50 newest
 * transactions and ignores date params, so all period filtering, categorisation,
 * budgets math and merchant grouping run client-side over that window. Budgets persist
 * as OBP personal-data fields (cross-device); a budgets fetch failure degrades to
 * "no budgets" instead of failing the screen.
 */
class PfmDashboardViewModel(
    private val transactionsRepository: TransactionsRepository,
    private val accountsRepository: AccountsRepository,
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

    fun onRetry() = load()

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

    fun onAccountSelected(accountId: String) {
        val loaded = rawState.value as? RawState.Loaded ?: return
        val account = loaded.accounts
            .firstOrNull { it.accountIdOrId == accountId && accountId != loaded.account.accountIdOrId }
        if (account != null) {
            viewModelScope.launch {
                rawState.value = RawState.Loading
                fetchFor(account, loaded.accounts)
            }
        }
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
            val accounts = accountsRepository.myAccounts()
                .getOrElse {
                    rawState.value = RawState.Failed(it)
                    return@launch
                }
            if (accounts.isEmpty()) {
                rawState.value = RawState.Failed(IllegalStateException("No accounts"))
                return@launch
            }
            fetchFor(resolveAccount(accounts), accounts)
        }
    }

    private suspend fun fetchFor(account: Account, accounts: List<Account>) {
        val transactions = transactionsRepository
            .listTransactionsWithAttributes(account.bankId, account.accountIdOrId)
            .getOrElse {
                rawState.value = RawState.Failed(it)
                return
            }
        // Budgets are supplementary — a failure must not break the dashboard.
        val budgets = budgetsRepository.budgets().getOrElse { PfmBudgets() }
        rawState.value = RawState.Loaded(account, accounts, transactions, budgets)
    }

    /** Persisted default account → checking-type → first. */
    private fun resolveAccount(accounts: List<Account>): Account {
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
            val insights = analyze(raw.transactions, start, end)
            ScreenState.Content(
                data = PfmContent(
                    accounts = raw.accounts,
                    selectedAccountId = raw.account.accountIdOrId,
                    accountLabel = raw.account.label.ifBlank { "Account" },
                    bankId = raw.account.bankId,
                    period = sel.period,
                    periodLabel = periodLabel(sel.period, start, end),
                    currency = insights.currency.ifBlank { raw.account.balance.currency },
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
            val account: Account,
            val accounts: List<Account>,
            val transactions: List<Transaction>,
            val budgets: PfmBudgets,
        ) : RawState
    }

    companion object {
        /** Categories that can carry a budget (cash/transfers/other excluded). */
        internal val BUDGET_CATEGORIES = ALL_CATEGORIES.filter { it.id !in setOf("cash", "transfers", "other") }
    }
}

/** Loaded content for the Spending Insights dashboard. */
@Immutable
data class PfmContent(
    val accounts: List<Account>,
    val selectedAccountId: String,
    val accountLabel: String,
    val bankId: String,
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
