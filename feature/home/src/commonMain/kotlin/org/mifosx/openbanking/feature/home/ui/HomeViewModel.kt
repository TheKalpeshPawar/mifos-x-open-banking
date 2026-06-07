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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.mifosx.openbanking.core.data.accounts.AccountsRepository
import org.mifosx.openbanking.core.data.accounts.PfmAccountsService
import org.mifosx.openbanking.core.datastore.UserPreferencesRepository
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.core.model.pfm.PfmScope
import org.mifosx.openbanking.core.model.pfm.pfmScope
import template.core.base.store.screen.ScreenState

/**
 * Home Dashboard ViewModel. Reads the offline-first [AccountsRepository.accountsStream]
 * (Store5 cache-then-network, `/my/accounts` — cross-bank) combined with the persisted
 * default-account preference, and derives the dashboard hero card + portfolio totals.
 * The hero card shows the user's default account (set via the account picker sheet);
 * with no persisted choice it falls back to checking-first.
 *
 * Exposes one [ScreenState] so the screen renders loading / content / empty / error /
 * no-network / unauthenticated uniformly. `Empty` means the user has no accounts at all.
 *
 * Greeting name is derived from the primary account's owner (the app's [SessionManager]
 * holds no user name — it manages session lifetime only, app-shell-wide).
 */
class HomeViewModel(
    accountsRepository: AccountsRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    pfmAccountsService: PfmAccountsService,
) : ViewModel() {

    private val stream = accountsRepository.accountsStream(viewModelScope)

    private val hasBusinessAccountsState = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            hasBusinessAccountsState.value = pfmAccountsService.classifiedAccounts()
                .getOrElse { emptyList() }
                .any { it.pfmScope == PfmScope.BUSINESS }
        }
    }

    val uiState: StateFlow<ScreenState<HomeContent>> =
        combine(
            stream.state,
            userPreferencesRepository.observeDefaultAccountId,
            hasBusinessAccountsState,
        ) { state, defaultId, hasBusiness ->
            project(state, defaultId, hasBusiness)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ScreenState.Loading,
        )

    fun onRetry() = stream.retry()

    fun onRefresh() = stream.refresh()

    /** Persists the default account chosen on the hero-card picker sheet. */
    fun onDefaultAccountSelected(account: Account) {
        viewModelScope.launch {
            userPreferencesRepository.setDefaultAccountId(account.accountIdOrId)
        }
    }

    private fun project(
        state: ScreenState<List<Account>>,
        defaultId: String,
        hasBusiness: Boolean,
    ): ScreenState<HomeContent> =
        when (state) {
            is ScreenState.Content -> {
                val accounts = state.data
                if (accounts.isEmpty()) {
                    ScreenState.Empty
                } else {
                    val primary = accounts.primaryAccount(defaultId)
                    ScreenState.Content(
                        data = HomeContent(
                            greetingName = primary?.owners?.firstOrNull()?.displayName.orEmpty(),
                            primaryAccount = primary,
                            accounts = accounts,
                            totalAccountCount = accounts.size,
                            hasBusinessAccounts = hasBusiness,
                        ),
                        freshness = state.freshness,
                        fetchedAt = state.fetchedAt,
                    )
                }
            }
            is ScreenState.Loading -> state
            is ScreenState.Empty -> state
            is ScreenState.NoNetwork -> state
            is ScreenState.Unauthenticated -> state
            is ScreenState.Error -> state
        }
}

/** Hero-card account: the persisted default when it still exists, else checking-first, else first. */
internal fun List<Account>.primaryAccount(defaultAccountId: String = ""): Account? =
    firstOrNull { defaultAccountId.isNotBlank() && it.accountIdOrId == defaultAccountId }
        ?: firstOrNull { it.typeOrProduct.contains("checking", ignoreCase = true) }
        ?: firstOrNull()

/** Loaded content for the Home Dashboard. */
@Immutable
data class HomeContent(
    val greetingName: String,
    val primaryAccount: Account?,
    val accounts: List<Account> = emptyList(),
    val totalAccountCount: Int,
    val hasBusinessAccounts: Boolean = false,
)
