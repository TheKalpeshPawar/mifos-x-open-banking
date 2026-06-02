/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.home.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.mifosx.openbanking.core.data.accounts.AccountsRepository
import org.mifosx.openbanking.core.data.transactions.TransactionsRepository
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.core.model.obp.Transaction
import template.core.base.store.screen.ScreenState
import template.core.base.store.screen.combineContent
import template.core.base.store.screen.dataOrNull
import template.core.base.store.screen.emptyIfContent

/**
 * Home Dashboard ViewModel. Reads the offline-first [AccountsRepository.accountsStream]
 * (Store5 cache-then-network, `/my/accounts` — cross-bank) and, once the account list
 * loads, fetches the primary account's recent transactions from THAT account's own bank
 * (see [TransactionsRepository] — bank-aware, not a single global config bank).
 *
 * Exposes one [ScreenState] so the screen renders loading / content / empty / error /
 * no-network / unauthenticated uniformly. `Empty` means the user has no accounts at all.
 *
 * Greeting name is derived from the primary account's owner (the app's [SessionManager]
 * holds no user name — it manages session lifetime only, app-shell-wide).
 */
class HomeViewModel(
    accountsRepository: AccountsRepository,
    private val transactionsRepository: TransactionsRepository,
) : ViewModel() {

    private val stream = accountsRepository.accountsStream(viewModelScope)
    private val transactionsFlow = MutableStateFlow(RecentTransactionsState())

    val uiState: StateFlow<ScreenState<HomeContent>> = stream.state
        .combineContent(transactionsFlow) { accounts, recent, _ ->
            val primary = accounts.primaryAccount()
            HomeContent(
                greetingName = primary?.owners?.firstOrNull()?.displayName.orEmpty(),
                primaryAccount = primary,
                totalAccountCount = accounts.size,
                totalBalance = accounts.sumOf { it.balance.amount.toDoubleOrNull() ?: 0.0 },
                currency = primary?.balance?.currency.orEmpty(),
                recentTransactions = recent.items,
                transactionsLoading = recent.loading,
            )
        }
        .emptyIfContent { it.totalAccountCount == 0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ScreenState.Loading,
        )

    init {
        // When the account list resolves, load the primary account's recent transactions
        // from its OWN bank — cross-bank safe (regression-guarded in TransactionsRepositoryTest).
        stream.state
            .onEach { state ->
                val primary = state.dataOrNull?.primaryAccount() ?: return@onEach
                val bankId = primary.bankId
                val accountId = primary.accountIdOrId
                if (bankId.isNotBlank() && accountId.isNotBlank()) {
                    transactionsFlow.update { it.copy(loading = true) }
                    transactionsRepository.listTransactions(bankId, accountId, limit = RECENT_LIMIT)
                        .onSuccess { txns ->
                            transactionsFlow.value = RecentTransactionsState(
                                loading = false,
                                items = txns.take(RECENT_LIMIT),
                            )
                        }
                        .onFailure { transactionsFlow.update { it.copy(loading = false) } }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onRetry() = stream.retry()

    fun onRefresh() = stream.refresh()

    companion object {
        const val RECENT_LIMIT = 5
    }
}

/** Hero-card account: first checking-type account, else the first account. */
internal fun List<Account>.primaryAccount(): Account? =
    firstOrNull { it.typeOrProduct.contains("checking", ignoreCase = true) } ?: firstOrNull()

/** Loaded content for the Home Dashboard. */
@Immutable
data class HomeContent(
    val greetingName: String,
    val primaryAccount: Account?,
    val totalAccountCount: Int,
    val totalBalance: Double,
    val currency: String,
    val recentTransactions: List<Transaction>,
    val transactionsLoading: Boolean = false,
)

/** Recent-transactions sub-state, fetched independently of the accounts stream. */
@Immutable
data class RecentTransactionsState(
    val loading: Boolean = false,
    val items: List<Transaction> = emptyList(),
)
