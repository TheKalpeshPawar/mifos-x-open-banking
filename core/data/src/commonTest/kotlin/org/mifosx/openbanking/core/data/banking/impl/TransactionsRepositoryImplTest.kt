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
import org.mifosx.openbanking.core.model.banking.TransactionItem
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.Store
import template.core.base.common.screen.ScreenState
import template.core.base.store.infra.StoreFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Verifies [TransactionsRepositoryImpl] streams the keyed transaction list as Content and computes
 * the per-account cache key `home:transactions:<accountId>` through the `cacheKeyFor` lambda.
 */
class TransactionsRepositoryImplTest {

    private val onlineInfo = NetworkInfo(type = NetworkType.WiFi, isMetered = false)

    private val transactions = listOf(
        TransactionItem(
            transactionId = "t1",
            accountId = "acc-1",
            description = "TESCO STORES",
            bookingDateTime = "2026-06-27T10:00:00Z",
            amount = "42.17",
            currency = "GBP",
            isCredit = false,
        ),
    )

    private fun store(): Store<String, List<TransactionItem>> =
        StoreFactory.createMemoryStore(fetcher = Fetcher.of { _: String -> transactions })

    private fun repo(fetchedAt: FakeFetchedAtRepository): TransactionsRepositoryImpl =
        TransactionsRepositoryImpl(
            store = store(),
            networkMonitor = FakeNetworkMonitor(NetworkStatus.Available(onlineInfo)),
            fetchedAtRepository = fetchedAt,
        )

    @Test
    fun transactionsStateEmitsContentAndUsesPerAccountCacheKey() = runTest(UnconfinedTestDispatcher()) {
        val fetchedAt = FakeFetchedAtRepository()
        val state = repo(fetchedAt)
            .transactionsState(flowOf("acc-1"), backgroundScope)
            .first { it is ScreenState.Content }

        val content = assertIs<ScreenState.Content<List<TransactionItem>>>(state)
        assertEquals(transactions, content.data)
        assertTrue("home:transactions:acc-1" in fetchedAt.readKeys)
    }

    @Test
    fun transactionsStateReusesCachedStreamOnSecondCall() = runTest(UnconfinedTestDispatcher()) {
        val repo = repo(FakeFetchedAtRepository())
        repo.transactionsState(flowOf("acc-1"), backgroundScope).first { it is ScreenState.Content }

        val second = repo.transactionsState(flowOf("acc-1"), backgroundScope).first { it is ScreenState.Content }

        assertIs<ScreenState.Content<List<TransactionItem>>>(second)
    }

    @Test
    fun refreshBeforeAnyStateCallIsNoOp() {
        repo(FakeFetchedAtRepository()).refresh()
    }

    @Test
    fun refreshAfterStateCallReEmitsContent() = runTest(UnconfinedTestDispatcher()) {
        val repo = repo(FakeFetchedAtRepository())
        repo.transactionsState(flowOf("acc-1"), backgroundScope).first { it is ScreenState.Content }

        repo.refresh()

        val state = repo.transactionsState(flowOf("acc-1"), backgroundScope).first { it is ScreenState.Content }
        assertIs<ScreenState.Content<List<TransactionItem>>>(state)
    }
}
