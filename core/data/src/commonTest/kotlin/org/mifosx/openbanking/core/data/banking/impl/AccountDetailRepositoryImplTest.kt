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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.mifosx.openbanking.core.common.AccountScheme
import org.mifosx.openbanking.core.data.banking.store.FakeFetchedAtRepository
import org.mifosx.openbanking.core.data.banking.store.FakeNetworkMonitor
import org.mifosx.openbanking.core.model.banking.AccountBalanceLine
import org.mifosx.openbanking.core.model.banking.AccountDetail
import org.mifosx.openbanking.core.model.banking.AccountDetailWithBalances
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.Store
import template.core.base.common.screen.ScreenState
import template.core.base.common.screen.combineScreenStates
import template.core.base.store.infra.StoreFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Verifies [AccountDetailRepositoryImpl] opens per-account streams over both stores, computes the
 * cache keys `accountDetail:detail:<id>` and `accountDetail:balances:<id>`, and that the streams a
 * caller receives always carry the account it asked for.
 *
 * Both fixture stores key their fetcher off the requested id, so a stream bound to the wrong
 * account produces visibly wrong data rather than passing unnoticed.
 */
class AccountDetailRepositoryImplTest {

    private val onlineInfo = NetworkInfo(type = NetworkType.WiFi, isMetered = false)

    private val detail = AccountDetail(
        accountId = "acc-1",
        accountHolderName = "Everyday Current",
        accountTypeCode = "CACC",
        currency = "GBP",
        identification = "40051512345678",
        scheme = AccountScheme.SortCode,
        servicerIdentification = "MIDLGB2105V",
        statusUpdateDateTime = "2026-06-28T18:30:00Z",
    )

    private val balancesByAccount = mapOf(
        "acc-1" to listOf(
            AccountBalanceLine(
                type = "InterimAvailable",
                amount = "2847.63",
                currency = "GBP",
                dateTime = "2026-06-28T18:30:00Z",
            ),
        ),
        "acc-2" to listOf(
            AccountBalanceLine(
                type = "InterimBooked",
                amount = "10.00",
                currency = "GBP",
                dateTime = "2026-06-28T18:30:00Z",
            ),
        ),
    )

    private val balances = balancesByAccount.getValue("acc-1")

    private fun detailStore(
        fetch: (String) -> AccountDetail = { key -> detail.copy(accountId = key) },
    ): Store<String, AccountDetail> =
        StoreFactory.createMemoryStore(fetcher = Fetcher.of { key: String -> fetch(key) })

    private fun balancesStore(
        fetch: (String) -> List<AccountBalanceLine> = { key -> balancesByAccount[key].orEmpty() },
    ): Store<String, List<AccountBalanceLine>> =
        StoreFactory.createMemoryStore(fetcher = Fetcher.of { key: String -> fetch(key) })

    private fun repo(
        fetchedAt: FakeFetchedAtRepository = FakeFetchedAtRepository(),
        detailStore: Store<String, AccountDetail> = detailStore(),
        balanceLinesStore: Store<String, List<AccountBalanceLine>> = balancesStore(),
    ): AccountDetailRepositoryImpl = AccountDetailRepositoryImpl(
        detailStore = detailStore,
        balanceLinesStore = balanceLinesStore,
        networkMonitor = FakeNetworkMonitor(NetworkStatus.Available(onlineInfo)),
        fetchedAtRepository = fetchedAt,
    )

    /** Merges the two halves the way the view model does, so the tests assert the rendered shape. */
    private fun AccountDetailRepositoryImpl.merged(
        accountId: String,
        scope: CoroutineScope,
    ): Flow<ScreenState<AccountDetailWithBalances>> = combineScreenStates(
        detailStream(accountId, scope).state,
        balanceLinesStream(accountId, scope).state,
    ) { account, rows -> AccountDetailWithBalances(detail = account, balances = rows) }

    @Test
    fun bothHalvesMergeIntoContent() = runTest(UnconfinedTestDispatcher()) {
        val state = repo().merged("acc-1", backgroundScope).first { it is ScreenState.Content }

        val content = assertIs<ScreenState.Content<AccountDetailWithBalances>>(state)
        assertEquals("acc-1", content.data.detail.accountId)
        assertEquals(balances, content.data.balances)
        assertEquals(false, content.data.hasNoBalances)
    }

    @Test
    fun eachHalfUsesAPerAccountCacheKey() = runTest(UnconfinedTestDispatcher()) {
        val fetchedAt = FakeFetchedAtRepository()

        repo(fetchedAt).merged("acc-1", backgroundScope).first { it is ScreenState.Content }

        assertTrue("accountDetail:detail:acc-1" in fetchedAt.readKeys)
        assertTrue("accountDetail:balances:acc-1" in fetchedAt.readKeys)
    }

    @Test
    fun differentAccountIdsProduceDifferentCacheKeys() = runTest(UnconfinedTestDispatcher()) {
        val fetchedAt = FakeFetchedAtRepository()

        repo(fetchedAt).merged("acc-9", backgroundScope).first { it is ScreenState.Content }

        assertTrue("accountDetail:detail:acc-9" in fetchedAt.readKeys)
        assertTrue("accountDetail:balances:acc-9" in fetchedAt.readKeys)
        assertTrue(fetchedAt.readKeys.none { it.endsWith(":acc-1") })
    }

    /**
     * The regression for the defect this repository shipped with: one instance is shared across
     * screens, so a second account must get its own streams rather than the first account's.
     */
    @Test
    fun aSecondAccountOnTheSameInstanceGetsItsOwnData() = runTest(UnconfinedTestDispatcher()) {
        val repo = repo()

        val first = repo.merged("acc-1", backgroundScope).first { it is ScreenState.Content }
        val second = repo.merged("acc-2", backgroundScope).first { it is ScreenState.Content }

        assertEquals("acc-1", assertIs<ScreenState.Content<AccountDetailWithBalances>>(first).data.detail.accountId)
        val secondContent = assertIs<ScreenState.Content<AccountDetailWithBalances>>(second)
        assertEquals("acc-2", secondContent.data.detail.accountId)
        assertEquals(balancesByAccount.getValue("acc-2"), secondContent.data.balances)
    }

    /**
     * A stream built on a scope that has since been cancelled must not be handed to a later
     * caller — the screen that opens next gets a live stream on its own scope.
     */
    @Test
    fun aRequestAfterTheFirstScopeDiedStillProducesContent() = runTest(UnconfinedTestDispatcher()) {
        val repo = repo()
        val deadScope = CoroutineScope(Job() + UnconfinedTestDispatcher(testScheduler))
        repo.merged("acc-1", deadScope).first { it is ScreenState.Content }
        deadScope.cancel()

        val state = repo.merged("acc-1", backgroundScope).first { it is ScreenState.Content }

        assertEquals("acc-1", assertIs<ScreenState.Content<AccountDetailWithBalances>>(state).data.detail.accountId)
    }

    @Test
    fun anAccountWithNoBalanceRowsIsStillContent() = runTest(UnconfinedTestDispatcher()) {
        val state = repo(balanceLinesStore = balancesStore { emptyList() })
            .merged("acc-1", backgroundScope)
            .first { it is ScreenState.Content }

        val content = assertIs<ScreenState.Content<AccountDetailWithBalances>>(state)
        assertEquals("acc-1", content.data.detail.accountId)
        assertTrue(content.data.hasNoBalances)
    }

    @Test
    fun failingDetailFetchSurfacesAsError() = runTest(UnconfinedTestDispatcher()) {
        val failing = detailStore { throw NoSuchElementException("No account detail returned") }

        val state = repo(detailStore = failing).merged("acc-1", backgroundScope).first { it is ScreenState.Error }

        assertIs<ScreenState.Error>(state)
    }

    @Test
    fun failingBalancesFetchSurfacesAsError() = runTest(UnconfinedTestDispatcher()) {
        val failing = balancesStore { throw IllegalStateException("balances unavailable") }

        val state = repo(balanceLinesStore = failing).merged("acc-1", backgroundScope).first { it is ScreenState.Error }

        assertIs<ScreenState.Error>(state)
    }

    @Test
    fun refreshingAStreamReEmitsContent() = runTest(UnconfinedTestDispatcher()) {
        val repo = repo()
        val stream = repo.detailStream("acc-1", backgroundScope)
        stream.state.first { it is ScreenState.Content }

        stream.refresh()

        val state = stream.state.first { it is ScreenState.Content }
        assertEquals("acc-1", assertIs<ScreenState.Content<AccountDetail>>(state).data.accountId)
    }
}
