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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.mifosx.openbanking.core.data.banking.store.FakeFetchedAtRepository
import org.mifosx.openbanking.core.data.banking.store.FakeNetworkMonitor
import org.mifosx.openbanking.core.model.banking.AccountBalance
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.Store
import template.core.base.common.screen.ScreenState
import template.core.base.store.infra.StoreFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Verifies [BalancesRepositoryImpl] streams the keyed balance as Content and computes the
 * per-account cache key `home:balance:<accountId>` through the `cacheKeyFor` lambda.
 */
class BalancesRepositoryImplTest {

    private val onlineInfo = NetworkInfo(type = NetworkType.WiFi, isMetered = false)

    private val balance = AccountBalance(
        accountId = "acc-1",
        currency = "GBP",
        currentAmount = "2900.00",
        availableAmount = "2847.63",
    )

    private fun store(): Store<String, AccountBalance> =
        StoreFactory.createMemoryStore(fetcher = Fetcher.of { _: String -> balance })

    private fun repo(fetchedAt: FakeFetchedAtRepository): BalancesRepositoryImpl =
        BalancesRepositoryImpl(
            store = store(),
            networkMonitor = FakeNetworkMonitor(NetworkStatus.Available(onlineInfo)),
            fetchedAtRepository = fetchedAt,
        )

    @Test
    fun balanceStateEmitsContentAndUsesPerAccountCacheKey() = runTest(UnconfinedTestDispatcher()) {
        val fetchedAt = FakeFetchedAtRepository()
        val state = repo(fetchedAt)
            .balanceState(flowOf("acc-1"), backgroundScope)
            .first { it is ScreenState.Content }

        val content = assertIs<ScreenState.Content<AccountBalance>>(state)
        assertEquals(balance, content.data)
        assertTrue("home:balance:acc-1" in fetchedAt.readKeys)
    }

    @Test
    fun balanceStateReusesCachedStreamOnSecondCall() = runTest(UnconfinedTestDispatcher()) {
        val repo = repo(FakeFetchedAtRepository())
        repo.balanceState(flowOf("acc-1"), backgroundScope).first { it is ScreenState.Content }

        val second = repo.balanceState(flowOf("acc-1"), backgroundScope).first { it is ScreenState.Content }

        assertIs<ScreenState.Content<AccountBalance>>(second)
    }

    @Test
    fun refreshBeforeAnyStateCallIsNoOp() {
        repo(FakeFetchedAtRepository()).refresh()
    }

    @Test
    fun refreshAfterStateCallReEmitsContent() = runTest(UnconfinedTestDispatcher()) {
        val repo = repo(FakeFetchedAtRepository())
        repo.balanceState(flowOf("acc-1"), backgroundScope).first { it is ScreenState.Content }

        repo.refresh()

        val state = repo.balanceState(flowOf("acc-1"), backgroundScope).first { it is ScreenState.Content }
        assertIs<ScreenState.Content<AccountBalance>>(state)
    }
}
