/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.accounts.impl

import org.mifosx.openbanking.core.data.accounts.AccountsRepository
import org.mifosx.openbanking.core.data.obp.toResult
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.core.network.api.AccountsApi
import org.mifosx.openbanking.core.network.obp.ObpConfig

/**
 * Network-bound accounts repository over [AccountsApi]. A Store5 cache layer
 * (offline-first, single-flight) wraps this in the data-layer fan-out follow-up.
 */
class AccountsRepositoryImpl(
    private val api: AccountsApi,
    private val config: ObpConfig,
) : AccountsRepository {

    override suspend fun listAccounts(): Result<List<Account>> =
        api.listAccounts(config.bankId).toResult().map { it.accounts }

    override suspend fun myAccounts(): Result<List<Account>> =
        api.myAccounts().toResult().map { it.accounts }

    override suspend fun accountDetail(accountId: String): Result<Account> =
        api.accountDetail(config.bankId, accountId).toResult()
}
