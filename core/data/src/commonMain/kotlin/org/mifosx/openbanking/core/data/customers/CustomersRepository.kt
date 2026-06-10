/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.customers

import kotlinx.coroutines.CoroutineScope
import org.mifosx.openbanking.core.data.infra.NetworkMonitor
import org.mifosx.openbanking.core.data.obp.toResult
import org.mifosx.openbanking.core.model.obp.Customer
import org.mifosx.openbanking.core.model.obp.CustomerRequest
import org.mifosx.openbanking.core.network.api.CustomersApi
import org.mifosx.openbanking.core.network.obp.ObpConfig
import org.mobilenativefoundation.store.store5.Store
import template.core.base.store.infra.FetchedAtRepository
import template.core.base.store.screen.ScreenDataStream
import template.core.base.store.screen.asScreenStream

/** Read + write access to OBP customers (field-officer surface). */
interface CustomersRepository {
    /** Durable, offline-first stream of the customer list (cache-then-network). */
    fun customersStream(scope: CoroutineScope): ScreenDataStream<List<Customer>>

    suspend fun list(): Result<List<Customer>>

    /** Customer records belonging to the logged-in user (any bank) — consumer surface. */
    suspend fun currentUserCustomers(): Result<List<Customer>>

    suspend fun get(customerId: String): Result<Customer>
    suspend fun create(request: CustomerRequest): Result<Customer>
    suspend fun update(customerId: String, request: CustomerRequest): Result<Customer>

    /**
     * Legal name of the account holder for the account at ([bankId], [accountId]): resolves the
     * customer-account-links (preferring the "Owner" relationship) then the customer's legal name.
     * Returns a blank string when the account has no linked customer.
     */
    suspend fun accountHolderName(bankId: String, accountId: String): Result<String>
}

class CustomersRepositoryImpl(
    private val api: CustomersApi,
    private val config: ObpConfig,
    private val customersStore: Store<Unit, List<Customer>>,
    private val networkMonitor: NetworkMonitor,
    private val fetchedAtRepository: FetchedAtRepository,
) : CustomersRepository {

    override fun customersStream(scope: CoroutineScope): ScreenDataStream<List<Customer>> =
        customersStore.asScreenStream(
            key = Unit,
            networkMonitor = networkMonitor,
            fetchedAtRepository = fetchedAtRepository,
            cacheKey = "customers",
            scope = scope,
            isEmpty = { it.isEmpty() },
        )

    override suspend fun list(): Result<List<Customer>> =
        api.listCustomers(config.bankId).toResult().map { it.customers }

    override suspend fun currentUserCustomers(): Result<List<Customer>> =
        api.currentUserCustomers().toResult().map { it.customers }

    override suspend fun get(customerId: String): Result<Customer> =
        api.getCustomer(config.bankId, customerId).toResult()

    override suspend fun create(request: CustomerRequest): Result<Customer> =
        api.createCustomer(config.bankId, request).toResult()

    override suspend fun update(customerId: String, request: CustomerRequest): Result<Customer> =
        api.updateCustomer(config.bankId, customerId, request).toResult()

    override suspend fun accountHolderName(bankId: String, accountId: String): Result<String> {
        val resolvedBank = bankId.ifBlank { config.bankId }
        return api.customerAccountLinks(resolvedBank, accountId).toResult().mapCatching { response ->
            val link = response.links.firstOrNull { it.relationshipType.equals("Owner", ignoreCase = true) }
                ?: response.links.firstOrNull()
            if (link == null) "" else api.getCustomer(resolvedBank, link.customerId).toResult().getOrThrow().legalName
        }
    }
}
