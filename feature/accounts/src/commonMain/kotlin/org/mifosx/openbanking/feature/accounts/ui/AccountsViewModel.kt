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
 * search query — filtering re-derives [AccountsContent] without a new network call.
 *
 * Exposes a single [ScreenState] so the screen renders loading / content / empty / error /
 * no-network / unauthenticated uniformly. `Empty` means the user has no accounts at all; a
 * search that matches nothing stays `Content` (search bar operable) with an empty list.
 */
class AccountsViewModel(
    accountsRepository: AccountsRepository,
) : ViewModel() {

    private val stream = accountsRepository.accountsStream(viewModelScope)

    private val queryFlow = MutableStateFlow("")
    val searchQuery: StateFlow<String> = queryFlow.asStateFlow()

    val uiState: StateFlow<ScreenState<AccountsContent>> = stream.state
        .combineContent(queryFlow) { accounts, query, _ ->
            val matches = accounts.filter { it.matchesQuery(query) }
            AccountsContent(
                filteredAccounts = matches,
                query = query,
                accountCount = accounts.size,
            )
        }
        .emptyIfContent { it.accountCount == 0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ScreenState.Loading,
        )

    fun onSearchQueryChanged(query: String) = queryFlow.update { query }

    fun onRetry() = stream.retry()

    fun onRefresh() = stream.refresh()
}

/** Loaded content for the My Accounts screen, including the client-side search result. */
@Immutable
data class AccountsContent(
    val filteredAccounts: List<Account>,
    val query: String,
    val accountCount: Int,
)

/**
 * Client-side free-text match. A blank query matches everything; otherwise the trimmed
 * query is matched case-insensitively against the account name/label, number, IBAN, the
 * resolved display identifier, and the account id. OBP `account_type` is a free-form
 * string (no fixed checking/savings/business enum), so search beats a category filter.
 */
fun Account.matchesQuery(query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true
    return listOf(label, number, iban, displayIdentifier, accountIdOrId)
        .any { it.contains(q, ignoreCase = true) }
}
