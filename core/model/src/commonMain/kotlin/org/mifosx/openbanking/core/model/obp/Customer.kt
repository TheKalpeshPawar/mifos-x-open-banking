/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.model.obp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** OBP customer (field-officer surface). */
@Serializable
data class Customer(
    @SerialName("customer_id") val customerId: String = "",
    @SerialName("customer_number") val customerNumber: String = "",
    @SerialName("legal_name") val legalName: String = "",
    @SerialName("mobile_phone_number") val mobilePhoneNumber: String = "",
    val email: String = "",
    @SerialName("date_of_birth") val dateOfBirth: String = "",
    @SerialName("kyc_status") val kycStatus: Boolean = false,
)

@Serializable
data class CustomersResponse(
    val customers: List<Customer> = emptyList(),
)

/**
 * Link between a customer and an account (OBP customer-account-links). `relationshipType` is e.g.
 * "Owner" or "Director" — an account can have several. The "Owner" link names the account holder.
 */
@Serializable
data class CustomerAccountLink(
    @SerialName("customer_id") val customerId: String = "",
    @SerialName("relationship_type") val relationshipType: String = "",
)

@Serializable
data class CustomerAccountLinksResponse(
    val links: List<CustomerAccountLink> = emptyList(),
)

/** Body for creating / updating a customer. */
@Serializable
data class CustomerRequest(
    @SerialName("legal_name") val legalName: String,
    @SerialName("mobile_phone_number") val mobilePhoneNumber: String,
    val email: String,
    @SerialName("date_of_birth") val dateOfBirth: String = "",
)
