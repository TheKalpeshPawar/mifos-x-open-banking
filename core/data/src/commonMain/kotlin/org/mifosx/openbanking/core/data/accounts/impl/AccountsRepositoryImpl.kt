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

import kotlinx.coroutines.CoroutineScope
import org.mifosx.openbanking.core.data.accounts.AccountsRepository
import org.mifosx.openbanking.core.data.infra.NetworkMonitor
import org.mifosx.openbanking.core.data.obp.toResult
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.core.network.api.AccountsApi
import org.mifosx.openbanking.core.network.obp.ObpConfig
import org.mobilenativefoundation.store.store5.Store
import template.core.base.store.infra.FetchedAtRepository
import template.core.base.store.screen.ScreenDataStream
import template.core.base.store.screen.asScreenStream

/**
 * Accounts repository. [accountsStream] is the Store5-backed offline-first read
 * (single-flight + in-memory cache + network-aware ScreenState); the suspend
 * methods remain for one-shot reads (detail, my-accounts).
 */
class AccountsRepositoryImpl(
    private val api: AccountsApi,
    private val config: ObpConfig,
    private val accountsStore: Store<Unit, List<Account>>,
    private val networkMonitor: NetworkMonitor,
    private val fetchedAtRepository: FetchedAtRepository,
) : AccountsRepository {

    override fun accountsStream(scope: CoroutineScope): ScreenDataStream<List<Account>> =
        accountsStore.asScreenStream(
            key = Unit,
            networkMonitor = networkMonitor,
            fetchedAtRepository = fetchedAtRepository,
            cacheKey = "accounts",
            scope = scope,
            isEmpty = { it.isEmpty() },
        )

    override suspend fun listAccounts(): Result<List<Account>> =
        api.myAccounts().toResult().map { it.accounts }

    override suspend fun myAccounts(): Result<List<Account>> =
        api.myAccounts().toResult().map { it.accounts }

    override suspend fun accountDetail(bankId: String, accountId: String): Result<Account> =
        api.accountDetail(bankId.ifBlank { config.bankId }, accountId).toResult()
}
