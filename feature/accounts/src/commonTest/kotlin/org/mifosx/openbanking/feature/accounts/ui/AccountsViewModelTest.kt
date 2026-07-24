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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.core.model.banking.AccountBalance
import org.mifosx.openbanking.core.model.banking.AccountWithBalance
import org.mifosx.openbanking.core.model.banking.BankAccount
import org.mifosx.openbanking.feature.accounts.FakeAccountsOverviewRepository
import template.core.base.common.screen.DataFreshness
import template.core.base.common.screen.ScreenState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AccountsViewModelTest {

    private val repo = FakeAccountsOverviewRepository()

    private fun createViewModel(): AccountsViewModel {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        return AccountsViewModel(repo)
    }

    private fun accountWith(
        id: String,
        available: String,
        subType: String = "CurrentAccount",
        currency: String = "GBP",
    ) = AccountWithBalance(
        account = BankAccount(id, "Nickname $id", subType, currency, "400515", "12345678", "40051512345678"),
        balance = AccountBalance(id, currency, "0", available),
    )

    private fun sampleAccounts() = listOf(
        accountWith("acc-current", "2847.63", "CurrentAccount"),
        accountWith("acc-savings", "12450.00", "Savings"),
        accountWith("acc-credit", "342.18", "CreditCard"),
        accountWith("acc-global", "500.00", "GlobalMoney"),
        accountWith("acc-wallet", "250.00", "GlobalWallet", currency = "USD"),
    )

    private fun content(state: ScreenState<*>) = state as ScreenState.Content

    @Test
    fun `content maps every account into a display row`() = runTest {
        val vm = createViewModel()
        repo.emissions.value = ScreenState.Content(sampleAccounts(), DataFreshness.FRESH)

        val state = vm.stateFlow.first { it.uiState is ScreenState.Content }
        val data = content(state.uiState).data as AccountsData

        assertEquals(5, data.rows.size)
    }

    @Test
    fun `empty account list yields Empty state`() = runTest {
        val vm = createViewModel()
        repo.emissions.value = ScreenState.Empty

        val state = vm.stateFlow.first { it.uiState is ScreenState.Empty }
        assertIs<ScreenState.Empty>(state.uiState)
    }

    @Test
    fun `accounts error propagates to ui state`() = runTest {
        val vm = createViewModel()
        repo.emissions.value = ScreenState.Error(RuntimeException("boom"))

        val state = vm.stateFlow.first { it.uiState is ScreenState.Error }
        assertIs<ScreenState.Error>(state.uiState)
    }

    @Test
    fun `filter narrows the rows to the chosen type`() = runTest {
        val vm = createViewModel()
        repo.emissions.value = ScreenState.Content(sampleAccounts(), DataFreshness.FRESH)
        vm.stateFlow.first { it.uiState is ScreenState.Content }

        vm.trySendAction(AccountsAction.FilterAccounts(AccountFilter.CREDIT))
        advanceUntilIdle()

        val data = content(vm.stateFlow.value.uiState).data as AccountsData
        assertEquals(1, data.rows.size)
        assertEquals("acc-credit", data.rows.first().id)
        assertEquals(AccountFilter.CREDIT, data.activeFilter)
    }

    @Test
    fun `credit card row renders as balance owed`() = runTest {
        val vm = createViewModel()
        repo.emissions.value = ScreenState.Content(sampleAccounts(), DataFreshness.FRESH)

        val state = vm.stateFlow.first { it.uiState is ScreenState.Content }
        val creditRow = (content(state.uiState).data as AccountsData).rows.first { it.id == "acc-credit" }
        assertTrue(creditRow.isBalanceOwed)
        assertEquals(AccountUiType.CREDIT, creditRow.type)
    }

    @Test
    fun `RetryLoad refreshes the repository`() = runTest {
        val vm = createViewModel()

        vm.trySendAction(AccountsAction.RetryLoad)
        advanceUntilIdle()

        assertEquals(1, repo.refreshCount)
    }
}
