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
import org.mifosx.openbanking.core.common.AccountScheme
import org.mifosx.openbanking.core.model.banking.AccountBalance
import org.mifosx.openbanking.core.model.banking.AccountWithBalance
import org.mifosx.openbanking.core.model.banking.BankAccount
import org.mifosx.openbanking.feature.accounts.FakeAccountsOverviewRepository
import template.core.base.common.screen.DataFreshness
import template.core.base.common.screen.ScreenState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AccountsViewModelTest {

    private val repo = FakeAccountsOverviewRepository()

    private fun createViewModel(): AccountsViewModel {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        return AccountsViewModel(repo)
    }

    private fun accountWith(
        id: String,
        available: String,
        typeCode: String = "CACC",
        currency: String = "GBP",
    ) = AccountWithBalance(
        account = BankAccount(
            accountId = id,
            accountHolderName = "Nickname $id",
            accountTypeCode = typeCode,
            currency = currency,
            identification = "40051512345678",
            scheme = AccountScheme.SortCode,
        ),
        balance = AccountBalance(id, currency, "0", available),
    )

    private fun sampleAccounts() = listOf(
        accountWith("acc-current", "2847.63", "CACC"),
        accountWith("acc-savings", "12450.00", "SVGS"),
        accountWith("acc-credit", "342.18", "CARD"),
        accountWith("acc-global", "500.00", "CACC"),
        accountWith("acc-wallet", "250.00", "CACC", currency = "USD"),
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
        assertEquals("acc-credit", data.rows.first().account.accountId)
        assertEquals(AccountFilter.CREDIT, data.activeFilter)
    }

    @Test
    fun `there are four filters and none of them is Global`() {
        assertEquals(
            listOf(AccountFilter.ALL, AccountFilter.CURRENT, AccountFilter.SAVINGS, AccountFilter.CREDIT),
            AccountFilter.entries,
        )
    }

    /**
     * The reason a Global Money account needs no filter of its own. HSBC UK Personal AIS v4.0 dropped
     * AccountSubType from the accounts payload and has no wallet type code, so a Global Money wallet
     * reports CACC exactly as an ordinary current account does, and the Current filter already covers it.
     */
    @Test
    fun `an account reporting the CACC type code is matched by the Current filter`() = runTest {
        val vm = createViewModel()
        repo.emissions.value = ScreenState.Content(
            listOf(accountWith("acc-global-money", "500.00", typeCode = "CACC")),
            DataFreshness.FRESH,
        )
        vm.stateFlow.first { it.uiState is ScreenState.Content }

        vm.trySendAction(AccountsAction.FilterAccounts(AccountFilter.CURRENT))
        advanceUntilIdle()

        val data = content(vm.stateFlow.value.uiState).data as AccountsData
        assertEquals(1, data.rows.size)
        assertEquals("acc-global-money", data.rows.first().account.accountId)
    }

    @Test
    fun `RetryLoad refreshes the repository`() = runTest {
        val vm = createViewModel()

        vm.trySendAction(AccountsAction.RetryLoad)
        advanceUntilIdle()

        assertEquals(1, repo.refreshCount)
    }
}
