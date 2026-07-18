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
import org.mifosx.openbanking.core.common.formatAccountIdentifier
import org.mifosx.openbanking.core.common.formatMinorUnits
import org.mifosx.openbanking.core.common.formatMoney
import org.mifosx.openbanking.core.common.sumMinorUnits
import org.mifosx.openbanking.core.data.banking.AccountsOverviewRepository
import org.mifosx.openbanking.core.data.callback.ConsentSession
import org.mifosx.openbanking.core.model.banking.AccountWithBalance
import template.core.base.common.screen.ScreenState
import template.core.base.common.screen.combineContent
import template.core.base.ui.viewmodel.BaseViewModel
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val GBP = "GBP"
private const val SUBTYPE_CURRENT = "CurrentAccount"
private const val SUBTYPE_SAVINGS = "Savings"
private const val SUBTYPE_CREDIT_CARD = "CreditCard"
private const val SUBTYPE_GLOBAL_MONEY = "GlobalMoney"
private const val SUBTYPE_GLOBAL_WALLET = "GlobalWallet"
private const val CONSENT_EXPIRY_THRESHOLD_DAYS = 30
private const val BALANCE_UNAVAILABLE = "—"

/** The account category, resolved from the OBIE `AccountSubType`; drives the row icon and label. */
enum class AccountUiType {
    CURRENT,
    SAVINGS,
    CREDIT,
    GLOBAL_MONEY,
    GLOBAL_WALLET,
    OTHER,
    ;

    companion object {
        fun fromSubtype(subType: String): AccountUiType = when (subType) {
            SUBTYPE_CURRENT -> CURRENT
            SUBTYPE_SAVINGS -> SAVINGS
            SUBTYPE_CREDIT_CARD -> CREDIT
            SUBTYPE_GLOBAL_MONEY -> GLOBAL_MONEY
            SUBTYPE_GLOBAL_WALLET -> GLOBAL_WALLET
            else -> OTHER
        }
    }
}

/** The client-side account-type filter backing the chip row. */
enum class AccountFilter(private val subtypes: Set<String>?) {
    ALL(null),
    CURRENT(setOf(SUBTYPE_CURRENT)),
    SAVINGS(setOf(SUBTYPE_SAVINGS)),
    CREDIT(setOf(SUBTYPE_CREDIT_CARD)),
    GLOBAL(setOf(SUBTYPE_GLOBAL_MONEY, SUBTYPE_GLOBAL_WALLET)),
    ;

    fun matches(subType: String): Boolean = subtypes == null || subType in subtypes
}

/** A display-ready account row. All money and identifiers are pre-formatted; the card renders strings. */
data class AccountRowUi(
    val id: String,
    val type: AccountUiType,
    val nickname: String,
    val identifier: String,
    val balanceLabel: String,
    val isBalanceOwed: Boolean,
)

/** Display-ready accounts payload: filtered rows, the net GBP total, and the consent-expiry banner state. */
data class AccountsData(
    val rows: List<AccountRowUi>,
    val totalBalanceLabel: String,
    val accountCount: Int,
    val activeFilter: AccountFilter,
    val isConsentExpiring: Boolean,
    val consentDaysRemaining: Int,
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
 * [AccountsData]: per-account identifier and balance are formatted here, the net GBP total sums the
 * available balances of non-credit GBP accounts, and the consent-expiry banner is derived from the
 * stored consent expiry. Navigation is handled by the screen, so no events are emitted.
 */
@OptIn(ExperimentalTime::class)
class AccountsViewModel(
    private val accountsRepository: AccountsOverviewRepository,
    private val consentSession: ConsentSession,
) : BaseViewModel<AccountsState, Nothing, AccountsAction>(initialState = AccountsState()) {

    private val filter = MutableStateFlow(AccountFilter.ALL)

    init {
        viewModelScope.launch {
            accountsRepository.overviewState(viewModelScope)
                .combineContent(filter) { accounts, active, _ -> buildData(accounts, active) }
                .collect { screenState -> updateState { copy(uiState = screenState) } }
        }
    }

    override fun handleAction(action: AccountsAction) {
        when (action) {
            is AccountsAction.FilterAccounts -> filter.value = action.filter
            AccountsAction.RetryLoad -> accountsRepository.refresh()
        }
    }

    private fun buildData(accounts: List<AccountWithBalance>, active: AccountFilter): AccountsData {
        val days = consentDaysRemaining()
        return AccountsData(
            rows = accounts.filter { active.matches(it.account.accountSubType) }.map { it.toRowUi() },
            totalBalanceLabel = netGbpBalanceLabel(accounts),
            accountCount = accounts.size,
            activeFilter = active,
            isConsentExpiring = days < CONSENT_EXPIRY_THRESHOLD_DAYS,
            consentDaysRemaining = days,
        )
    }

    private fun consentDaysRemaining(): Int =
        consentSession.consentExpiration()
            ?.let { (it - Clock.System.now()).inWholeDays.toInt() }
            ?: Int.MAX_VALUE

    private fun netGbpBalanceLabel(accounts: List<AccountWithBalance>): String {
        val amounts = accounts
            .filter { it.account.currency == GBP && it.account.accountSubType != SUBTYPE_CREDIT_CARD }
            .mapNotNull { it.balance?.availableAmount }
        return formatMinorUnits(sumMinorUnits(amounts), GBP)
    }

    private fun AccountWithBalance.toRowUi(): AccountRowUi = AccountRowUi(
        id = account.accountId,
        type = AccountUiType.fromSubtype(account.accountSubType),
        nickname = account.nickname,
        identifier = formatAccountIdentifier(
            subType = account.accountSubType,
            rawIdentification = account.rawIdentification,
            sortCode = account.sortCode,
            accountNumber = account.accountNumber,
        ),
        balanceLabel = balance?.let { formatMoney(it.availableAmount, it.currency) } ?: BALANCE_UNAVAILABLE,
        isBalanceOwed = account.accountSubType == SUBTYPE_CREDIT_CARD,
    )
}
