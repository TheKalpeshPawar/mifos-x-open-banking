/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.accounts.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.mifosx.openbanking.core.data.accounts.AccountsRepository
import org.mifosx.openbanking.core.model.obp.Account
import template.core.base.store.screen.ScreenState
import template.core.base.store.screen.combineContent
import template.core.base.store.screen.emptyIfContent

/**
 * My Accounts ViewModel. Reads the offline-first [AccountsRepository.accountsStream]
 * (Store5 cache-then-network, auto-refresh on reconnect) and fuses it with a client-side
 * [AccountTypeFilter] — filtering re-derives [AccountsContent] without a new network call.
 *
 * Exposes a single [ScreenState] so the screen renders loading / content / empty / error /
 * no-network / unauthenticated uniformly. `Empty` means the user has no accounts at all; a
 * filter that matches nothing stays `Content` (tabs remain operable) with an empty list.
 */
class AccountsViewModel(
    accountsRepository: AccountsRepository,
) : ViewModel() {

    private val stream = accountsRepository.accountsStream(viewModelScope)

    private val activeFilterFlow = MutableStateFlow(AccountTypeFilter.ALL)
    val activeFilter: StateFlow<AccountTypeFilter> = activeFilterFlow.asStateFlow()

    val uiState: StateFlow<ScreenState<AccountsContent>> = stream.state
        .combineContent(activeFilterFlow) { accounts, filter, _ ->
            AccountsContent(
                filteredAccounts = accounts.filter(filter::matches),
                activeFilter = filter,
                accountCount = accounts.size,
                totalBalance = accounts.sumOf { it.balance.amount.toDoubleOrNull() ?: 0.0 },
                currency = accounts.firstOrNull()?.balance?.currency.orEmpty(),
            )
        }
        .emptyIfContent { it.accountCount == 0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ScreenState.Loading,
        )

    fun onFilterChanged(filter: AccountTypeFilter) = activeFilterFlow.update { filter }

    fun onRetry() = stream.retry()

    fun onRefresh() = stream.refresh()
}

/** Loaded content for the My Accounts screen, including the client-side filter result. */
@Immutable
data class AccountsContent(
    val filteredAccounts: List<Account>,
    val activeFilter: AccountTypeFilter,
    val accountCount: Int,
    val totalBalance: Double,
    val currency: String,
)

/** Client-side account-type filter for the My Accounts tab row. */
enum class AccountTypeFilter(val label: String) {
    ALL("ALL"),
    CHECKING("CHECKING"),
    SAVINGS("SAVINGS"),
    BUSINESS("BUSINESS"),
    ;

    /** Best-effort match against the OBP `account_type` string. ALL matches everything. */
    fun matches(account: Account): Boolean =
        this == ALL || account.accountType.contains(name, ignoreCase = true)
}
