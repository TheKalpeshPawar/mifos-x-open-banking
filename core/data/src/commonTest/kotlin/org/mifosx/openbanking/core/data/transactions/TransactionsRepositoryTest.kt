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
    var lastBankId: String? = null

    override suspend fun listTransactions(
        bankId: String,
        accountId: String,
        limit: Int?,
        offset: Int?,
    ): NetworkResult<TransactionsResponse, NetworkError> {
        lastBankId = bankId
        return listResult
    }

    override suspend fun getTransaction(
        bankId: String,
        accountId: String,
        transactionId: String,
    ) = NetworkResult.Success(Transaction())
}

private fun txnRepo(api: TransactionsApi): TransactionsRepositoryImpl =
    TransactionsRepositoryImpl(
        api,
        provideTransactionsStore(api, FakeObpCacheDao(), testJson()),
        testNetworkMonitor(),
        NoopFetchedAt,
    )

class TransactionsRepositoryTest {

    @Test
    fun listTransactions_unwrapsList() = runTest {
        val repo = txnRepo(
            FakeTransactionsApi(
                NetworkResult.Success(TransactionsResponse(transactions = listOf(Transaction(id = "t1")))),
            ),
        )
        val result = repo.listTransactions("bank-a", "acc-1")
        assertTrue(result.isSuccess)
        assertEquals("t1", result.getOrNull()?.firstOrNull()?.id)
    }

    @Test
    fun listTransactions_error_isFailure() = runTest {
        val repo = txnRepo(FakeTransactionsApi(NetworkResult.Error(NetworkError.SERVER)))
        assertTrue(repo.listTransactions("bank-a", "acc-1").isFailure)
    }

    @Test
    fun listTransactions_usesAccountOwnBankId_notGlobalConfig() = runTest {
        // Cross-bank: an account from /my/accounts living at bank-B must be queried at
        // bank-B, not at any single global config bank. Regression guard for the
        // config.bankId-pinning bug.
        val api = FakeTransactionsApi(
            NetworkResult.Success(TransactionsResponse(transactions = listOf(Transaction(id = "t9")))),
        )
        val repo = txnRepo(api)
        repo.listTransactions("bank-B", "acc-at-bank-b")
        assertEquals("bank-B", api.lastBankId)
    }
}
