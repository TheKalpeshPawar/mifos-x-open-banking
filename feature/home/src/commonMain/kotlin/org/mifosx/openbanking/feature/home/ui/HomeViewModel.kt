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
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.mifosx.openbanking.core.data.banking.AccountsOverviewRepository
import org.mifosx.openbanking.core.model.banking.AccountWithBalance
import org.mifosx.openbanking.core.model.banking.BankAccount
import org.mifosx.openbanking.core.model.hsbcProduct.HsbcProductType
import template.core.base.common.screen.ScreenState
import template.core.base.common.screen.mapContent
import template.core.base.ui.viewmodel.BaseViewModel
import kotlin.time.Clock

/** The time-of-day greeting shown in the header. */
enum class Greeting {
    Morning,
    Afternoon,
    Evening,
}

/**
 * Everything the home dashboard renders: the customer's accounts, their cards, and the greeting.
 *
 * @param accounts Every non-card account, in the order the bank returned them.
 * @param cards Every credit-card account.
 * @param greeting The time-of-day greeting.
 */
data class HomeData(
    val accounts: List<AccountWithBalance>,
    val cards: List<AccountWithBalance>,
    val greeting: Greeting,
)

data class HomeState(
    val uiState: ScreenState<HomeData> = ScreenState.Loading,
)

sealed interface HomeAction {
    /** Retry the load after an error or offline state. */
    data object RetryLoad : HomeAction
}

/**
 * Drives the home dashboard from the accounts overview, which already carries a balance per account.
 * The list is split into cards and everything else.
 */
class HomeViewModel(
    private val accountsRepository: AccountsOverviewRepository,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : BaseViewModel<HomeState, Nothing, HomeAction>(initialState = HomeState()) {

    init {
        viewModelScope.launch {
            accountsRepository.overviewState(viewModelScope)
                .mapContent { accounts, _ ->
                    buildData(accounts)
                }.collect { screenState ->
                    updateState {
                        copy(uiState = screenState)
                    }
                }
        }
    }

    override fun handleAction(action: HomeAction) {
        when (action) {
            HomeAction.RetryLoad -> accountsRepository.refresh()
        }
    }

    private fun buildData(accounts: List<AccountWithBalance>): HomeData {
        val (cards, rest) = accounts.partition { it.account.isCreditCard() }
        return HomeData(
            accounts = rest,
            cards = cards,
            greeting = currentGreeting(),
        )
    }

    private fun currentGreeting(): Greeting {
        val hour = clock.now().toLocalDateTime(timeZone).hour
        return when {
            hour < AFTERNOON_FROM -> Greeting.Morning
            hour < EVENING_FROM -> Greeting.Afternoon
            else -> Greeting.Evening
        }
    }

    private companion object {
        const val AFTERNOON_FROM = 12
        const val EVENING_FROM = 18
    }
}

private fun BankAccount.isCreditCard(): Boolean = HsbcProductType.resolve(
    accountTypeCode = accountTypeCode,
    description = description,
) == HsbcProductType.CreditCard
