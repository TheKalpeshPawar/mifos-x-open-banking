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
import org.mifosx.openbanking.core.data.banking.AccountDetailRepository
import org.mifosx.openbanking.core.data.infra.NetworkMonitor
import org.mifosx.openbanking.core.model.banking.AccountBalanceLine
import org.mifosx.openbanking.core.model.banking.AccountDetail
import org.mobilenativefoundation.store.store5.Store
import template.core.base.store.infra.FetchedAtRepository
import template.core.base.store.screen.ScreenDataStream
import template.core.base.store.screen.asScreenStream

/**
 * Opens streams over the account-detail and balance-line stores.
 *
 * The two stores fetch concurrently and cache independently, so returning to this screen renders
 * from cache instead of re-issuing both requests.
 */
internal class AccountDetailRepositoryImpl(
    private val detailStore: Store<String, AccountDetail>,
    private val balanceLinesStore: Store<String, List<AccountBalanceLine>>,
    private val networkMonitor: NetworkMonitor,
    private val fetchedAtRepository: FetchedAtRepository,
) : AccountDetailRepository {

    override fun detailStream(accountId: String, scope: CoroutineScope): ScreenDataStream<AccountDetail> =
        detailStore.asScreenStream(
            key = accountId,
            networkMonitor = networkMonitor,
            fetchedAtRepository = fetchedAtRepository,
            cacheKey = "$DETAIL_CACHE_KEY:$accountId",
            scope = scope,
        )

    override fun balanceLinesStream(
        accountId: String,
        scope: CoroutineScope,
    ): ScreenDataStream<List<AccountBalanceLine>> =
        balanceLinesStore.asScreenStream(
            key = accountId,
            networkMonitor = networkMonitor,
            fetchedAtRepository = fetchedAtRepository,
            cacheKey = "$BALANCES_CACHE_KEY:$accountId",
            scope = scope,
        )

    private companion object {
        const val DETAIL_CACHE_KEY = "accountDetail:detail"
        const val BALANCES_CACHE_KEY = "accountDetail:balances"
    }
}
