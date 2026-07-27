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
import org.mifosx.openbanking.core.data.banking.ScheduledPaymentsRepository
import org.mifosx.openbanking.core.data.infra.NetworkMonitor
import org.mifosx.openbanking.core.model.banking.ScheduledPaymentItem
import org.mobilenativefoundation.store.store5.Store
import template.core.base.store.infra.FetchedAtRepository
import template.core.base.store.screen.ScreenDataStream
import template.core.base.store.screen.asScreenStream

/**
 * Opens a stream over the scheduled-payments store, keyed and cached per account.
 */
internal class ScheduledPaymentsRepositoryImpl(
    private val store: Store<String, List<ScheduledPaymentItem>>,
    private val networkMonitor: NetworkMonitor,
    private val fetchedAtRepository: FetchedAtRepository,
) : ScheduledPaymentsRepository {

    override fun scheduledPaymentsStream(
        accountId: String,
        scope: CoroutineScope,
    ): ScreenDataStream<List<ScheduledPaymentItem>> =
        store.asScreenStream(
            key = accountId,
            networkMonitor = networkMonitor,
            fetchedAtRepository = fetchedAtRepository,
            cacheKey = "$CACHE_KEY:$accountId",
            scope = scope,
        )

    private companion object {
        const val CACHE_KEY = "scheduledPayments:list"
    }
}
