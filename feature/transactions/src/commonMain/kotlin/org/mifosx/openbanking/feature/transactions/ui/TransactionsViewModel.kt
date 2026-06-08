/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.transactions.ui

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
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import org.mifosx.openbanking.core.data.accounts.AccountsRepository
import org.mifosx.openbanking.core.data.payments.PaymentsRepository
import org.mifosx.openbanking.core.data.transactions.CounterpartyNameResolver
import org.mifosx.openbanking.core.data.transactions.TransactionsRepository
import org.mifosx.openbanking.core.data.transactions.counterpartyDisplayName
import org.mifosx.openbanking.core.model.obp.Transaction
import org.mifosx.openbanking.core.model.obp.TransactionRequestSummary
import template.core.base.store.screen.DataFreshness
import template.core.base.store.screen.ScreenState
import kotlin.time.Clock

enum class TransactionTypeFilter { ALL, DEBIT, CREDIT, PENDING }

enum class DateRangePreset(val label: String, val days: Int?) {
    LAST_7_DAYS("7 days", 7),
    LAST_30_DAYS("30 days", 30),
    LAST_60_DAYS("60 days", 60),
    LAST_90_DAYS("90 days", 90),
    CUSTOM("Custom", null),
}

/** Active date window. [start]/[end] are resolved bounds (null = unbounded). */
@Immutable
data class DateRangeFilter(
    val preset: DateRangePreset = DateRangePreset.LAST_30_DAYS,
    val start: LocalDate? = null,
    val end: LocalDate? = null,
) {
    val label: String
        get() = if (preset == DateRangePreset.CUSTOM && start != null && end != null) {
            "$start – $end"
        } else {
            preset.label
        }
}

/** An initiated-but-unbooked payment (above the SCA threshold, awaiting confirmation). */
@Immutable
data class PendingPayment(
    val id: String,
    val description: String,
    val amount: String,
    val currency: String,
    val date: LocalDate?,
)

/** Booked transactions for one calendar day (groups render newest-day first). */
@Immutable
data class TransactionDayGroup(
    val date: LocalDate,
    val transactions: List<Transaction>,
)

/** Month-to-date totals over booked transactions only (pending payments excluded). */
@Immutable
data class MonthlySummary(
    val spent: String,
    val received: String,
    val currency: String,
)

/**
 * Transaction History ViewModel for one account. Booked rows come from the v6 transactions
 * endpoint (attributes inline for TXN_TYPE badges); pending rows are transaction-requests that
 * are INITIATED with no booked transaction (SCA challenge not yet answered). The sandbox caps
 * the window at the 50 newest transactions and ignores paging/date params, so filtering,
 * search, the date range, and pagination ([PAGE_SIZE] rows per page) all run client-side.
 */
class TransactionsViewModel(
    private val transactionsRepository: TransactionsRepository,
    private val paymentsRepository: PaymentsRepository,
    private val accountsRepository: AccountsRepository,
    private val counterpartyNameResolver: CounterpartyNameResolver,
    private val bankId: String,
    private val accountId: String,
    private val todayProvider: () -> LocalDate = { Clock.System.todayIn(TimeZone.currentSystemDefault()) },
) : ViewModel() {

    private val rawState = MutableStateFlow<RawState>(RawState.Loading)
    private val filters = MutableStateFlow(FilterState())

    val uiState: StateFlow<ScreenState<TransactionsContent>> =
        combine(rawState, filters) { raw, f -> project(raw, f) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ScreenState.Loading,
            )

    init {
        load()
    }

    fun onRetry() = load()

    fun onQueryChanged(query: String) = filters.update { it.copy(query = query, page = 1) }

    fun onFilterChanged(filter: TransactionTypeFilter) = filters.update { it.copy(filter = filter, page = 1) }

    fun onRangePresetSelected(preset: DateRangePreset) {
        if (preset == DateRangePreset.CUSTOM) return // custom arrives via onCustomRangeSelected
        val today = todayProvider()
        val start = preset.days?.let { today.minus(DatePeriod(days = it)) }
        filters.update { it.copy(range = DateRangeFilter(preset, start, null), page = 1) }
    }

    fun onCustomRangeSelected(start: LocalDate, end: LocalDate) = filters.update {
        it.copy(range = DateRangeFilter(DateRangePreset.CUSTOM, start, end), page = 1)
    }

    fun onLoadMore() = filters.update { it.copy(page = it.page + 1) }

    private fun load() {
        rawState.value = RawState.Loading
        viewModelScope.launch {
            val transactions = transactionsRepository
                .listTransactionsWithAttributes(bankId, accountId)
                .getOrElse {
                    rawState.value = RawState.Failed(it)
                    return@launch
                }
            // Pending payments are supplementary — a failure here must not break the screen.
            val requests = paymentsRepository.listTransactionRequests(bankId, accountId)
                .getOrElse { emptyList() }
            val currency = accountsRepository.accountDetail(bankId, accountId)
                .getOrNull()?.balance?.currency?.takeIf { it.isNotBlank() }
                ?: transactions.firstOrNull()?.details?.value?.currency.orEmpty().ifBlank { "EUR" }
            val counterpartyNames = runCatching {
                counterpartyNameResolver.resolve(bankId, accountId, transactions)
            }.getOrDefault(emptyMap())
            val placeholder = runCatching { counterpartyNameResolver.placeholderHolder() }.getOrDefault("")
            rawState.value = RawState.Loaded(
                transactions = transactions,
                pendingRequests = requests.filter { it.isPending },
                currency = currency,
                counterpartyNames = counterpartyNames,
                counterpartyPlaceholder = placeholder,
            )
        }
    }

    private fun project(raw: RawState, f: FilterState): ScreenState<TransactionsContent> = when (raw) {
        is RawState.Loading -> ScreenState.Loading
        is RawState.Failed -> ScreenState.Error(raw.error)
        is RawState.Loaded -> projectLoaded(raw, f)
    }

    private fun projectLoaded(raw: RawState.Loaded, f: FilterState): ScreenState<TransactionsContent> {
        val pending = visiblePending(raw, f)
        val booked = visibleBooked(raw, f)
        if (pending.isEmpty() && booked.isEmpty()) return ScreenState.Empty

        val visible = booked.take(f.page * PAGE_SIZE)
        val groups = visible
            .groupBy { it.completedDate ?: todayProvider() }
            .toList()
            .sortedByDescending { it.first }
            .map { (date, txns) -> TransactionDayGroup(date, txns) }
        return ScreenState.Content(
            data = TransactionsContent(
                pending = pending,
                groups = groups,
                summary = summarize(raw),
                filter = f.filter,
                range = f.range,
                query = f.query,
                hasMore = booked.size > visible.size,
                counterpartyNames = raw.counterpartyNames,
                counterpartyPlaceholder = raw.counterpartyPlaceholder,
            ),
            freshness = DataFreshness.FRESH,
        )
    }

    private fun visiblePending(raw: RawState.Loaded, f: FilterState): List<PendingPayment> {
        if (f.filter == TransactionTypeFilter.DEBIT || f.filter == TransactionTypeFilter.CREDIT) {
            return emptyList()
        }
        return raw.pendingRequests
            .map { it.toPendingPayment() }
            .filter { p -> inRange(p.date, f.range) && matches(p.description, p.amount, f.query) }
            .sortedByDescending { it.date }
    }

    private fun visibleBooked(raw: RawState.Loaded, f: FilterState): List<Transaction> {
        if (f.filter == TransactionTypeFilter.PENDING) return emptyList()
        return raw.transactions
            .filter { t ->
                val amount = t.details.value.amount.toDoubleOrNull() ?: 0.0
                when (f.filter) {
                    TransactionTypeFilter.DEBIT -> amount < 0
                    TransactionTypeFilter.CREDIT -> amount > 0
                    else -> true
                }
            }
            .filter { t -> inRange(t.completedDate, f.range) }
            .filter { t ->
                val counterparty = counterpartyDisplayName(t, raw.counterpartyNames, raw.counterpartyPlaceholder)
                matches("${t.details.description} $counterparty", t.details.value.amount, f.query)
            }
            .sortedByDescending { it.details.completed }
    }

    /** Month-to-date totals over ALL booked transactions (unaffected by filters/search). */
    private fun summarize(raw: RawState.Loaded): MonthlySummary {
        val today = todayProvider()
        var spent = 0.0
        var received = 0.0
        raw.transactions.forEach { t ->
            val date = t.completedDate ?: return@forEach
            if (date.year != today.year || date.month != today.month) return@forEach
            val amount = t.details.value.amount.toDoubleOrNull() ?: return@forEach
            if (amount < 0) spent += -amount else received += amount
        }
        return MonthlySummary(
            spent = formatAmount(spent),
            received = formatAmount(received),
            currency = raw.currency,
        )
    }

    private fun inRange(date: LocalDate?, range: DateRangeFilter): Boolean = when {
        date == null -> true
        range.start != null && date < range.start -> false
        range.end != null && date > range.end -> false
        else -> true
    }

    private fun matches(text: String, amount: String, query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return text.contains(q, ignoreCase = true) || amount.trimStart('-', '+').contains(q)
    }

    private fun TransactionRequestSummary.toPendingPayment() = PendingPayment(
        id = id,
        description = details.description.ifBlank { "Payment" },
        amount = details.value.amount,
        currency = details.value.currency,
        date = parseIsoDate(startDate),
    )

    private data class FilterState(
        val filter: TransactionTypeFilter = TransactionTypeFilter.ALL,
        val range: DateRangeFilter = DateRangeFilter(),
        val query: String = "",
        val page: Int = 1,
    )

    private sealed interface RawState {
        data object Loading : RawState
        data class Failed(val error: Throwable) : RawState
        data class Loaded(
            val transactions: List<Transaction>,
            val pendingRequests: List<TransactionRequestSummary>,
            val currency: String,
            val counterpartyNames: Map<String, String> = emptyMap(),
            val counterpartyPlaceholder: String = "",
        ) : RawState
    }

    init {
        // Resolve the default Last-30-Days bounds against the injected clock once at start.
        onRangePresetSelected(DateRangePreset.LAST_30_DAYS)
    }

    companion object {
        const val PAGE_SIZE = 10
    }
}

/** Loaded content for the Transaction History screen. */
@Immutable
data class TransactionsContent(
    val pending: List<PendingPayment>,
    val groups: List<TransactionDayGroup>,
    val summary: MonthlySummary,
    val filter: TransactionTypeFilter,
    val range: DateRangeFilter,
    val query: String,
    val hasMore: Boolean,
    val counterpartyNames: Map<String, String> = emptyMap(),
    val counterpartyPlaceholder: String = "",
)

/** The transaction's booked date (completed timestamp), or null when unparseable. */
internal val Transaction.completedDate: LocalDate?
    get() = parseIsoDate(details.completed)

internal fun parseIsoDate(iso: String): LocalDate? =
    runCatching { LocalDate.parse(iso.take(10)) }.getOrNull()

private fun formatAmount(value: Double): String {
    val cents = kotlin.math.round(value * 100).toLong()
    val whole = cents / 100
    val frac = (cents % 100).toString().padStart(2, '0')
    return "$whole.$frac"
}
