/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.standingorders

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.mifosx.openbanking.core.data.customers.CustomersRepository
import org.mifosx.openbanking.core.data.obp.toResult
import org.mifosx.openbanking.core.data.transactions.TransactionsRepository
import org.mifosx.openbanking.core.database.cache.ObpCacheDao
import org.mifosx.openbanking.core.database.cache.ObpCacheEntity
import org.mifosx.openbanking.core.model.obp.CreateStandingOrderRequest
import org.mifosx.openbanking.core.model.obp.StandingOrder
import org.mifosx.openbanking.core.network.api.StandingOrdersApi
import org.mifosx.openbanking.core.network.obp.ObpConfig
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Standing orders for an account. OBP only exposes POST-create (no list/get/delete at
 * any API version), so:
 *  - [listRecurring] DERIVES rows from transaction history (outgoing `TXN_TYPE=SO`
 *    transactions grouped into series) and merges orders created on this device, which
 *    are persisted in the local JSON cache (the server cannot return them).
 *  - [create] POSTs the real OBP endpoint, then records the order locally so it shows
 *    up in the list immediately.
 */
interface StandingOrdersRepository {
    suspend fun listRecurring(accountId: String): Result<List<StandingOrder>>
    suspend fun create(accountId: String, name: String, request: CreateStandingOrderRequest): Result<StandingOrder>
}

class StandingOrdersRepositoryImpl(
    private val api: StandingOrdersApi,
    private val transactionsRepository: TransactionsRepository,
    private val customersRepository: CustomersRepository,
    private val config: ObpConfig,
    private val dao: ObpCacheDao,
    private val json: Json,
) : StandingOrdersRepository {

    @OptIn(ExperimentalTime::class)
    override suspend fun listRecurring(accountId: String): Result<List<StandingOrder>> =
        transactionsRepository.listTransactionsWithAttributes(config.bankId, accountId).map { transactions ->
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val derived = resolveHolderNames(deriveStandingOrders(transactions, today))
            val created = readCreated(accountId)
                // A created order that has started paying shows up as a derived series; drop the local copy.
                .filter { local -> derived.none { it.name.equals(local.name, ignoreCase = true) } }
            (created + derived).sortedWith(
                compareBy<StandingOrder> { statusRank(it.status) }.thenBy { it.nextPaymentDate },
            )
        }

    /**
     * Swaps the transaction holder name (often the login username) for the counterparty
     * account holder's LEGAL name via customer-account-links. Falls back to the existing
     * name when the lookup fails or comes back blank.
     */
    private suspend fun resolveHolderNames(orders: List<StandingOrder>): List<StandingOrder> {
        val holderByAccount = orders
            .map { it.counterpartyAccount }
            .filter { it.isNotBlank() }
            .distinct()
            .associateWith { account ->
                customersRepository.accountHolderName(config.bankId, account)
                    .getOrNull()
                    ?.takeIf { it.isNotBlank() }
            }
        return orders.map { order ->
            holderByAccount[order.counterpartyAccount]
                ?.let { order.copy(counterpartyName = it) }
                ?: order
        }
    }

    override suspend fun create(
        accountId: String,
        name: String,
        request: CreateStandingOrderRequest,
    ): Result<StandingOrder> =
        api.createStandingOrder(config.bankId, accountId, request).toResult().map { response ->
            val order = StandingOrder(
                id = response.standingOrderId.ifBlank { "so-created-${request.counterpartyId}" },
                name = name,
                counterpartyName = name,
                counterpartyAccount = request.counterpartyId,
                amountValue = request.amount.amount,
                amountCurrency = request.amount.currency,
                frequency = request.`when`.frequency,
                lastPaymentDate = "",
                nextPaymentDate = request.dateStarts.substringBefore('T'),
                status = if (response.active) StandingOrder.STATUS_ACTIVE else StandingOrder.STATUS_PAUSED,
                created = true,
            )
            persistCreated(accountId, order)
            order
        }

    private suspend fun readCreated(accountId: String): List<StandingOrder> {
        val payload = dao.observe(createdKey(accountId)).firstOrNull() ?: return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(StandingOrder.serializer()), payload)
        }.getOrDefault(emptyList())
    }

    private suspend fun persistCreated(accountId: String, order: StandingOrder) {
        val merged = readCreated(accountId).filter { it.id != order.id } + order
        dao.upsert(
            ObpCacheEntity(
                storeKey = createdKey(accountId),
                payload = json.encodeToString(ListSerializer(StandingOrder.serializer()), merged),
            ),
        )
    }

    private fun createdKey(accountId: String) = "standing-orders-created:$accountId"
}
