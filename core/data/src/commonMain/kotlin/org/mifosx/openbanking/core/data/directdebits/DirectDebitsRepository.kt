/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.directdebits

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.mifosx.openbanking.core.data.transactions.TransactionsRepository
import org.mifosx.openbanking.core.database.cache.ObpCacheDao
import org.mifosx.openbanking.core.database.cache.ObpCacheEntity
import org.mifosx.openbanking.core.model.obp.DirectDebitMandate
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Direct-debit mandates for an account. OBP only exposes POST-create (no list/get/cancel
 * at any API version), so:
 *  - [listMandates] DERIVES rows from transaction history (outgoing `TXN_TYPE=DD`
 *    transactions grouped into collection series).
 *  - [cancel] records the cancellation locally (the server has no cancel endpoint) — the
 *    mandate renders CANCELLED from then on, persisted across restarts in the local
 *    JSON cache.
 */
interface DirectDebitsRepository {
    suspend fun listMandates(bankId: String, accountId: String): Result<List<DirectDebitMandate>>

    suspend fun cancel(accountId: String, mandateId: String): Result<Unit>
}

class DirectDebitsRepositoryImpl(
    private val transactionsRepository: TransactionsRepository,
    private val dao: ObpCacheDao,
    private val json: Json,
) : DirectDebitsRepository {

    @OptIn(ExperimentalTime::class)
    override suspend fun listMandates(bankId: String, accountId: String): Result<List<DirectDebitMandate>> {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return transactionsRepository.listTransactionsWithAttributes(bankId, accountId).map { transactions ->
            val cancelled = readCancelled(accountId)
            deriveDirectDebits(transactions, today).map { mandate ->
                if (mandate.id in cancelled) {
                    mandate.copy(status = DirectDebitMandate.STATUS_CANCELLED, nextCollectionDate = "")
                } else {
                    mandate
                }
            }
        }
    }

    override suspend fun cancel(accountId: String, mandateId: String): Result<Unit> {
        persistCancelled(accountId, readCancelled(accountId) + mandateId)
        return Result.success(Unit)
    }

    private suspend fun readCancelled(accountId: String): Set<String> {
        val payload = dao.observe(cancelledKey(accountId)).firstOrNull() ?: return emptySet()
        return runCatching { json.decodeFromString(idsSerializer, payload) }.getOrDefault(emptySet())
    }

    private suspend fun persistCancelled(accountId: String, ids: Set<String>) {
        dao.upsert(
            ObpCacheEntity(
                storeKey = cancelledKey(accountId),
                payload = json.encodeToString(idsSerializer, ids),
            ),
        )
    }

    private fun cancelledKey(accountId: String) = "direct-debits-cancelled:$accountId"

    private val idsSerializer = SetSerializer(String.serializer())
}
