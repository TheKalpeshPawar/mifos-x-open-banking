/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accounts.ui

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.mifosx.openbanking.core.data.banking.AccountsOverviewRepository
import org.mifosx.openbanking.core.model.banking.AccountWithBalance
import template.core.base.common.screen.ScreenState
import template.core.base.common.screen.combineContent
import template.core.base.ui.viewmodel.BaseViewModel

private const val SUBTYPE_CURRENT = "CurrentAccount"
private const val SUBTYPE_SAVINGS = "Savings"
private const val SUBTYPE_CREDIT_CARD = "CreditCard"
private const val SUBTYPE_GLOBAL_MONEY = "GlobalMoney"
private const val SUBTYPE_GLOBAL_WALLET = "GlobalWallet"

/**
 * Normalises a raw account-type value to one of the five UI subtypes. Accepts the OBIE
 * `AccountTypeCode` codes case-insensitively (`CACC`, `SVGS`, `CCRD`) plus the legacy subtype tokens
 * so the screen classifies correctly whichever vocabulary the bank uses.
 */
private fun String.canonicalTypeCode(): String = when (lowercase()) {
    "currentaccount", "current", "cacc" -> SUBTYPE_CURRENT
    "savings", "svgs" -> SUBTYPE_SAVINGS
    "creditcard", "credit", "card", "ccrd" -> SUBTYPE_CREDIT_CARD
    "globalmoney" -> SUBTYPE_GLOBAL_MONEY
    "globalwallet" -> SUBTYPE_GLOBAL_WALLET
    else -> this
}

/** The account category, resolved from the OBIE `AccountTypeCode`; drives the row icon and label. */
enum class AccountUiType {
    CURRENT,
    SAVINGS,
    CREDIT,
    GLOBAL_MONEY,
    GLOBAL_WALLET,
    OTHER, ;

    companion object {
        fun fromTypeCode(accountTypeCode: String): AccountUiType = when (accountTypeCode.canonicalTypeCode()) {
            SUBTYPE_CURRENT -> CURRENT
            SUBTYPE_SAVINGS -> SAVINGS
            SUBTYPE_CREDIT_CARD -> CREDIT
            SUBTYPE_GLOBAL_MONEY -> GLOBAL_MONEY
            SUBTYPE_GLOBAL_WALLET -> GLOBAL_WALLET
            else -> OTHER
        }
    }
}

/**
 * The client-side account-type filter backing the chip row.
 *
 * There is deliberately no Global filter. HSBC UK Personal AIS v4.0 dropped `AccountSubType` from
 * `OBAccount6` entirely and its `AccountTypeCode` enum has no wallet code, so a Global Money account
 * arrives as `CACC` — indistinguishable here from an ordinary current account, and already listed
 * under [CURRENT]. A `GLOBAL` entry matching the `GlobalMoney`/`GlobalWallet` subtypes could never
 * match anything and rendered a permanently empty list. The only runtime signal for Global Money is
 * the free-text `Description`, which `HsbcProductType.resolve` reads and the account-detail screen
 * now displays.
 */
enum class AccountFilter(private val subtypes: Set<String>?) {
    ALL(null),
    CURRENT(setOf(SUBTYPE_CURRENT)),
    SAVINGS(setOf(SUBTYPE_SAVINGS)),
    CREDIT(setOf(SUBTYPE_CREDIT_CARD)),
    ;

    fun matches(accountTypeCode: String): Boolean = subtypes == null || accountTypeCode.canonicalTypeCode() in subtypes
}

/** Accounts payload: the filtered accounts (each with its resolved balance) and the active filter. */
data class AccountsData(
    val rows: List<AccountWithBalance>,
    val activeFilter: AccountFilter,
)

data class AccountsState(
    val uiState: ScreenState<AccountsData> = ScreenState.Loading,
)

sealed interface AccountsAction {
    /** Client-side chip filter by account type — no API round-trip. */
    data class FilterAccounts(val filter: AccountFilter) : AccountsAction

    /** Re-fetch accounts and re-resolve every balance from the network. */
    data object RetryLoad : AccountsAction
}

/**
 * Drives the accounts overview. Combines the offline-first [AccountsOverviewRepository] stream with a
 * client-side filter into one [ScreenState], mapping [AccountWithBalance] rows into a display-ready
 * [AccountsData]: per-account identifier and balance are formatted here. Navigation is handled by the
 * screen, so no events are emitted.
 */
class AccountsViewModel(
    private val accountsRepository: AccountsOverviewRepository,
) : BaseViewModel<AccountsState, Nothing, AccountsAction>(initialState = AccountsState()) {

    private val filter = MutableStateFlow(AccountFilter.ALL)

    init {
        viewModelScope.launch {
            accountsRepository.overviewState(viewModelScope)
                .combineContent(filter) {
                        accounts, active, _ ->
                    buildData(accounts, active)
                }.collect { screenState ->
                    updateState {
                        copy(uiState = screenState)
                    }
                }
        }
    }

    override fun handleAction(action: AccountsAction) {
        when (action) {
            is AccountsAction.FilterAccounts -> filter.value = action.filter
            AccountsAction.RetryLoad -> accountsRepository.refresh()
        }
    }

    private fun buildData(accounts: List<AccountWithBalance>, active: AccountFilter): AccountsData =
        AccountsData(
            rows = accounts.filter { active.matches(it.account.accountTypeCode) },
            activeFilter = active,
        )
}
