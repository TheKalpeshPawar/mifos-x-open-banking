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

/**
 * OBP counterparty / beneficiary linked to an account. Field names + types verified
 * against the live OBP v4.0.0 `GET .../owner/counterparties` response (2026-06-03).
 */
@Serializable
data class Counterparty(
    @SerialName("counterparty_id") val counterpartyId: String = "",
    val name: String = "",
    val description: String = "",
    val currency: String = "",
    @SerialName("this_bank_id") val thisBankId: String = "",
    @SerialName("this_account_id") val thisAccountId: String = "",
    @SerialName("this_view_id") val thisViewId: String = "",
    @SerialName("other_bank_routing_scheme") val otherBankRoutingScheme: String = "",
    @SerialName("other_bank_routing_address") val otherBankRoutingAddress: String = "",
    @SerialName("other_branch_routing_scheme") val otherBranchRoutingScheme: String = "",
    @SerialName("other_branch_routing_address") val otherBranchRoutingAddress: String = "",
    @SerialName("other_account_routing_scheme") val otherAccountRoutingScheme: String = "",
    @SerialName("other_account_routing_address") val otherAccountRoutingAddress: String = "",
    @SerialName("other_account_secondary_routing_scheme") val otherAccountSecondaryRoutingScheme: String = "",
    @SerialName("other_account_secondary_routing_address") val otherAccountSecondaryRoutingAddress: String = "",
    @SerialName("is_beneficiary") val isBeneficiary: Boolean = false,
    @SerialName("created_by_user_id") val createdByUserId: String = "",
    val bespoke: List<Bespoke> = emptyList(),
)

/** Arbitrary key/value pair OBP allows on a counterparty. */
@Serializable
data class Bespoke(
    val key: String = "",
    val value: String = "",
)

/** Wrapper for the OBP counterparties-list response (`{ "counterparties": [...] }`). */
@Serializable
data class CounterpartiesResponse(
    val counterparties: List<Counterparty> = emptyList(),
)

/**
 * Request body for OBP v4.0.0 `POST .../owner/counterparties` (Add Beneficiary).
 * All fields are required by OBP; routing/secondary fields may be empty strings.
 */
@Serializable
data class CreateCounterpartyRequest(
    val name: String,
    val description: String = "",
    val currency: String,
    @SerialName("other_account_routing_scheme") val otherAccountRoutingScheme: String,
    @SerialName("other_account_routing_address") val otherAccountRoutingAddress: String,
    @SerialName("other_bank_routing_scheme") val otherBankRoutingScheme: String,
    @SerialName("other_bank_routing_address") val otherBankRoutingAddress: String,
    @SerialName("other_branch_routing_scheme") val otherBranchRoutingScheme: String = "",
    @SerialName("other_branch_routing_address") val otherBranchRoutingAddress: String = "",
    @SerialName("other_account_secondary_routing_scheme") val otherAccountSecondaryRoutingScheme: String = "",
    @SerialName("other_account_secondary_routing_address") val otherAccountSecondaryRoutingAddress: String = "",
    @SerialName("is_beneficiary") val isBeneficiary: Boolean = true,
    val bespoke: List<Bespoke> = emptyList(),
)
