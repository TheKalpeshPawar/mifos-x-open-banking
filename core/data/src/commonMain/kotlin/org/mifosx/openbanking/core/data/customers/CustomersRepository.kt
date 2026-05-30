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

import org.mifosx.openbanking.core.data.obp.toResult
import org.mifosx.openbanking.core.model.obp.Customer
import org.mifosx.openbanking.core.model.obp.CustomerRequest
import org.mifosx.openbanking.core.network.api.CustomersApi
import org.mifosx.openbanking.core.network.obp.ObpConfig

/** Read + write access to OBP customers (field-officer surface). */
interface CustomersRepository {
    suspend fun list(): Result<List<Customer>>
    suspend fun get(customerId: String): Result<Customer>
    suspend fun create(request: CustomerRequest): Result<Customer>
    suspend fun update(customerId: String, request: CustomerRequest): Result<Customer>
}

class CustomersRepositoryImpl(
    private val api: CustomersApi,
    private val config: ObpConfig,
) : CustomersRepository {

    override suspend fun list(): Result<List<Customer>> =
        api.listCustomers(config.bankId).toResult().map { it.customers }

    override suspend fun get(customerId: String): Result<Customer> =
        api.getCustomer(config.bankId, customerId).toResult()

    override suspend fun create(request: CustomerRequest): Result<Customer> =
        api.createCustomer(config.bankId, request).toResult()

    override suspend fun update(customerId: String, request: CustomerRequest): Result<Customer> =
        api.updateCustomer(config.bankId, customerId, request).toResult()
}
