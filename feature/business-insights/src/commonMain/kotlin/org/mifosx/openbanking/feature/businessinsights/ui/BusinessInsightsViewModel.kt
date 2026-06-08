/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.businessinsights.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.todayIn
import org.mifosx.openbanking.core.data.accounts.PfmAccountsService
import org.mifosx.openbanking.core.data.pfm.analyze
import org.mifosx.openbanking.core.data.pfm.periodRange
import org.mifosx.openbanking.core.data.transactions.CounterpartyNameResolver
import org.mifosx.openbanking.core.data.transactions.TransactionsRepository
import org.mifosx.openbanking.core.data.transactions.counterpartyDisplayName
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.core.model.obp.Transaction
import org.mifosx.openbanking.core.model.pfm.BUSINESS_TAXONOMY
import org.mifosx.openbanking.core.model.pfm.CategorySpend
import org.mifosx.openbanking.core.model.pfm.MerchantSpend
import org.mifosx.openbanking.core.model.pfm.PfmPeriod
import org.mifosx.openbanking.core.model.pfm.PfmScope
import org.mifosx.openbanking.core.model.pfm.pfmScope
import template.core.base.store.screen.DataFreshness
import template.core.base.store.screen.ScreenState
import kotlin.time.Clock

/**
 * Cash-flow insights for ONE business account at a time. Business accounts are independent
 * books, so there is no cross-account aggregation and no FX — amounts stay in the account's
 * native currency. There are no budgets: the screen reports money in, money out and net
 * for the period, plus an expense breakdown over the business taxonomy.
 */
class BusinessInsightsViewModel(
    private val pfmAccountsService: PfmAccountsService,
    private val transactionsRepository: TransactionsRepository,
    private val counterpartyNameResolver: CounterpartyNameResolver,
    private val todayProvider: () -> LocalDate = { Clock.System.todayIn(TimeZone.currentSystemDefault()) },
) : ViewModel() {

    private val rawState = MutableStateFlow<RawState>(RawState.Loading)
    private val selection = MutableStateFlow(Selection())

    val uiState: StateFlow<ScreenState<BizContent>> =
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
        val account = loaded.businessAccounts
            .firstOrNull { it.accountIdOrId == accountId && accountId != loaded.account.accountIdOrId }
        if (account != null) {
            load(preferredId = account.accountIdOrId)
        }
    }

    private fun load(preferredId: String? = null) {
        rawState.value = RawState.Loading
        viewModelScope.launch {
            val business = pfmAccountsService.classifiedAccounts()
                .getOrElse {
                    rawState.value = RawState.Failed(it)
                    return@launch
                }
                .filter { it.pfmScope == PfmScope.BUSINESS }
            if (business.isEmpty()) {
                rawState.value = RawState.Empty
                return@launch
            }
            val account = business.firstOrNull { it.accountIdOrId == preferredId } ?: business.first()
            val transactions = transactionsRepository
                .listTransactionsWithAttributes(account.bankId, account.accountIdOrId)
                .getOrElse {
                    rawState.value = RawState.Failed(it)
                    return@launch
                }
            // Resolve counterparties so the login-username placeholder never surfaces as a
            // merchant; a failure degrades to raw names rather than failing the screen.
            val counterpartyNames = runCatching {
                counterpartyNameResolver.resolve(account.bankId, account.accountIdOrId, transactions)
            }.getOrDefault(emptyMap())
            val counterpartyPlaceholder =
                runCatching { counterpartyNameResolver.placeholderHolder() }.getOrDefault("")
            rawState.value = RawState.Loaded(
                account = account,
                businessAccounts = business,
                transactions = transactions,
                counterpartyNames = counterpartyNames,
                counterpartyPlaceholder = counterpartyPlaceholder,
            )
        }
    }

    private fun project(raw: RawState, sel: Selection): ScreenState<BizContent> = when (raw) {
        is RawState.Loading -> ScreenState.Loading
        is RawState.Failed -> ScreenState.Error(raw.error)
        is RawState.Empty -> ScreenState.Empty
        is RawState.Loaded -> {
            val today = todayProvider()
            val (start, end) = if (sel.period == PfmPeriod.CUSTOM && sel.customStart != null && sel.customEnd != null) {
                sel.customStart to sel.customEnd
            } else {
                periodRange(sel.period, today)
            }
            val displayName: (Transaction) -> String = {
                counterpartyDisplayName(it, raw.counterpartyNames, raw.counterpartyPlaceholder)
            }
            val insights = analyze(raw.transactions, start, end, BUSINESS_TAXONOMY, displayName = displayName)
            val expenseCategories = insights.categories.filterNot { it.id == INCOME_CATEGORY_ID }
            ScreenState.Content(
                data = BizContent(
                    accounts = raw.businessAccounts,
                    selectedAccountId = raw.account.accountIdOrId,
                    accountLabel = raw.account.label.ifBlank { "Business account" },
                    bankId = raw.account.bankId,
                    period = sel.period,
                    periodLabel = bizPeriodLabel(sel.period, start, end),
                    currency = insights.currency.ifBlank { raw.account.balance.currency },
                    moneyIn = insights.summary.received,
                    moneyOut = insights.summary.spent,
                    net = insights.summary.net,
                    categories = expenseCategories,
                    topMerchants = insights.topMerchants,
                    hasActivity = insights.summary.spent > 0 || insights.summary.received > 0,
                ),
                freshness = DataFreshness.FRESH,
            )
        }
    }

    private data class Selection(
        val period: PfmPeriod = PfmPeriod.THIS_MONTH,
        val customStart: LocalDate? = null,
        val customEnd: LocalDate? = null,
    )

    private sealed interface RawState {
        data object Loading : RawState
        data object Empty : RawState
        data class Failed(val error: Throwable) : RawState
        data class Loaded(
            val account: Account,
            val businessAccounts: List<Account>,
            val transactions: List<Transaction>,
            val counterpartyNames: Map<String, String>,
            val counterpartyPlaceholder: String,
        ) : RawState
    }
}

/** Loaded content for the Business Insights screen. */
@Immutable
data class BizContent(
    val accounts: List<Account>,
    val selectedAccountId: String,
    val accountLabel: String,
    val bankId: String,
    val period: PfmPeriod,
    val periodLabel: String,
    val currency: String,
    val moneyIn: Double,
    val moneyOut: Double,
    val net: Double,
    val categories: List<CategorySpend>,
    val topMerchants: List<MerchantSpend>,
    val hasActivity: Boolean,
)

/**
 * The income category absorbs credit-keyword matches in the taxonomy but must never render
 * inside the expense donut — a debit whose description happens to mention "invoice" or
 * "client" would otherwise inflate an income slice in an expenses-only breakdown.
 */
private const val INCOME_CATEGORY_ID = "income"

private val BIZ_MONTH_NAMES = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

internal fun bizPeriodLabel(period: PfmPeriod, start: LocalDate, end: LocalDate): String {
    fun month(date: LocalDate) = BIZ_MONTH_NAMES[date.month.number - 1]
    return when (period) {
        PfmPeriod.THIS_MONTH, PfmPeriod.LAST_MONTH -> "${month(start)} ${start.year}"
        PfmPeriod.LAST_3_MONTHS -> "${month(start)} – ${month(end)} ${end.year}"
        PfmPeriod.CUSTOM -> "${start.day} ${month(start).take(3)} ${start.year} – " +
            "${end.day} ${month(end).take(3)} ${end.year}"
    }
}
