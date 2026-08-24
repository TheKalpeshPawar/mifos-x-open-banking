/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking.impl

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkInfo
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkStatus
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.mifosx.openbanking.core.common.AccountScheme
import org.mifosx.openbanking.core.data.banking.store.FakeFetchedAtRepository
import org.mifosx.openbanking.core.data.banking.store.FakeNetworkMonitor
import org.mifosx.openbanking.core.model.banking.AccountBalance
import org.mifosx.openbanking.core.model.banking.AccountWithBalance
import org.mifosx.openbanking.core.model.banking.BankAccount
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.Store
import template.core.base.common.screen.ScreenState
import template.core.base.store.infra.StoreFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Verifies [AccountsOverviewRepositoryImpl] fans the accounts stream out over per-account balances:
 * merging each balance into Content, tolerating a single failed balance fetch, collapsing an empty
 * account set to Empty, passing an accounts error through, and re-emitting on refresh.
 */
class AccountsOverviewRepositoryImplTest {

    private val onlineInfo = NetworkInfo(type = NetworkType.WiFi, isMetered = false)

    private fun account(id: String) = BankAccount(
        accountId = id,
        accountHolderName = id,
        accountTypeCode = "CACC",
        currency = "GBP",
        identification = "40051512345678",
        scheme = AccountScheme.SortCode,
    )

    private fun balance(id: String) = AccountBalance(
        accountId = id,
        currency = "GBP",
        currentAmount = "100.00",
        availableAmount = "90.00",
    )

    private fun accountsStore(accounts: () -> List<BankAccount>): Store<String, List<BankAccount>> =
        StoreFactory.createMemoryStore(fetcher = Fetcher.of { _: String -> accounts() })

    private fun balancesStore(resolve: (String) -> AccountBalance): Store<String, AccountBalance> =
        StoreFactory.createMemoryStore(fetcher = Fetcher.of { id: String -> resolve(id) })

    private fun repo(
        accounts: () -> List<BankAccount>,
        balances: (String) -> AccountBalance = ::balance,
    ) = AccountsOverviewRepositoryImpl(
        accountsStore = accountsStore(accounts),
        balancesStore = balancesStore(balances),
        networkMonitor = FakeNetworkMonitor(NetworkStatus.Available(onlineInfo)),
        fetchedAtRepository = FakeFetchedAtRepository(),
    )

    @Test
    fun overviewMergesEachAccountWithItsBalance() = runTest(UnconfinedTestDispatcher()) {
        val repo = repo(accounts = { listOf(account("acc-1"), account("acc-2")) })

        val content = assertIs<ScreenState.Content<List<AccountWithBalance>>>(
            repo.overviewState(backgroundScope).first { it is ScreenState.Content },
        )

        assertEquals(2, content.data.size)
        assertEquals("acc-1", content.data[0].account.accountId)
        assertEquals(balance("acc-1"), content.data[0].balance)
        assertEquals(balance("acc-2"), content.data[1].balance)
    }

    @Test
    fun overviewLeavesAFailedBalanceNullAndKeepsTheRest() = runTest(UnconfinedTestDispatcher()) {
        val repo = repo(
            accounts = { listOf(account("ok"), account("bad")) },
            balances = { id -> if (id == "bad") error("balance unavailable") else balance(id) },
        )

        val content = assertIs<ScreenState.Content<List<AccountWithBalance>>>(
            repo.overviewState(backgroundScope).first { it is ScreenState.Content },
        )

        val byId = content.data.associateBy { it.account.accountId }
        assertEquals(balance("ok"), byId.getValue("ok").balance)
        assertNull(byId.getValue("bad").balance, "one failed balance must not fail the whole screen")
    }

    @Test
    fun overviewEmitsEmptyForNoAccounts() = runTest(UnconfinedTestDispatcher()) {
        val repo = repo(accounts = { emptyList() })

        assertIs<ScreenState.Empty>(repo.overviewState(backgroundScope).first { it is ScreenState.Empty })
    }

    @Test
    fun overviewSurfacesAnAccountsError() = runTest(UnconfinedTestDispatcher()) {
        val repo = repo(accounts = { error("accounts down") })

        assertIs<ScreenState.Error>(repo.overviewState(backgroundScope).first { it is ScreenState.Error })
    }

    @Test
    fun refreshBeforeAnyStateCallIsNoOp() {
        repo(accounts = { emptyList() }).refresh()
    }

    @Test
    fun refreshReEmitsContentAfterAStateCall() = runTest(UnconfinedTestDispatcher()) {
        val repo = repo(accounts = { listOf(account("acc-1")) })
        repo.overviewState(backgroundScope).first { it is ScreenState.Content }

        repo.refresh()

        val content = assertIs<ScreenState.Content<List<AccountWithBalance>>>(
            repo.overviewState(backgroundScope).first { it is ScreenState.Content },
        )
        assertEquals(balance("acc-1"), content.data.single().balance)
    }
}
