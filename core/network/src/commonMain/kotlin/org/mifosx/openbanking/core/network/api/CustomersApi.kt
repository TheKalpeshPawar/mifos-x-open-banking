/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.network.api

import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.PUT
import de.jensklingenberg.ktorfit.http.Path
import org.mifosx.openbanking.core.model.obp.Customer
import org.mifosx.openbanking.core.model.obp.CustomerAccountLinksResponse
import org.mifosx.openbanking.core.model.obp.CustomerRequest
import org.mifosx.openbanking.core.model.obp.CustomersResponse
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

/** OBP customer endpoints (field-officer surface). */
interface CustomersApi {

    @GET("v3.0.0/banks/{bankId}/customers")
    suspend fun listCustomers(
        @Path("bankId") bankId: String,
    ): NetworkResult<CustomersResponse, NetworkError>

    /** Customer records belonging to the LOGGED-IN user (any bank) — consumer surface. */
    @GET("v4.0.0/users/current/customers")
    suspend fun currentUserCustomers(): NetworkResult<CustomersResponse, NetworkError>

    // v5.1.0: v3.0.0 has no get-customer-by-id route (returns OBP-10404).
    @GET("v5.1.0/banks/{bankId}/customers/{customerId}")
    suspend fun getCustomer(
        @Path("bankId") bankId: String,
        @Path("customerId") customerId: String,
    ): NetworkResult<Customer, NetworkError>

    /** Customer⇄account links for an account; the "Owner" link names the account holder. */
    @GET("v5.0.0/banks/{bankId}/accounts/{accountId}/customer-account-links")
    suspend fun customerAccountLinks(
        @Path("bankId") bankId: String,
        @Path("accountId") accountId: String,
    ): NetworkResult<CustomerAccountLinksResponse, NetworkError>

    @POST("v3.0.0/banks/{bankId}/customers")
    suspend fun createCustomer(
        @Path("bankId") bankId: String,
        @Body request: CustomerRequest,
    ): NetworkResult<Customer, NetworkError>

    @PUT("v3.0.0/banks/{bankId}/customers/{customerId}")
    suspend fun updateCustomer(
        @Path("bankId") bankId: String,
        @Path("customerId") customerId: String,
        @Body request: CustomerRequest,
    ): NetworkResult<Customer, NetworkError>
}
