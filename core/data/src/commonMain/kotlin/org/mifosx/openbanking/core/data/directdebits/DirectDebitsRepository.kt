/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.directdebits

import org.mifosx.openbanking.core.data.obp.toResult
import org.mifosx.openbanking.core.model.obp.DirectDebit
import org.mifosx.openbanking.core.network.api.DirectDebitsApi
import org.mifosx.openbanking.core.network.obp.ObpConfig

/** Read + cancel access to an account's OBP direct-debit mandates. */
interface DirectDebitsRepository {
    suspend fun list(accountId: String): Result<List<DirectDebit>>
    suspend fun cancel(accountId: String, directDebitId: String): Result<DirectDebit>
}

class DirectDebitsRepositoryImpl(
    private val api: DirectDebitsApi,
    private val config: ObpConfig,
) : DirectDebitsRepository {

    override suspend fun list(accountId: String): Result<List<DirectDebit>> =
        api.listDirectDebits(config.bankId, accountId).toResult().map { it.directDebits }

    override suspend fun cancel(accountId: String, directDebitId: String): Result<DirectDebit> =
        api.cancelDirectDebit(config.bankId, accountId, directDebitId).toResult()
}
