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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.core.model.banking.AccountBalance
import org.mifosx.openbanking.core.model.banking.BankAccount
import org.mifosx.openbanking.core.model.banking.TransactionItem
import org.mifosx.openbanking.feature.home.FakeAccountsRepository
import org.mifosx.openbanking.feature.home.FakeBalancesRepository
import org.mifosx.openbanking.feature.home.FakeTransactionsRepository
import org.mifosx.openbanking.feature.home.FakeUserDataRepository
import template.core.base.common.screen.DataFreshness
import template.core.base.common.screen.ScreenState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class HomeViewModelTest {

    private val accountsRepo = FakeAccountsRepository()
    private val balancesRepo = FakeBalancesRepository()
    private val transactionsRepo = FakeTransactionsRepository()
    private val userDataRepo = FakeUserDataRepository()

    private fun createViewModel(): HomeViewModel {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        return HomeViewModel(accountsRepo, balancesRepo, transactionsRepo, userDataRepo)
    }

    private fun account(id: String, subType: String = "CurrentAccount") =
        BankAccount(id, "Nickname $id", subType, "GBP", "400515", "12345678")

    private fun balance(id: String) = AccountBalance(id, "GBP", "2900.00", "2847.63")

    private fun debit(id: String, date: String) =
        TransactionItem(id, "acc-1", "Merchant $id", date, "10.00", "GBP", isCredit = false)

    @Test
    fun `content combines accounts balance and transactions into HomeData`() = runTest {
        val vm = createViewModel()
        accountsRepo.emissions.value =
            ScreenState.Content(listOf(account("acc-1"), account("acc-2")), DataFreshness.FRESH)
        balancesRepo.emissions.value = ScreenState.Content(balance("acc-1"), DataFreshness.FRESH)
        transactionsRepo.emissions.value = ScreenState.Content(
            List(8) { debit("t$it", "2026-06-1${it}T10:00:00Z") },
            DataFreshness.FRESH,
        )

        val state = vm.stateFlow.first { it.uiState is ScreenState.Content }
        val data = (state.uiState as ScreenState.Content).data

        assertEquals("acc-1", data.selectedAccountId)
        assertEquals(2, data.accounts.size)
        assertEquals("Nickname acc-1", data.accountNickname)
        assertEquals("£2,900.00", data.balanceLabel)
        assertEquals("40-05-15  12345678", data.accountNumberLabel)
        assertEquals(5, data.recentTransactions.size)
    }

    @Test
    fun `opening and dismissing the account selector toggles its visibility`() = runTest {
        val vm = createViewModel()

        vm.trySendAction(HomeAction.OpenAccountSelector)
        advanceUntilIdle()
        assertEquals(true, vm.stateFlow.value.isAccountSelectorVisible)

        vm.trySendAction(HomeAction.DismissAccountSelector)
        advanceUntilIdle()
        assertEquals(false, vm.stateFlow.value.isAccountSelectorVisible)
    }

    @Test
    fun `selecting an account closes the selector and persists the choice`() = runTest {
        val vm = createViewModel()
        vm.trySendAction(HomeAction.OpenAccountSelector)
        advanceUntilIdle()

        vm.trySendAction(HomeAction.SelectAccount("acc-2"))
        advanceUntilIdle()

        assertEquals(false, vm.stateFlow.value.isAccountSelectorVisible)
        assertEquals("acc-2", userDataRepo.userData.first().selectedAccountId)
    }

    @Test
    fun `empty account list yields Empty state`() = runTest {
        val vm = createViewModel()
        accountsRepo.emissions.value = ScreenState.Content(emptyList(), DataFreshness.FRESH)

        val state = vm.stateFlow.first { it.uiState is ScreenState.Empty }
        assertIs<ScreenState.Empty>(state.uiState)
    }

    @Test
    fun `accounts error propagates to ui state`() = runTest {
        val vm = createViewModel()
        accountsRepo.emissions.value = ScreenState.Error(RuntimeException("boom"))

        val state = vm.stateFlow.first { it.uiState is ScreenState.Error }
        assertIs<ScreenState.Error>(state.uiState)
    }

    @Test
    fun `SelectAccount persists the chosen account id`() = runTest {
        val vm = createViewModel()

        vm.trySendAction(HomeAction.SelectAccount("acc-2"))
        advanceUntilIdle()

        assertEquals("acc-2", userDataRepo.userData.value.selectedAccountId)
    }

    @Test
    fun `RetryLoad refreshes every repository`() = runTest {
        val vm = createViewModel()

        vm.trySendAction(HomeAction.RetryLoad)
        advanceUntilIdle()

        assertEquals(1, accountsRepo.refreshCount)
        assertEquals(1, balancesRepo.refreshCount)
        assertEquals(1, transactionsRepo.refreshCount)
    }
}
