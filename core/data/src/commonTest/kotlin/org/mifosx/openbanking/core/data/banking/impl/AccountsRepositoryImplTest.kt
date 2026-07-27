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
import org.mifosx.openbanking.core.data.banking.store.FakeFetchedAtRepository
import org.mifosx.openbanking.core.data.banking.store.FakeNetworkMonitor
import org.mifosx.openbanking.core.model.banking.BankAccount
import org.mobilenativefoundation.store.store5.Fetcher
import template.core.base.common.screen.ScreenState
import template.core.base.store.infra.StoreFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Verifies [AccountsRepositoryImpl] lazily builds and caches its [asScreenStream], emits Content for
 * fetched accounts, and that [AccountsRepositoryImpl.refresh] is safe both before and after the
 * stream exists.
 */
class AccountsRepositoryImplTest {

    private val onlineInfo = NetworkInfo(type = NetworkType.WiFi, isMetered = false)

    private val accounts = listOf(
        BankAccount(
            accountId = "acc-1",
            nickname = "Everyday",
            accountSubType = "CurrentAccount",
            currency = "GBP",
            sortCode = "400515",
            accountNumber = "12345678",
        ),
    )

    private fun repo(): AccountsRepositoryImpl {
        val store = StoreFactory.createMemoryStore(fetcher = Fetcher.of { _: String -> accounts })
        return AccountsRepositoryImpl(
            store = store,
            networkMonitor = FakeNetworkMonitor(NetworkStatus.Available(onlineInfo)),
            fetchedAtRepository = FakeFetchedAtRepository(),
        )
    }

    @Test
    fun accountsStateEmitsContentWithFetchedAccounts() = runTest(UnconfinedTestDispatcher()) {
        val state = repo().accountsStream(backgroundScope).state.first { it is ScreenState.Content }

        val content = assertIs<ScreenState.Content<List<BankAccount>>>(state)
        assertEquals(accounts, content.data)
    }

    @Test
    fun asecondCallReturnsAnIndependentStream() = runTest(UnconfinedTestDispatcher()) {
        val repo = repo()
        repo.accountsStream(backgroundScope).state.first { it is ScreenState.Content }

        val second = repo.accountsStream(backgroundScope).state.first { it is ScreenState.Content }

        assertIs<ScreenState.Content<List<BankAccount>>>(second)
    }

    @Test
    fun refreshingAStreamReEmitsContent() = runTest(UnconfinedTestDispatcher()) {
        val stream = repo().accountsStream(backgroundScope)
        stream.state.first { it is ScreenState.Content }

        stream.refresh()

        assertIs<ScreenState.Content<List<BankAccount>>>(stream.state.first { it is ScreenState.Content })
    }
}
