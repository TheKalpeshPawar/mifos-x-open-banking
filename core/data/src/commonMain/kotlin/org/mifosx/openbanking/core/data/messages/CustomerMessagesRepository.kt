/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.messages

import kotlinx.coroutines.CoroutineScope
import org.mifosx.openbanking.core.data.infra.NetworkMonitor
import org.mifosx.openbanking.core.data.obp.toResult
import org.mifosx.openbanking.core.model.obp.CustomerMessage
import org.mifosx.openbanking.core.model.obp.CustomerMessageRequest
import org.mifosx.openbanking.core.network.api.CustomerMessagesApi
import org.mifosx.openbanking.core.network.obp.ObpConfig
import org.mobilenativefoundation.store.store5.Store
import template.core.base.store.infra.FetchedAtRepository
import template.core.base.store.screen.ScreenDataStream
import template.core.base.store.screen.asScreenStream

/** Read + send OBP customer messages. */
interface CustomerMessagesRepository {
    /** Durable, offline-first stream of a customer's message thread (cache-then-network). */
    fun messagesStream(customerId: String, scope: CoroutineScope): ScreenDataStream<List<CustomerMessage>>

    suspend fun list(customerId: String): Result<List<CustomerMessage>>
    suspend fun send(customerId: String, request: CustomerMessageRequest): Result<CustomerMessage>
}

class CustomerMessagesRepositoryImpl(
    private val api: CustomerMessagesApi,
    private val config: ObpConfig,
    private val messagesStore: Store<String, List<CustomerMessage>>,
    private val networkMonitor: NetworkMonitor,
    private val fetchedAtRepository: FetchedAtRepository,
) : CustomerMessagesRepository {

    override fun messagesStream(
        customerId: String,
        scope: CoroutineScope,
    ): ScreenDataStream<List<CustomerMessage>> =
        messagesStore.asScreenStream(
            key = customerId,
            networkMonitor = networkMonitor,
            fetchedAtRepository = fetchedAtRepository,
            cacheKey = "messages:$customerId",
            scope = scope,
            isEmpty = { it.isEmpty() },
        )

    override suspend fun list(customerId: String): Result<List<CustomerMessage>> =
        api.listMessages(config.bankId, customerId).toResult().map { it.messages }

    override suspend fun send(customerId: String, request: CustomerMessageRequest): Result<CustomerMessage> =
        api.sendMessage(config.bankId, customerId, request).toResult()
}
