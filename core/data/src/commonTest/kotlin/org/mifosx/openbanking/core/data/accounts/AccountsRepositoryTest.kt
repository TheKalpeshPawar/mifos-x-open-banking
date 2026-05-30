/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.accounts

import kotlinx.coroutines.test.runTest
import org.mifosx.openbanking.core.data.accounts.impl.AccountsRepositoryImpl
import org.mifosx.openbanking.core.data.infra.impl.NetworkMonitorImpl
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.core.model.obp.AccountsResponse
import org.mifosx.openbanking.core.model.obp.ObpException
import org.mifosx.openbanking.core.network.api.AccountsApi
import org.mifosx.openbanking.core.network.obp.ObpConfig
import org.mifosx.openbanking.core.store.accounts.provideAccountsStore
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import template.core.base.store.infra.FetchedAtRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

private class FakeAccountsApi(
    var accountsResult: NetworkResult<AccountsResponse, NetworkError> =
        NetworkResult.Success(AccountsResponse()),
    var detailResult: NetworkResult<Account, NetworkError> =
        NetworkResult.Success(Account()),
) : AccountsApi {
    override suspend fun listAccounts(bankId: String) = accountsResult
    override suspend fun myAccounts() = accountsResult
    override suspend fun accountDetail(bankId: String, accountId: String) = detailResult
}

private object NoopFetchedAt : FetchedAtRepository {
    override suspend fun read(storeKey: String): Instant? = null
    override suspend fun write(storeKey: String, instant: Instant) {}
}

private fun buildRepo(api: AccountsApi, config: ObpConfig) = AccountsRepositoryImpl(
    api = api,
    config = config,
    accountsStore = provideAccountsStore(api, config),
    networkMonitor = NetworkMonitorImpl(),
    fetchedAtRepository = NoopFetchedAt,
)

class AccountsRepositoryTest {

    private val config = ObpConfig(consumerKey = "test-key")

    @Test
    fun listAccounts_success_unwrapsAccounts() = runTest {
        val api = FakeAccountsApi(
            accountsResult = NetworkResult.Success(
                AccountsResponse(accounts = listOf(Account(id = "acc-1", label = "Main"))),
            ),
        )
        val repo = buildRepo(api, config)

        val result = repo.listAccounts()

        assertTrue(result.isSuccess)
        assertEquals(listOf(Account(id = "acc-1", label = "Main")), result.getOrNull())
    }

    @Test
    fun listAccounts_error_mapsToObpException() = runTest {
        val api = FakeAccountsApi(accountsResult = NetworkResult.Error(NetworkError.UNAUTHORIZED))
        val repo = buildRepo(api, config)

        val result = repo.listAccounts()

        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue(ex is ObpException)
        assertEquals(NetworkError.UNAUTHORIZED.name, ex.reason)
    }

    @Test
    fun accountDetail_passesBankIdAndAccountId() = runTest {
        val api = FakeAccountsApi(detailResult = NetworkResult.Success(Account(id = "acc-9")))
        val repo = buildRepo(api, config)

        val result = repo.accountDetail("acc-9")

        assertEquals("acc-9", result.getOrNull()?.id)
    }
}
