/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.pfm.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mifosx.openbanking.core.data.accounts.PfmAccountsService
import org.mifosx.openbanking.core.data.pfm.BudgetsRepository
import org.mifosx.openbanking.core.datastore.UserPreferencesRepository
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.core.model.pfm.PfmScope
import org.mifosx.openbanking.core.model.pfm.pfmScope
import template.core.base.store.screen.DataFreshness
import template.core.base.store.screen.ScreenState

/**
 * PFM settings: base-currency selection for the unified personal dashboard. Options are
 * the distinct currencies across the user's personal accounts; the current selection is
 * the persisted `pfm_base_currency` field, falling back to the default account's currency.
 * Saving appends a new personal-data-field row (newest row wins on read).
 */
class PfmSettingsViewModel(
    private val pfmAccountsService: PfmAccountsService,
    private val budgetsRepository: BudgetsRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val state = MutableStateFlow<ScreenState<PfmSettingsContent>>(ScreenState.Loading)
    val uiState: StateFlow<ScreenState<PfmSettingsContent>> = state.asStateFlow()

    private val noticeState = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = noticeState.asStateFlow()

    init {
        load()
    }

    fun onRetry() = load()

    fun onNoticeConsumed() {
        noticeState.value = null
    }

    fun onCurrencySelected(code: String) {
        viewModelScope.launch {
            budgetsRepository.saveBaseCurrency(code)
                .onSuccess {
                    state.value = (state.value as? ScreenState.Content)?.let { current ->
                        ScreenState.Content(
                            data = current.data.copy(selected = code),
                            freshness = DataFreshness.FRESH,
                        )
                    } ?: state.value
                    noticeState.value = "Base currency set to $code."
                }
                .onFailure { noticeState.value = "Could not save. Please try again." }
        }
    }

    private fun load() {
        state.value = ScreenState.Loading
        viewModelScope.launch {
            val personal = pfmAccountsService.classifiedAccounts()
                .getOrElse {
                    state.value = ScreenState.Error(it)
                    return@launch
                }
                .filter { it.pfmScope == PfmScope.PERSONAL }
            val options = personal.map { it.balance.currency }.filter { it.isNotBlank() }.distinct().sorted()
            val selected = budgetsRepository.baseCurrency().getOrNull()
                ?: resolveDefaultAccount(personal)?.balance?.currency.orEmpty()
            state.value = ScreenState.Content(
                data = PfmSettingsContent(
                    options = options,
                    selected = selected.ifBlank { options.firstOrNull().orEmpty() },
                ),
                freshness = DataFreshness.FRESH,
            )
        }
    }

    /** Persisted default account → checking-type → first, over personal accounts only. */
    private fun resolveDefaultAccount(accounts: List<Account>): Account? {
        val defaultId = userPreferencesRepository.userData.value.defaultAccountId
        return accounts.firstOrNull { it.accountIdOrId == defaultId }
            ?: accounts.firstOrNull { it.typeOrProduct.contains("checking", ignoreCase = true) }
            ?: accounts.firstOrNull()
    }
}

/** Loaded content for the PFM settings screen. */
@Immutable
data class PfmSettingsContent(
    val options: List<String>,
    val selected: String,
)
