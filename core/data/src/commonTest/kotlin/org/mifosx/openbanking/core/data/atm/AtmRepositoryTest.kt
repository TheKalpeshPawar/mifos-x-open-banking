/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.atm

import kotlinx.coroutines.test.runTest
import org.mifosx.openbanking.core.data.testutil.FakeObpCacheDao
import org.mifosx.openbanking.core.data.testutil.NoopFetchedAt
import org.mifosx.openbanking.core.data.testutil.testJson
import org.mifosx.openbanking.core.data.testutil.testNetworkMonitor
import org.mifosx.openbanking.core.model.obp.Atm
import org.mifosx.openbanking.core.model.obp.AtmsResponse
import org.mifosx.openbanking.core.network.api.AtmApi
import org.mifosx.openbanking.core.network.obp.ObpConfig
import org.mifosx.openbanking.core.store.atm.provideAtmStore
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun atm(id: String, bank: String) = Atm(id = id, bankId = bank, name = id)

private class FakeAtmApi(
    private val byBank: Map<String, NetworkResult<AtmsResponse, NetworkError>>,
) : AtmApi {
    override suspend fun listAtms(bankId: String): NetworkResult<AtmsResponse, NetworkError> =
        byBank[bankId] ?: NetworkResult.Success(AtmsResponse())
}

private fun repository(api: AtmApi): AtmRepositoryImpl {
    val config = ObpConfig()
    return AtmRepositoryImpl(
        api = api,
        config = config,
        atmsStore = provideAtmStore(api, config, FakeObpCacheDao(), testJson()),
        networkMonitor = testNetworkMonitor(),
        fetchedAtRepository = NoopFetchedAt,
    )
}

class AtmRepositoryTest {

    @Test
    fun atmsForBanks_aggregatesAcrossBanksAndDedupes() = runTest {
        val repo = repository(
            FakeAtmApi(
                mapOf(
                    "ac.bank.uk" to NetworkResult.Success(
                        AtmsResponse(listOf(atm("a1", "ac.bank.uk"), atm("a2", "ac.bank.uk"))),
                    ),
                    "neon.bank.eu" to NetworkResult.Success(
                        AtmsResponse(listOf(atm("a2", "neon.bank.eu"), atm("a3", "neon.bank.eu"))),
                    ),
                ),
            ),
        )
        val atms = repo.atmsForBanks(listOf("ac.bank.uk", "neon.bank.eu", "ac.bank.uk", "")).getOrThrow()
        assertEquals(listOf("a1", "a2", "a3"), atms.map { it.id })
    }

    @Test
    fun atmsForBanks_toleratesPerBankFailure() = runTest {
        val repo = repository(
            FakeAtmApi(
                mapOf(
                    "ac.bank.uk" to NetworkResult.Success(AtmsResponse(listOf(atm("a1", "ac.bank.uk")))),
                    "down.bank" to NetworkResult.Error(NetworkError.UNAUTHORIZED),
                ),
            ),
        )
        val atms = repo.atmsForBanks(listOf("ac.bank.uk", "down.bank")).getOrThrow()
        assertEquals(listOf("a1"), atms.map { it.id })
    }

    @Test
    fun atmsForBanks_failsWhenEveryBankFails() = runTest {
        val repo = repository(
            FakeAtmApi(mapOf("down.bank" to NetworkResult.Error(NetworkError.UNAUTHORIZED))),
        )
        assertTrue(repo.atmsForBanks(listOf("down.bank")).isFailure)
    }

    @Test
    fun atmsForBanks_failsWhenNoBanks() = runTest {
        val repo = repository(FakeAtmApi(emptyMap()))
        assertTrue(repo.atmsForBanks(listOf("", "  ")).isFailure)
    }
}
