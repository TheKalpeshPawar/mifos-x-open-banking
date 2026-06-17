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
import org.mifosx.openbanking.core.data.profile.ProfileRepository
import org.mifosx.openbanking.core.data.transactions.TransactionsRepository
import org.mifosx.openbanking.core.database.cache.ObpCacheDao
import org.mifosx.openbanking.core.database.cache.ObpCacheEntity
import org.mifosx.openbanking.core.model.obp.CreateStandingOrderRequest
import org.mifosx.openbanking.core.model.obp.StandingOrder
import org.mifosx.openbanking.core.model.obp.StandingOrderDetail
import org.mifosx.openbanking.core.model.obp.Transaction
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
    suspend fun listRecurring(bankId: String, accountId: String): Result<List<StandingOrder>>

    /**
     * One standing order with its observed execution history (the booked `TXN_TYPE=SO`
     * transactions of its series, newest first). Created-on-device orders that have not
     * paid yet return an empty history. Unknown [standingOrderId] → failure.
     */
    suspend fun detail(bankId: String, accountId: String, standingOrderId: String): Result<StandingOrderDetail>

    suspend fun create(
        bankId: String,
        accountId: String,
        name: String,
        request: CreateStandingOrderRequest,
    ): Result<StandingOrder>
}

class StandingOrdersRepositoryImpl(
    private val api: StandingOrdersApi,
    private val transactionsRepository: TransactionsRepository,
    private val customersRepository: CustomersRepository,
    private val profileRepository: ProfileRepository,
    private val config: ObpConfig,
    private val dao: ObpCacheDao,
    private val json: Json,
) : StandingOrdersRepository {

    override suspend fun listRecurring(bankId: String, accountId: String): Result<List<StandingOrder>> {
        val resolvedBank = bankId.ifBlank { config.bankId }
        return transactionsRepository.listTransactionsWithAttributes(resolvedBank, accountId).map { transactions ->
            assembleOrders(resolvedBank, accountId, transactions)
        }
    }

    override suspend fun detail(
        bankId: String,
        accountId: String,
        standingOrderId: String,
    ): Result<StandingOrderDetail> {
        val resolvedBank = bankId.ifBlank { config.bankId }
        return transactionsRepository.listTransactionsWithAttributes(resolvedBank, accountId)
            .mapCatching { transactions ->
                val order = assembleOrders(resolvedBank, accountId, transactions)
                    .firstOrNull { it.id == standingOrderId }
                    ?: throw NoSuchElementException("Standing order not found: $standingOrderId")
                StandingOrderDetail(
                    order = order,
                    executions = deriveExecutions(transactions, standingOrderId),
                )
            }
    }

    /** Derived series (holder names resolved) merged with locally created orders, list-sorted. */
    @OptIn(ExperimentalTime::class)
    private suspend fun assembleOrders(
        resolvedBank: String,
        accountId: String,
        transactions: List<Transaction>,
    ): List<StandingOrder> {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val derived = resolveHolderNames(resolvedBank, deriveStandingOrders(transactions, today))
        val created = readCreated(accountId)
            .filter { local -> derived.none { it.name.equals(local.name, ignoreCase = true) } }
        return (created + derived).sortedWith(
            compareBy<StandingOrder> { statusRank(it.status) }.thenBy { it.nextPaymentDate },
        )
    }

    /**
     * Swaps the transaction holder name for the account holder's LEGAL name when the
     * counterparty is the user's own account: OBP puts the LOGIN USERNAME in
     * `other_account.holder.name` for self-transfers (and obfuscates the account id, so a
     * per-account lookup is impossible) — the holder's real name is the user's customer
     * record at this bank. External counterparties keep their transaction holder name.
     */
    private suspend fun resolveHolderNames(bankId: String, orders: List<StandingOrder>): List<StandingOrder> {
        val username = if (orders.any { it.counterpartyName.isNotBlank() }) {
            profileRepository.current().getOrNull()?.username.orEmpty()
        } else {
            ""
        }
        val legalName = if (username.isBlank()) {
            ""
        } else {
            customersRepository.currentUserCustomers().getOrNull()
                ?.firstOrNull { it.bankId == bankId }
                ?.legalName
                .orEmpty()
        }
        return if (legalName.isBlank()) {
            orders
        } else {
            orders.map { order ->
                if (order.counterpartyName == username) order.copy(counterpartyName = legalName) else order
            }
        }
    }

    override suspend fun create(
        bankId: String,
        accountId: String,
        name: String,
        request: CreateStandingOrderRequest,
    ): Result<StandingOrder> =
        api.createStandingOrder(bankId.ifBlank { config.bankId }, accountId, request).toResult().map { response ->
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
