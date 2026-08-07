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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.mifosx.openbanking.core.data.banking.PaymentHistoryRepository
import org.mifosx.openbanking.core.data.banking.mapper.toEntity
import org.mifosx.openbanking.core.data.banking.mapper.toFailureEntity
import org.mifosx.openbanking.core.data.banking.mapper.toPaymentHistoryItem
import org.mifosx.openbanking.core.data.banking.mapper.toPaymentReceipt
import org.mifosx.openbanking.core.database.banking.dao.PaymentHistoryDao
import org.mifosx.openbanking.core.model.banking.payment.PaymentDisposition
import org.mifosx.openbanking.core.model.banking.payment.PaymentDraft
import org.mifosx.openbanking.core.model.banking.payment.PaymentHistoryItem
import org.mifosx.openbanking.core.model.banking.payment.PaymentReceipt
import org.mifosx.openbanking.core.model.banking.payment.PaymentStatus
import org.mifosx.openbanking.core.network.api.ConsentCreationScope
import org.mifosx.openbanking.core.network.api.OAuth
import org.mifosx.openbanking.core.network.api.Pisp
import kotlin.time.Clock

/**
 * Reads [PaymentHistoryDao.observeRecent] for the hub, writes on payment outcomes, and refreshes
 * in-flight statuses via [Pisp.getDomesticPayment].
 *
 * Stateless — the only thing it holds are its injected collaborators.
 */
internal class PaymentHistoryRepositoryImpl(
    private val dao: PaymentHistoryDao,
    private val pisp: Pisp,
    private val oauth: OAuth,
) : PaymentHistoryRepository {

    override fun observeRecent(): Flow<List<PaymentHistoryItem>> =
        dao.observeRecent().map { entities -> entities.map { it.toPaymentHistoryItem() } }

    override suspend fun saveSubmitted(receipt: PaymentReceipt, draft: PaymentDraft) {
        dao.upsert(receipt.toEntity(draft))
    }

    override suspend fun saveFailed(draft: PaymentDraft, errorKind: String, errorDescription: String) {
        val now = Clock.System.now().toEpochMilliseconds().toString()
        dao.upsert(draft.toFailureEntity(errorKind, errorDescription).copy(creationDateTime = now))
    }

    /**
     * Refreshes every in-flight submitted payment.
     *
     * Only rows whose status resolves to [PaymentStatus.PaymentDisposition.InProgress] are
     * refreshed; terminal successes, pre-submission failures, and rejected payments are skipped.
     * Each refreshed row is re-upserted with the updated status and [syncedAt] timestamp.
     */
    @Suppress("ReturnCount")
    override suspend fun refreshStatuses() {
        val entities = dao.observeRecent().first()
        val token = when (val result = oauth.clientCredentialsToken(ConsentCreationScope.PAYMENTS)) {
            is template.core.base.network.NetworkResult.Success -> result.data.accessToken
            is template.core.base.network.NetworkResult.Error -> return
        }

        entities
            .filter { it.domesticPaymentId != null && it.errorKind == null }
            .filter { e ->
                val resolved = e.status?.let(PaymentStatus.Companion::fromWire)
                resolved?.disposition == PaymentDisposition.InProgress
            }
            .forEach { entity ->
                val now = Clock.System.now().toEpochMilliseconds().toString()
                val result = pisp.getDomesticPayment(token, entity.domesticPaymentId!!)
                when (result) {
                    is template.core.base.network.NetworkResult.Success -> {
                        val receipt = result.data.toPaymentReceipt()
                        dao.upsert(
                            entity.copy(
                                status = receipt.status.name,
                                settlementDateTime = receipt.settlementDateTime
                                    .takeIf { it.isNotBlank() },
                                syncedAt = now,
                            ),
                        )
                    }
                    is template.core.base.network.NetworkResult.Error -> {
                        // Keep the last-known status; mark that we tried.
                        dao.upsert(entity.copy(syncedAt = now))
                    }
                }
            }
    }
}
