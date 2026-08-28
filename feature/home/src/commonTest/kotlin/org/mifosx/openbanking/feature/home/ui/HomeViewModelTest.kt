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
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import org.mifosx.openbanking.core.model.banking.AccountWithBalance
import org.mifosx.openbanking.feature.home.FakeAccountsOverviewRepository
import org.mifosx.openbanking.feature.home.FixedClock
import org.mifosx.openbanking.feature.home.HomeFixtures
import template.core.base.common.screen.DataFreshness
import template.core.base.common.screen.ScreenState
import template.core.base.common.screen.dataOrNull
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class HomeViewModelTest {

    private val repo = FakeAccountsOverviewRepository()

    private fun viewModel(clock: Clock = FixedClock(MORNING)): HomeViewModel {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        return HomeViewModel(accountsRepository = repo, clock = clock, timeZone = TimeZone.UTC)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun content(): ScreenState.Content<List<AccountWithBalance>> =
        ScreenState.Content(HomeFixtures.all(), DataFreshness.FRESH)

    @Test
    fun contentPartitionsCardsAwayFromAccounts() = runTest {
        val vm = viewModel()
        repo.emissions.value = content()

        val data = vm.stateFlow.first().uiState.dataOrNull
        assertEquals(3, data?.accounts?.size)
        assertEquals(1, data?.cards?.size)
    }

    @Test
    fun aCardAccountLandsInCardsAndNeverInAccounts() = runTest {
        val vm = viewModel()
        repo.emissions.value = content()

        val data = vm.stateFlow.first().uiState.dataOrNull
        assertEquals(HomeFixtures.CARD_ID, data?.cards?.single()?.account?.accountId)
        assertTrue(data?.accounts.orEmpty().none { it.account.accountId == HomeFixtures.CARD_ID })
    }

    @Test
    fun aGlobalMoneyWalletLandsInAccountsDespiteReportingCacc() = runTest {
        val vm = viewModel()
        repo.emissions.value = content()

        val data = vm.stateFlow.first().uiState.dataOrNull
        assertTrue(data?.accounts.orEmpty().any { it.account.accountId == HomeFixtures.GLOBAL_MONEY_ID })
    }

    @Test
    fun anAccountWithoutABalanceStillAppears() = runTest {
        val vm = viewModel()
        val noBalance = HomeFixtures.current().copy(balance = null)
        repo.emissions.value = ScreenState.Content(listOf(noBalance), DataFreshness.FRESH)

        val data = vm.stateFlow.first().uiState.dataOrNull
        assertEquals(1, data?.accounts?.size)
    }

    @Test
    fun beforeNoonTheGreetingIsMorning() = runTest {
        val vm = viewModel(FixedClock(MORNING))
        repo.emissions.value = content()

        assertEquals(Greeting.Morning, vm.stateFlow.first().uiState.dataOrNull?.greeting)
    }

    @Test
    fun betweenNoonAndSixTheGreetingIsAfternoon() = runTest {
        val vm = viewModel(FixedClock(AFTERNOON))
        repo.emissions.value = content()

        assertEquals(Greeting.Afternoon, vm.stateFlow.first().uiState.dataOrNull?.greeting)
    }

    @Test
    fun fromSixTheGreetingIsEvening() = runTest {
        val vm = viewModel(FixedClock(EVENING))
        repo.emissions.value = content()

        assertEquals(Greeting.Evening, vm.stateFlow.first().uiState.dataOrNull?.greeting)
    }

    @Test
    fun loadingIsPassedThrough() = runTest {
        val vm = viewModel()
        repo.emissions.value = ScreenState.Loading

        assertIs<ScreenState.Loading>(vm.stateFlow.first().uiState)
    }

    @Test
    fun emptyIsPassedThrough() = runTest {
        val vm = viewModel()
        repo.emissions.value = ScreenState.Empty

        assertIs<ScreenState.Empty>(vm.stateFlow.first().uiState)
    }

    @Test
    fun errorIsPassedThrough() = runTest {
        val vm = viewModel()
        repo.emissions.value = ScreenState.Error(IllegalStateException("boom"))

        assertIs<ScreenState.Error>(vm.stateFlow.first().uiState)
    }

    @Test
    fun retryLoadRefreshesTheOverview() = runTest {
        val vm = viewModel()
        repo.emissions.value = content()

        vm.trySendAction(HomeAction.RetryLoad)

        assertEquals(1, repo.refreshCount)
    }

    private companion object {
        val MORNING: Instant = Instant.parse("2026-08-27T09:00:00Z")
        val AFTERNOON: Instant = Instant.parse("2026-08-27T14:00:00Z")
        val EVENING: Instant = Instant.parse("2026-08-27T20:00:00Z")
    }
}
