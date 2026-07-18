/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import org.mifosx.openbanking.core.data.banking.AccountsRepository
import org.mifosx.openbanking.core.data.banking.store.BankingStores
import org.mifosx.openbanking.core.data.infra.NetworkMonitor
import org.mifosx.openbanking.core.model.banking.BankAccount
import org.mobilenativefoundation.store.store5.Store
import template.core.base.common.screen.ScreenState
import template.core.base.store.infra.FetchedAtRepository
import template.core.base.store.screen.ScreenDataStream
import template.core.base.store.screen.asScreenStream

internal class AccountsRepositoryImpl(
    private val store: Store<String, List<BankAccount>>,
    private val networkMonitor: NetworkMonitor,
    private val fetchedAtRepository: FetchedAtRepository,
) : AccountsRepository {

    private var stream: ScreenDataStream<List<BankAccount>>? = null

    private fun stream(scope: CoroutineScope): ScreenDataStream<List<BankAccount>> =
        stream ?: store.asScreenStream(
            key = BankingStores.ACCOUNTS_KEY,
            networkMonitor = networkMonitor,
            fetchedAtRepository = fetchedAtRepository,
            cacheKey = CACHE_KEY,
            scope = scope,
        ).also { stream = it }

    override fun accountsState(scope: CoroutineScope): Flow<ScreenState<List<BankAccount>>> =
        stream(scope).state

    override fun refresh() {
        stream?.refresh()
    }

    private companion object {
        const val CACHE_KEY = "home:accounts"
    }
}
