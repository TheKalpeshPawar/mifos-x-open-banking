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
import org.mifosx.openbanking.core.data.banking.BalancesRepository
import org.mifosx.openbanking.core.data.infra.NetworkMonitor
import org.mifosx.openbanking.core.model.banking.AccountBalance
import org.mobilenativefoundation.store.store5.Store
import template.core.base.store.infra.FetchedAtRepository
import template.core.base.store.screen.ScreenDataStream
import template.core.base.store.screen.asScreenStream

internal class BalancesRepositoryImpl(
    private val store: Store<String, AccountBalance>,
    private val networkMonitor: NetworkMonitor,
    private val fetchedAtRepository: FetchedAtRepository,
) : BalancesRepository {

    override fun balanceStream(
        accountIdFlow: Flow<String>,
        scope: CoroutineScope,
    ): ScreenDataStream<AccountBalance> =
        store.asScreenStream(
            keyFlow = accountIdFlow,
            networkMonitor = networkMonitor,
            fetchedAtRepository = fetchedAtRepository,
            cacheKeyFor = { accountId -> "$CACHE_KEY:$accountId" },
            scope = scope,
        )

    private companion object {
        const val CACHE_KEY = "home:balance"
    }
}
