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
import org.mifosx.openbanking.core.model.obp.Transaction
import org.mifosx.openbanking.core.model.obp.TransactionsResponse
import org.mifosx.openbanking.core.network.api.TransactionsApi
import org.mifosx.openbanking.core.network.obp.ObpConfig
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

    override suspend fun getTransaction(bankId: String, accountId: String, transactionId: String) =
        NetworkResult.Success(Transaction())
}

class TransactionsRepositoryTest {
    private val repo = TransactionsRepositoryImpl(
        FakeTransactionsApi(
            NetworkResult.Success(TransactionsResponse(transactions = listOf(Transaction(id = "t1")))),
        ),
        ObpConfig(),
    )

    @Test
    fun listTransactions_unwrapsList() = runTest {
        val result = repo.listTransactions("acc-1")
        assertTrue(result.isSuccess)
        assertEquals("t1", result.getOrNull()?.firstOrNull()?.id)
    }

    @Test
    fun listTransactions_error_isFailure() = runTest {
        val errRepo = TransactionsRepositoryImpl(
            FakeTransactionsApi(NetworkResult.Error(NetworkError.SERVER)),
            ObpConfig(),
        )
        assertTrue(errRepo.listTransactions("acc-1").isFailure)
    }
}
