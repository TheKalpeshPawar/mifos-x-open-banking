/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.kyc

import kotlinx.coroutines.CoroutineScope
import org.mifosx.openbanking.core.data.infra.NetworkMonitor
import org.mifosx.openbanking.core.data.obp.toResult
import org.mifosx.openbanking.core.model.obp.KycCheckRequest
import org.mifosx.openbanking.core.model.obp.KycDocument
import org.mifosx.openbanking.core.network.api.KycApi
import org.mifosx.openbanking.core.network.obp.ObpConfig
import org.mobilenativefoundation.store.store5.Store
import template.core.base.store.infra.FetchedAtRepository
import template.core.base.store.screen.ScreenDataStream
import template.core.base.store.screen.asScreenStream

/** Read KYC documents and submit KYC check results for a customer. */
interface KycRepository {
    /** Durable, offline-first stream of a customer's KYC documents (cache-then-network). */
    fun documentsStream(customerId: String, scope: CoroutineScope): ScreenDataStream<List<KycDocument>>

    suspend fun listDocuments(customerId: String): Result<List<KycDocument>>
    suspend fun submitCheck(customerId: String, request: KycCheckRequest): Result<KycCheckRequest>
}

class KycRepositoryImpl(
    private val api: KycApi,
    private val config: ObpConfig,
    private val documentsStore: Store<String, List<KycDocument>>,
    private val networkMonitor: NetworkMonitor,
    private val fetchedAtRepository: FetchedAtRepository,
) : KycRepository {

    override fun documentsStream(
        customerId: String,
        scope: CoroutineScope,
    ): ScreenDataStream<List<KycDocument>> =
        documentsStore.asScreenStream(
            key = customerId,
            networkMonitor = networkMonitor,
            fetchedAtRepository = fetchedAtRepository,
            cacheKey = "kyc:$customerId",
            scope = scope,
            isEmpty = { it.isEmpty() },
        )

    override suspend fun listDocuments(customerId: String): Result<List<KycDocument>> =
        api.listDocuments(config.bankId, customerId).toResult().map { it.documents }

    override suspend fun submitCheck(customerId: String, request: KycCheckRequest): Result<KycCheckRequest> =
        api.submitCheck(config.bankId, customerId, request).toResult()
}
