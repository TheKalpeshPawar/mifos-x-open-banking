/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.home

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.mifosx.openbanking.core.data.banking.AccountsRepository
import org.mifosx.openbanking.core.data.banking.BalancesRepository
import org.mifosx.openbanking.core.data.banking.TransactionsRepository
import org.mifosx.openbanking.core.data.user.UserDataRepository
import org.mifosx.openbanking.core.model.banking.AccountBalance
import org.mifosx.openbanking.core.model.banking.BankAccount
import org.mifosx.openbanking.core.model.banking.TransactionItem
import org.mifosx.openbanking.core.model.banking.TransactionsPage
import org.mifosx.openbanking.core.model.user.DarkThemeConfig
import org.mifosx.openbanking.core.model.user.LanguageConfig
import org.mifosx.openbanking.core.model.user.ThemeBrand
import org.mifosx.openbanking.core.model.user.UserData
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

class FakeAccountsRepository : AccountsRepository {
    val emissions = MutableStateFlow<ScreenState<List<BankAccount>>>(ScreenState.Loading)
    var refreshCount = 0
        private set

    override fun accountsState(scope: CoroutineScope): Flow<ScreenState<List<BankAccount>>> = emissions
    override fun refresh() {
        refreshCount++
    }
}

class FakeBalancesRepository : BalancesRepository {
    val emissions = MutableStateFlow<ScreenState<AccountBalance>>(ScreenState.Loading)
    var refreshCount = 0
        private set

    override fun balanceState(accountIdFlow: Flow<String>, scope: CoroutineScope): Flow<ScreenState<AccountBalance>> =
        emissions

    override fun refresh() {
        refreshCount++
    }
}

class FakeTransactionsRepository : TransactionsRepository {
    val emissions = MutableStateFlow<ScreenState<List<TransactionItem>>>(ScreenState.Loading)
    var refreshCount = 0
        private set

    override fun transactionsState(
        accountIdFlow: Flow<String>,
        scope: CoroutineScope,
    ): Flow<ScreenState<List<TransactionItem>>> = emissions

    override fun refresh() {
        refreshCount++
    }

    // Home renders the store-backed stream above and never pages, so the cursor pager
    // returns an empty terminal page.
    override suspend fun firstPage(accountId: String): NetworkResult<TransactionsPage, NetworkError> =
        NetworkResult.Success(TransactionsPage(emptyList(), nextLink = null, totalPages = null))

    override suspend fun nextPage(nextLink: String): NetworkResult<TransactionsPage, NetworkError> =
        NetworkResult.Success(TransactionsPage(emptyList(), nextLink = null, totalPages = null))
}

/**
 * Minimal [UserDataRepository] fake: only [userData] and [setSelectedAccountId] carry behaviour the
 * home ViewModel relies on; the remaining preference setters are inert.
 */
class FakeUserDataRepository : UserDataRepository {
    private val state = MutableStateFlow(UserData.DEFAULT)

    override val userData: StateFlow<UserData> = state.asStateFlow()
    override val passcode: String get() = state.value.passcode
    override val observeLanguage: Flow<LanguageConfig> = state.map { it.appLanguage }
    override val observeDarkThemeConfig: Flow<DarkThemeConfig> = state.map { it.darkThemeConfig }
    override val observeDynamicColorPreference: Flow<Boolean> = state.map { it.useDynamicColor }
    override val observeScreenCapturePreference: Flow<Boolean> = state.map { it.enableScreenCapture }

    override suspend fun setLanguage(language: LanguageConfig) = state.update { it.copy(appLanguage = language) }
    override suspend fun setThemeBrand(themeBrand: ThemeBrand) = state.update { it.copy(themeBrand = themeBrand) }
    override suspend fun setDarkThemeConfig(darkThemeConfig: DarkThemeConfig) =
        state.update { it.copy(darkThemeConfig = darkThemeConfig) }

    override suspend fun setDynamicColorPreference(useDynamicColor: Boolean) =
        state.update { it.copy(useDynamicColor = useDynamicColor) }

    override suspend fun setIsAuthenticated(isAuthenticated: Boolean) =
        state.update { it.copy(isAuthenticated = isAuthenticated) }

    override suspend fun setIsUnlocked(isUnlocked: Boolean) = state.update { it.copy(isUnlocked = isUnlocked) }
    override suspend fun setIsPasscodeEnabled(isPasscodeEnabled: Boolean) =
        state.update { it.copy(isPasscodeEnabled = isPasscodeEnabled) }

    override suspend fun setIsBiometricsEnabled(isBiometricsEnabled: Boolean) =
        state.update { it.copy(isBiometricsEnabled = isBiometricsEnabled) }

    override suspend fun setSelectedAccountId(accountId: String) =
        state.update { it.copy(selectedAccountId = accountId) }

    override suspend fun clearUserData() {
        state.value = UserData.DEFAULT
    }
}
