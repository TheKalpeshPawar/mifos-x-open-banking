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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.mifosx.openbanking.core.data.accounts.AccountsRepository
import org.mifosx.openbanking.core.model.obp.Account
import template.core.base.store.screen.ScreenState
import template.core.base.store.screen.emptyIfContent
import template.core.base.store.screen.mapContent

/**
 * Home Dashboard ViewModel. Reads the offline-first [AccountsRepository.accountsStream]
 * (Store5 cache-then-network, `/my/accounts` — cross-bank) and derives the dashboard hero
 * card + portfolio totals from the resolved account list.
 *
 * Exposes one [ScreenState] so the screen renders loading / content / empty / error /
 * no-network / unauthenticated uniformly. `Empty` means the user has no accounts at all.
 *
 * Greeting name is derived from the primary account's owner (the app's [SessionManager]
 * holds no user name — it manages session lifetime only, app-shell-wide).
 */
class HomeViewModel(
    accountsRepository: AccountsRepository,
) : ViewModel() {

    private val stream = accountsRepository.accountsStream(viewModelScope)

    val uiState: StateFlow<ScreenState<HomeContent>> = stream.state
        .mapContent { accounts, _ ->
            val primary = accounts.primaryAccount()
            HomeContent(
                greetingName = primary?.owners?.firstOrNull()?.displayName.orEmpty(),
                primaryAccount = primary,
                totalAccountCount = accounts.size,
            )
        }
        .emptyIfContent { it.totalAccountCount == 0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ScreenState.Loading,
        )

    fun onRetry() = stream.retry()

    fun onRefresh() = stream.refresh()
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
)
