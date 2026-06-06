/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.banks

import kotlinx.coroutines.test.runTest
import org.mifosx.openbanking.core.model.obp.Bank
import org.mifosx.openbanking.core.model.obp.BankAttribute
import org.mifosx.openbanking.core.network.api.BanksApi
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import kotlin.test.Test
import kotlin.test.assertEquals

private class FakeBanksApi(
    var result: NetworkResult<Bank, NetworkError> = NetworkResult.Success(Bank()),
    var calls: Int = 0,
) : BanksApi {
    override suspend fun getBank(bankId: String): NetworkResult<Bank, NetworkError> {
        calls++
        return result
    }
}

class BanksRepositoryTest {

    @Test
    fun bankName_resolvesFullName() = runTest {
        val repo = BanksRepositoryImpl(
            FakeBanksApi(NetworkResult.Success(Bank(id = "ac.bank.uk", fullName = "Afternoon Coffee Bank"))),
        )
        assertEquals("Afternoon Coffee Bank", repo.bankName("ac.bank.uk"))
    }

    @Test
    fun bankName_fallsBackToIdOnError() = runTest {
        val repo = BanksRepositoryImpl(FakeBanksApi(NetworkResult.Error(NetworkError.SERVER)))
        assertEquals("TESCGB2L", repo.bankName("TESCGB2L"))
    }

    @Test
    fun bankName_cachesResult() = runTest {
        val api = FakeBanksApi(NetworkResult.Success(Bank(fullName = "Mifos-X-Open-Bank")))
        val repo = BanksRepositoryImpl(api)
        repo.bankName("mifos-x-openbank")
        repo.bankName("mifos-x-openbank")
        assertEquals(1, api.calls)
    }

    @Test
    fun bankName_blankReturnsBlank() = runTest {
        val api = FakeBanksApi()
        val repo = BanksRepositoryImpl(api)
        assertEquals("", repo.bankName(""))
        assertEquals(0, api.calls)
    }

    @Test
    fun bank_returnsDirectoryEntryWithCountry() = runTest {
        val repo = BanksRepositoryImpl(
            FakeBanksApi(
                NetworkResult.Success(
                    Bank(
                        id = "ac.bank.uk",
                        fullName = "Afternoon Coffee Bank",
                        attributes = listOf(BankAttribute(name = "SWIFT_BIC", value = "ACMEGB2L")),
                    ),
                ),
            ),
        )
        assertEquals("GB", repo.bank("ac.bank.uk")?.countryCode)
    }

    @Test
    fun bank_nullOnErrorAndRetriesNextCall() = runTest {
        val api = FakeBanksApi(NetworkResult.Error(NetworkError.SERVER))
        val repo = BanksRepositoryImpl(api)
        assertEquals(null, repo.bank("neon.bank.eu"))
        repo.bank("neon.bank.eu")
        assertEquals(2, api.calls)
    }

    @Test
    fun bank_cachesSuccess() = runTest {
        val api = FakeBanksApi(NetworkResult.Success(Bank(id = "neon.bank.eu")))
        val repo = BanksRepositoryImpl(api)
        repo.bank("neon.bank.eu")
        repo.bank("neon.bank.eu")
        assertEquals(1, api.calls)
    }

    @Test
    fun bank_blankReturnsNull() = runTest {
        val api = FakeBanksApi()
        val repo = BanksRepositoryImpl(api)
        assertEquals(null, repo.bank(""))
        assertEquals(0, api.calls)
    }
}
