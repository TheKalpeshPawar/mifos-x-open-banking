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
import org.mifosx.openbanking.core.data.banking.TransactionDetailRepository
import org.mifosx.openbanking.core.data.infra.NetworkMonitor
import org.mifosx.openbanking.core.model.banking.TransactionDetail
import org.mobilenativefoundation.store.store5.Store
import template.core.base.store.infra.FetchedAtRepository
import template.core.base.store.screen.ScreenDataStream
import template.core.base.store.screen.asScreenStream

/**
 * Opens a stream over the transaction-details store, keyed and cached per account.
 */
internal class TransactionDetailRepositoryImpl(
    private val store: Store<String, List<TransactionDetail>>,
    private val networkMonitor: NetworkMonitor,
    private val fetchedAtRepository: FetchedAtRepository,
) : TransactionDetailRepository {

    override fun transactionDetailStream(
        accountId: String,
        scope: CoroutineScope,
    ): ScreenDataStream<List<TransactionDetail>> =
        store.asScreenStream(
            key = accountId,
            networkMonitor = networkMonitor,
            fetchedAtRepository = fetchedAtRepository,
            cacheKey = "$CACHE_KEY:$accountId",
            scope = scope,
        )

    private companion object {
        const val CACHE_KEY = "transactionDetail:list"
    }
}
