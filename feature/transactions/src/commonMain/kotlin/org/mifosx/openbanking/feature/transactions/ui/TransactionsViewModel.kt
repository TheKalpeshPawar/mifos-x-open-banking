/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.transactions.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.mifosx.openbanking.core.common.formatMinorUnits
import org.mifosx.openbanking.core.common.formatSignedMoney
import org.mifosx.openbanking.core.common.sumMinorUnits
import org.mifosx.openbanking.core.data.banking.TransactionsRepository
import org.mifosx.openbanking.core.model.banking.TransactionCategory
import org.mifosx.openbanking.core.model.banking.TransactionListItem
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import template.core.base.ui.viewmodel.BaseViewModel

private const val GBP = "GBP"
private const val ISO_DATE_LENGTH = 10

/** Client-side money-direction filter backing the chip row (no API round-trip). */
enum class TransactionFilter { ALL, MONEY_IN, MONEY_OUT }

/** The recoverable error classes surfaced by the transactions endpoint, resolved from the HTTP status. */
enum class TransactionsErrorKind(val recoverable: Boolean) {
    SESSION_EXPIRED(recoverable = true),
    CONSENT_WITHDRAWN(recoverable = false),
    RATE_LIMITED(recoverable = true),
    NETWORK(recoverable = true),
}

/** A display-ready transaction row. Amount is pre-formatted with its sign. */
data class TransactionRowUi(
    val key: String,
    val transactionId: String,
    val description: String,
    val amountLabel: String,
    val isCredit: Boolean,
    val category: TransactionCategory,
    val isPending: Boolean,
)

/** A date-grouped run of rows, e.g. header `SUN 28 JUN 2026`. */
data class TransactionGroup(
    val dateLabel: String,
    val rows: List<TransactionRowUi>,
)

/** Content payload: the date-grouped rows plus the period money-in / money-out totals. */
data class TransactionsData(
    val groups: List<TransactionGroup>,
    val moneyInLabel: String,
    val moneyOutLabel: String,
)

/** The rendered data region — the surrounding filter chips + search live on [TransactionsState]. */
sealed interface TransactionsUiState {
    data object Loading : TransactionsUiState
    data class Content(val data: TransactionsData) : TransactionsUiState
    data object Empty : TransactionsUiState
    data class Error(val kind: TransactionsErrorKind) : TransactionsUiState
}

data class TransactionsState(
    val accountId: String = "",
    val uiState: TransactionsUiState = TransactionsUiState.Loading,
    val activeFilter: TransactionFilter = TransactionFilter.ALL,
    val query: String = "",
    val dateFrom: LocalDate? = null,
    val dateTo: LocalDate? = null,
    val showDateRangePicker: Boolean = false,
    val hasNextPage: Boolean = false,
    val isPaginating: Boolean = false,
)

sealed interface TransactionsAction {
    data object LoadTransactions : TransactionsAction
    data object RetryLoad : TransactionsAction
    data object LoadMore : TransactionsAction
    data class FilterTransactions(val filter: TransactionFilter) : TransactionsAction
    data class SearchTransactions(val query: String) : TransactionsAction
    data object OpenDateRangePicker : TransactionsAction
    data class SetDateRange(val from: LocalDate, val to: LocalDate) : TransactionsAction
    data object DismissDateRangePicker : TransactionsAction
    data object ClearFilters : TransactionsAction
}

/**
 * Drives the account-scoped transactions screen. Pages the OBIE transaction history transparently by
 * following the `Links.Next` cursor (accumulating rows in memory), then derives the rendered state
 * from that accumulated list: client-side money-direction filter, case-insensitive search, date-range
 * filter, `BookingDateTime` grouping, and the period money-in / money-out totals. Navigation is handled
 * by the screen, so no events are emitted.
 */
class TransactionsViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: TransactionsRepository,
) : BaseViewModel<TransactionsState, Nothing, TransactionsAction>(
    initialState = TransactionsState(
        accountId = savedStateHandle.get<String>(ACCOUNT_ID_ARG).orEmpty(),
    ),
) {

    private val accountId: String = state.accountId

    /** All rows fetched so far across pages, in server order (newest first). */
    private var accumulated: List<TransactionListItem> = emptyList()
    private var nextLink: String? = null

    init {
        load()
    }

    override fun handleAction(action: TransactionsAction) {
        when (action) {
            TransactionsAction.LoadTransactions, TransactionsAction.RetryLoad -> load()
            TransactionsAction.LoadMore -> loadMore()
            is TransactionsAction.FilterTransactions -> {
                updateState { copy(activeFilter = action.filter) }
                recompute()
            }
            is TransactionsAction.SearchTransactions -> {
                updateState { copy(query = action.query) }
                recompute()
            }
            TransactionsAction.OpenDateRangePicker -> updateState { copy(showDateRangePicker = true) }
            TransactionsAction.DismissDateRangePicker -> updateState { copy(showDateRangePicker = false) }
            is TransactionsAction.SetDateRange -> {
                updateState { copy(dateFrom = action.from, dateTo = action.to, showDateRangePicker = false) }
                recompute()
            }
            TransactionsAction.ClearFilters -> {
                updateState {
                    copy(activeFilter = TransactionFilter.ALL, query = "", dateFrom = null, dateTo = null)
                }
                recompute()
            }
        }
    }

    private fun load() {
        updateState { copy(uiState = TransactionsUiState.Loading, isPaginating = false) }
        viewModelScope.launch {
            when (val result = repository.firstPage(accountId)) {
                is NetworkResult.Success -> {
                    accumulated = result.data.items
                    nextLink = result.data.nextLink
                    updateState { copy(hasNextPage = result.data.hasNextPage) }
                    recompute()
                }

                is NetworkResult.Error ->
                    updateState { copy(uiState = TransactionsUiState.Error(result.error.toErrorKind())) }
            }
        }
    }

    private fun loadMore() {
        val cursor = nextLink
        if (cursor == null || state.isPaginating) return
        updateState { copy(isPaginating = true) }
        viewModelScope.launch {
            when (val result = repository.nextPage(cursor)) {
                is NetworkResult.Success -> {
                    accumulated = accumulated + result.data.items
                    nextLink = result.data.nextLink
                    updateState { copy(isPaginating = false, hasNextPage = result.data.hasNextPage) }
                    recompute()
                }

                // A failed load-more keeps the already-shown rows; the button re-appears for another try.
                is NetworkResult.Error -> updateState { copy(isPaginating = false) }
            }
        }
    }

    /** Rebuilds the visible state from [accumulated] and the current filters — synchronous, no fetch. */
    private fun recompute() {
        val current = state
        val filtered = accumulated
            .asSequence()
            .filter { current.activeFilter.matches(it.isCredit) }
            .filter { it.matchesQuery(current.query) }
            .filter { it.matchesDateRange(current.dateFrom, current.dateTo) }
            .toList()

        val uiState = if (filtered.isEmpty()) {
            TransactionsUiState.Empty
        } else {
            TransactionsUiState.Content(buildData(filtered))
        }
        updateState { copy(uiState = uiState) }
    }

    private fun buildData(rows: List<TransactionListItem>): TransactionsData {
        val currency = rows.firstOrNull { it.currency.isNotBlank() }?.currency ?: GBP
        val moneyIn = sumMinorUnits(rows.filter { it.isCredit }.map { it.amount })
        val moneyOut = sumMinorUnits(rows.filterNot { it.isCredit }.map { it.amount })

        var index = 0
        val groups = rows
            .groupBy { it.datePrefix() }
            .entries
            .sortedByDescending { it.key }
            .map { (prefix, groupRows) ->
                TransactionGroup(
                    dateLabel = dateLabel(prefix),
                    rows = groupRows.map { it.toRowUi(index++) },
                )
            }

        return TransactionsData(
            groups = groups,
            moneyInLabel = "+" + formatMinorUnits(moneyIn, currency),
            moneyOutLabel = "-" + formatMinorUnits(moneyOut, currency),
        )
    }

    private fun TransactionListItem.toRowUi(index: Int): TransactionRowUi = TransactionRowUi(
        key = transactionId.ifBlank { "txn-$index" },
        transactionId = transactionId,
        description = description,
        amountLabel = formatSignedMoney(amount, currency, isCredit),
        isCredit = isCredit,
        category = category,
        isPending = isPending,
    )

    companion object {
        const val ACCOUNT_ID_ARG = "accountId"
    }
}

private fun TransactionFilter.matches(isCredit: Boolean): Boolean = when (this) {
    TransactionFilter.ALL -> true
    TransactionFilter.MONEY_IN -> isCredit
    TransactionFilter.MONEY_OUT -> !isCredit
}

private fun TransactionListItem.matchesQuery(query: String): Boolean =
    query.isBlank() || description.contains(query.trim(), ignoreCase = true)

private fun TransactionListItem.matchesDateRange(from: LocalDate?, to: LocalDate?): Boolean {
    if (from == null || to == null) return true
    val day = datePrefix()
    // ISO `yyyy-MM-dd` prefixes compare chronologically as plain strings.
    return day.isNotEmpty() && day >= from.toString() && day <= to.toString()
}

/** The `yyyy-MM-dd` prefix of the OBIE `BookingDateTime`, or empty when absent. */
private fun TransactionListItem.datePrefix(): String = bookingDateTime.take(ISO_DATE_LENGTH)

/** Formats an ISO date prefix as `SUN 28 JUN 2026`, falling back to the raw prefix when unparseable. */
private fun dateLabel(isoDatePrefix: String): String {
    val date = runCatching { LocalDate.parse(isoDatePrefix) }.getOrNull() ?: return isoDatePrefix
    val dow = date.dayOfWeek.name.take(3).uppercase()
    val month = date.month.name.take(3).uppercase()
    return "$dow ${date.day} $month ${date.year}"
}

private fun NetworkError.toErrorKind(): TransactionsErrorKind = when (this) {
    is NetworkError.Client.Unauthorized -> TransactionsErrorKind.SESSION_EXPIRED
    is NetworkError.Client.Forbidden -> TransactionsErrorKind.CONSENT_WITHDRAWN
    is NetworkError.Client.RateLimited -> TransactionsErrorKind.RATE_LIMITED
    else -> TransactionsErrorKind.NETWORK
}
