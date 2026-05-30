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

import org.mifosx.openbanking.core.data.obp.toResult
import org.mifosx.openbanking.core.model.obp.Transaction
import org.mifosx.openbanking.core.network.api.TransactionsApi
import org.mifosx.openbanking.core.network.obp.ObpConfig

/** Read access to an account's OBP transactions. */
interface TransactionsRepository {
    suspend fun listTransactions(accountId: String, limit: Int? = null): Result<List<Transaction>>
    suspend fun getTransaction(accountId: String, transactionId: String): Result<Transaction>
}

class TransactionsRepositoryImpl(
    private val api: TransactionsApi,
    private val config: ObpConfig,
) : TransactionsRepository {

    override suspend fun listTransactions(accountId: String, limit: Int?): Result<List<Transaction>> =
        api.listTransactions(config.bankId, accountId, limit).toResult().map { it.transactions }

    override suspend fun getTransaction(accountId: String, transactionId: String): Result<Transaction> =
        api.getTransaction(config.bankId, accountId, transactionId).toResult()
}
