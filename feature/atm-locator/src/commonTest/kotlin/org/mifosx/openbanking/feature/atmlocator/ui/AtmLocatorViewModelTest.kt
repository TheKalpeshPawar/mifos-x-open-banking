/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.atmlocator.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.core.data.accounts.AccountsRepository
import org.mifosx.openbanking.core.data.atm.AtmRepository
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.core.model.obp.Atm
import org.mifosx.openbanking.core.model.obp.GeoLocation
import org.mifosx.openbanking.core.model.obp.OpeningHours
import org.mifosx.openbanking.core.model.obp.PostalAddress
import template.core.base.store.screen.ScreenDataStream
import template.core.base.store.screen.ScreenState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun atm(id: String, bank: String, city: String, lat: Double, lng: Double) = Atm(
    id = id,
    bankId = bank,
    name = id,
    address = PostalAddress(line1 = "$id Street", city = city, postCode = "EC1$id"),
    location = GeoLocation(latitude = lat, longitude = lng),
)

private class FakeAtmRepository(
    var result: Result<List<Atm>> = Result.success(emptyList()),
) : AtmRepository {
    var requestedBanks: List<String> = emptyList()
    override fun atmsStream(scope: CoroutineScope): ScreenDataStream<List<Atm>> = TODO("not used")
    override suspend fun listAtms(): Result<List<Atm>> = TODO("not used")
    override suspend fun atmsForBanks(bankIds: List<String>): Result<List<Atm>> {
        requestedBanks = bankIds
        return result
    }
}

private class FakeAccountsRepository(
    private val accounts: List<Account> = listOf(
        Account(id = "ac.checking.001", bankId = "ac.bank.uk"),
        Account(id = "neon.wallet.001", bankId = "neon.bank.eu"),
    ),
) : AccountsRepository {
    override fun accountsStream(scope: CoroutineScope): ScreenDataStream<List<Account>> = TODO("not used")
    override suspend fun listAccounts(): Result<List<Account>> = TODO("not used")
    override suspend fun myAccounts(): Result<List<Account>> = Result.success(accounts)
    override suspend fun accountDetail(bankId: String, accountId: String): Result<Account> = TODO("not used")
}

class AtmLocatorViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(
        atms: FakeAtmRepository = FakeAtmRepository(),
        accounts: FakeAccountsRepository = FakeAccountsRepository(),
    ) = AtmLocatorViewModel(atmRepository = atms, accountsRepository = accounts)

    private suspend fun TestScope.content(model: AtmLocatorViewModel): AtmLocatorContent {
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        val s = model.uiState.value
        assertTrue(s is ScreenState.Content, "expected Content, was $s")
        return s.data
    }

    @Test
    fun load_aggregatesAtmsAcrossEveryUserBank() = runTest(dispatcher) {
        val repo = FakeAtmRepository(
            result = Result.success(
                listOf(
                    atm("a1", "ac.bank.uk", "London", 51.5141, -0.1410),
                    atm("a2", "neon.bank.eu", "Berlin", 52.5200, 13.4050),
                ),
            ),
        )
        val c = content(vm(repo))
        assertEquals(listOf("ac.bank.uk", "neon.bank.eu"), repo.requestedBanks)
        assertEquals(2, c.atms.size)
        assertEquals("a1 Street, London, EC1a1", c.atms.first().addressLine)
        assertEquals("2 ATMs", c.resultSummary)
    }

    @Test
    fun emptyAtms_isEmptyState() = runTest(dispatcher) {
        val model = vm(FakeAtmRepository(result = Result.success(emptyList())))
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(model.uiState.value is ScreenState.Empty)
    }

    @Test
    fun loadFailure_isError_andRetryRecovers() = runTest(dispatcher) {
        val repo = FakeAtmRepository(result = Result.failure(IllegalStateException("boom")))
        val model = vm(repo)
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(model.uiState.value is ScreenState.Error)

        repo.result = Result.success(listOf(atm("a1", "ac.bank.uk", "London", 51.5, -0.1)))
        model.onRetry()
        advanceUntilIdle()
        assertTrue(model.uiState.value is ScreenState.Content)
    }

    @Test
    fun userLocation_sortsNearestFirst_andLabelsDistance() = runTest(dispatcher) {
        val repo = FakeAtmRepository(
            result = Result.success(
                listOf(
                    atm("far", "ac.bank.uk", "Berlin", 52.5200, 13.4050),
                    atm("near", "ac.bank.uk", "London", 51.5141, -0.1410),
                ),
            ),
        )
        val model = vm(repo)
        content(model)
        model.onUserLocation(51.5074, -0.1278)
        advanceUntilIdle()
        val c = (model.uiState.value as ScreenState.Content).data
        assertEquals(listOf("near", "far"), c.atms.map { it.id })
        assertTrue(c.hasLocation)
        assertTrue(c.atms.first().distanceLabel.isNotBlank())
        assertEquals("2 ATMs near you", c.resultSummary)
    }

    @Test
    fun query_filtersByCity() = runTest(dispatcher) {
        val repo = FakeAtmRepository(
            result = Result.success(
                listOf(
                    atm("a1", "ac.bank.uk", "London", 51.5, -0.1),
                    atm("a2", "neon.bank.eu", "Berlin", 52.5, 13.4),
                ),
            ),
        )
        val model = vm(repo)
        content(model)
        model.onQueryChanged("berlin")
        advanceUntilIdle()
        val c = (model.uiState.value as ScreenState.Content).data
        assertEquals(listOf("a2"), c.atms.map { it.id })
    }

    @Test
    fun noBanks_isEmptyState() = runTest(dispatcher) {
        val model = vm(accounts = FakeAccountsRepository(accounts = emptyList()))
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(model.uiState.value is ScreenState.Empty)
    }

    @Test
    fun selection_togglesOpenStateAndSwitchesBetweenAtms() = runTest(dispatcher) {
        val repo = FakeAtmRepository(
            result = Result.success(
                listOf(
                    atm("a1", "ac.bank.uk", "London", 51.5, -0.1),
                    atm("a2", "neon.bank.eu", "Berlin", 52.5, 13.4),
                ),
            ),
        )
        val model = vm(repo)
        content(model)

        model.onAtmSelected("a1")
        advanceUntilIdle()
        assertEquals("a1", (model.uiState.value as ScreenState.Content).data.selectedAtmId)

        model.onAtmSelected("a1")
        advanceUntilIdle()
        assertEquals("", (model.uiState.value as ScreenState.Content).data.selectedAtmId)

        model.onAtmSelected("a1")
        model.onAtmSelected("a2")
        advanceUntilIdle()
        assertEquals("a2", (model.uiState.value as ScreenState.Content).data.selectedAtmId)
    }

    @Test
    fun row_surfacesHoursAccessibilityAndDeposits() = runTest(dispatcher) {
        val open247 = OpeningHours("00:00", "23:59")
        val rich = Atm(
            id = "rich",
            bankId = "ac.bank.uk",
            name = "Acme ATM",
            address = PostalAddress(line1 = "1 High St", city = "London"),
            location = GeoLocation(51.5, -0.1),
            moreInfo = "Wheelchair access",
            isAccessible = "true",
            hasDepositCapability = "true",
            monday = open247,
            tuesday = open247,
            wednesday = open247,
            thursday = open247,
            friday = open247,
            saturday = open247,
            sunday = open247,
        )
        val c = content(vm(FakeAtmRepository(result = Result.success(listOf(rich)))))
        val row = c.atms.first()
        assertEquals("Open 24 hours", row.hoursLabel)
        assertTrue(row.accessible)
        assertTrue(row.acceptsDeposits)
        assertEquals("Wheelchair access", row.moreInfo)
    }
}
