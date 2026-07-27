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
import org.mifosx.openbanking.core.data.banking.StatementDetailRepository
import org.mifosx.openbanking.core.data.infra.NetworkMonitor
import org.mifosx.openbanking.core.model.banking.StatementDetail
import org.mifosx.openbanking.core.model.banking.TransactionItem
import org.mobilenativefoundation.store.store5.Store
import template.core.base.store.infra.FetchedAtRepository
import template.core.base.store.screen.ScreenDataStream
import template.core.base.store.screen.asScreenStream

/**
 * Opens streams over the statement-detail and statement-transactions stores.
 *
 * Both stores are keyed by a composite `"$accountId|$statementId"`, and each cache key adds both ids so
 * two statements on the same account never collide. Stateless: each call builds a fresh stream bound to
 * the caller's scope, which the view model owns.
 */
internal class StatementDetailRepositoryImpl(
    private val detailStore: Store<String, StatementDetail>,
    private val transactionsStore: Store<String, List<TransactionItem>>,
    private val networkMonitor: NetworkMonitor,
    private val fetchedAtRepository: FetchedAtRepository,
) : StatementDetailRepository {

    override fun statementStream(
        accountId: String,
        statementId: String,
        scope: CoroutineScope,
    ): ScreenDataStream<StatementDetail> =
        detailStore.asScreenStream(
            key = "$accountId|$statementId",
            networkMonitor = networkMonitor,
            fetchedAtRepository = fetchedAtRepository,
            cacheKey = "$DETAIL_CACHE_KEY:$accountId:$statementId",
            scope = scope,
        )

    override fun statementTransactionsStream(
        accountId: String,
        statementId: String,
        scope: CoroutineScope,
    ): ScreenDataStream<List<TransactionItem>> =
        transactionsStore.asScreenStream(
            key = "$accountId|$statementId",
            networkMonitor = networkMonitor,
            fetchedAtRepository = fetchedAtRepository,
            cacheKey = "$TXNS_CACHE_KEY:$accountId:$statementId",
            scope = scope,
        )

    private companion object {
        const val DETAIL_CACHE_KEY = "statementDetail:meta"
        const val TXNS_CACHE_KEY = "statementDetail:txns"
    }
}
