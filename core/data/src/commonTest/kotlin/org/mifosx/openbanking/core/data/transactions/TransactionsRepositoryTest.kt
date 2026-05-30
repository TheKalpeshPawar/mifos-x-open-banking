/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.transactions

import kotlinx.coroutines.test.runTest
import org.mifosx.openbanking.core.data.testutil.FakeObpCacheDao
import org.mifosx.openbanking.core.data.testutil.NoopFetchedAt
import org.mifosx.openbanking.core.data.testutil.testJson
import org.mifosx.openbanking.core.data.testutil.testNetworkMonitor
import org.mifosx.openbanking.core.model.obp.Transaction
import org.mifosx.openbanking.core.model.obp.TransactionsResponse
import org.mifosx.openbanking.core.network.api.TransactionsApi
import org.mifosx.openbanking.core.network.obp.ObpConfig
import org.mifosx.openbanking.core.store.transactions.provideTransactionsStore
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeTransactionsApi(
    var listResult: NetworkResult<TransactionsResponse, NetworkError> =
        NetworkResult.Success(TransactionsResponse()),
) : TransactionsApi {
    override suspend fun listTransactions(
        bankId: String,
        accountId: String,
        limit: Int?,
        offset: Int?,
    ) = listResult

    override suspend fun getTransaction(
        bankId: String,
        accountId: String,
        transactionId: String,
    ) = NetworkResult.Success(Transaction())
}

private fun txnRepo(api: TransactionsApi): TransactionsRepositoryImpl {
    val config = ObpConfig()
    return TransactionsRepositoryImpl(
        api,
        config,
        provideTransactionsStore(api, config, FakeObpCacheDao(), testJson()),
        testNetworkMonitor(),
        NoopFetchedAt,
    )
}

class TransactionsRepositoryTest {

    @Test
    fun listTransactions_unwrapsList() = runTest {
        val repo = txnRepo(
            FakeTransactionsApi(
                NetworkResult.Success(TransactionsResponse(transactions = listOf(Transaction(id = "t1")))),
            ),
        )
        val result = repo.listTransactions("acc-1")
        assertTrue(result.isSuccess)
        assertEquals("t1", result.getOrNull()?.firstOrNull()?.id)
    }

    @Test
    fun listTransactions_error_isFailure() = runTest {
        val repo = txnRepo(FakeTransactionsApi(NetworkResult.Error(NetworkError.SERVER)))
        assertTrue(repo.listTransactions("acc-1").isFailure)
    }
}
