/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.home.ui

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import org.mifosx.openbanking.core.common.formatMoney
import org.mifosx.openbanking.core.common.formatShortMonthDay
import org.mifosx.openbanking.core.common.formatSignedMoney
import org.mifosx.openbanking.core.common.formatSortCode
import org.mifosx.openbanking.core.data.banking.AccountsRepository
import org.mifosx.openbanking.core.data.banking.BalancesRepository
import org.mifosx.openbanking.core.data.banking.TransactionsRepository
import org.mifosx.openbanking.core.data.user.UserDataRepository
import org.mifosx.openbanking.core.model.banking.AccountBalance
import org.mifosx.openbanking.core.model.banking.BankAccount
import org.mifosx.openbanking.core.model.banking.TransactionItem
import template.core.base.common.screen.ScreenState
import template.core.base.common.screen.combineScreenStates
import template.core.base.common.screen.dataOrNull
import template.core.base.ui.viewmodel.BaseViewModel

/**
 * An account chip in the switcher: stable id plus the display nickname. [accountSubType],
 * [accountNumber] and [rawIdentification] let the chip fall back to a "type ·· last 4" label via
 * `accountDisplayName` when the bank supplied no nickname.
 */
data class AccountChipUi(
    val id: String,
    val nickname: String,
    val accountSubType: String = "",
    val accountNumber: String = "",
    val rawIdentification: String = "",
    val accountHolderName: String = "",
)

/** A recent-transaction row, pre-formatted for display. */
data class TransactionRowUi(
    val id: String,
    val description: String,
    val dateLabel: String,
    val amountLabel: String,
    val isCredit: Boolean,
)

/**
 * Display-ready home dashboard payload. All money, dates, and identifiers are already formatted —
 * the screen only renders these strings.
 */
data class HomeData(
    val accounts: List<AccountChipUi>,
    val selectedAccountId: String,
    val accountTypeLabel: String,
    val accountHolderName: String,
    /** Selected account's raw fields, so the hero card can fall back to a "type ·· last 4" label. */
    val accountSubType: String = "",
    val accountNumber: String = "",
    val rawIdentification: String = "",
    val balanceLabel: String,
    val availableAmountLabel: String,
    val accountNumberLabel: String,
    val recentTransactions: List<TransactionRowUi>,
)

data class HomeState(
    val uiState: ScreenState<HomeData> = ScreenState.Loading,
    /**
     * Whether the account selector sheet is open. Sheet visibility is presentation state rather than
     * loaded data, so it sits here beside [uiState] instead of inside [HomeData].
     */
    val isAccountSelectorVisible: Boolean = false,
)

sealed interface HomeAction {
    /** The PSU picked an account in the selector sheet — persist the choice and close the sheet. */
    data class SelectAccount(val accountId: String) : HomeAction

    /** The PSU tapped the hero balance card, which opens the account selector. */
    data object OpenAccountSelector : HomeAction

    /** The selector sheet was dismissed without a choice. */
    data object DismissAccountSelector : HomeAction

    /** Retry the whole load after an error or offline state. */
    data object RetryLoad : HomeAction
}

/**
 * Drives the home dashboard. Combines three offline-first repository streams into one
 * [ScreenState] via [combineScreenStates]: the account list is fetched once, and the selected
 * account (resolved from DataStore, falling back to the first account) keys the balance and
 * transaction streams. Domain models are mapped to a display-ready [HomeData] here so the screen
 * stays free of formatting logic. Navigation is handled by the screen, so no events are emitted.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val accountsRepository: AccountsRepository,
    private val balancesRepository: BalancesRepository,
    private val transactionsRepository: TransactionsRepository,
    private val userDataRepository: UserDataRepository,
) : BaseViewModel<HomeState, Nothing, HomeAction>(initialState = HomeState()) {

    /** The three streams this screen renders, scoped to this view model. */
    private val accountsStream = accountsRepository.accountsStream(viewModelScope)

    private val accounts: Flow<ScreenState<List<BankAccount>>> =
        accountsStream.state
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), replay = 1)

    private val selectedAccountId: Flow<String> = combine(
        accounts.map { it.dataOrNull.orEmpty() },
        userDataRepository.userData.map { it.selectedAccountId },
    ) { available, persisted ->
        when {
            available.any { it.accountId == persisted } -> persisted
            else -> available.firstOrNull()?.accountId.orEmpty()
        }
    }.distinctUntilChanged().filter { it.isNotBlank() }

    private val balanceStream = balancesRepository.balanceStream(selectedAccountId, viewModelScope)

    private val transactionsStream =
        transactionsRepository.transactionsStream(selectedAccountId, viewModelScope)

    private val balance: Flow<ScreenState<AccountBalance>> = balanceStream.state

    private val transactions: Flow<ScreenState<List<TransactionItem>>> = transactionsStream.state

    init {
        viewModelScope.launch {
            accounts
                .flatMapLatest { accountsState -> resolve(accountsState) }
                .collect { screenState -> updateState { copy(uiState = screenState) } }
        }
    }

    override fun handleAction(action: HomeAction) {
        when (action) {
            is HomeAction.SelectAccount -> {
                updateState { copy(isAccountSelectorVisible = false) }
                viewModelScope.launch { userDataRepository.setSelectedAccountId(action.accountId) }
            }

            HomeAction.OpenAccountSelector -> updateState { copy(isAccountSelectorVisible = true) }

            HomeAction.DismissAccountSelector -> updateState { copy(isAccountSelectorVisible = false) }

            HomeAction.RetryLoad -> retry()
        }
    }

    private fun resolve(accountsState: ScreenState<List<BankAccount>>): Flow<ScreenState<HomeData>> =
        when (accountsState) {
            is ScreenState.Content ->
                if (accountsState.data.isEmpty()) {
                    flowOf(ScreenState.Empty)
                } else {
                    combineScreenStates(flowOf(accountsState), balance, transactions) { list, accountBalance, txns ->
                        buildHomeData(list, accountBalance, txns)
                    }
                }

            is ScreenState.Loading -> flowOf(ScreenState.Loading)
            is ScreenState.Empty -> flowOf(ScreenState.Empty)
            is ScreenState.Error -> flowOf(ScreenState.Error(accountsState.error, accountsState.isNetworkError))
            is ScreenState.NoNetwork -> flowOf(ScreenState.NoNetwork(accountsState.isCaptivePortal))
            is ScreenState.Unauthenticated -> flowOf(ScreenState.Unauthenticated)
        }

    private fun buildHomeData(
        accounts: List<BankAccount>,
        balance: AccountBalance,
        transactions: List<TransactionItem>,
    ): HomeData {
        val selectedId = balance.accountId
        val selected = accounts.firstOrNull { it.accountId == selectedId }
        return HomeData(
            accounts = accounts.map {
                AccountChipUi(
                    id = it.accountId,
                    nickname = it.nickname,
                    accountHolderName = it.accountHolderName,
                    accountSubType = it.accountSubType,
                    accountNumber = it.accountNumber,
                    rawIdentification = it.rawIdentification,
                )
            },
            selectedAccountId = selectedId,
            accountTypeLabel = selected?.accountSubType.orEmpty().uppercase(),
            accountHolderName = selected?.accountHolderName.orEmpty(),
            accountSubType = selected?.accountSubType.orEmpty(),
            accountNumber = selected?.accountNumber.orEmpty(),
            rawIdentification = selected?.rawIdentification.orEmpty(),
            balanceLabel = formatMoney(balance.currentAmount, balance.currency),
            availableAmountLabel = formatMoney(balance.availableAmount, balance.currency),
            accountNumberLabel = selected?.let { "${formatSortCode(it.sortCode)}  ${it.accountNumber}" }.orEmpty(),
            recentTransactions = transactions.take(RECENT_TRANSACTIONS_LIMIT).map { it.toRowUi() },
        )
    }

    private fun TransactionItem.toRowUi(): TransactionRowUi = TransactionRowUi(
        id = transactionId,
        description = description,
        dateLabel = formatShortMonthDay(bookingDateTime),
        amountLabel = formatSignedMoney(amount, currency, isCredit),
        isCredit = isCredit,
    )

    private fun retry() {
        accountsStream.refresh()
        balanceStream.refresh()
        transactionsStream.refresh()
    }

    private companion object {
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
        const val RECENT_TRANSACTIONS_LIMIT = 5
    }
}
