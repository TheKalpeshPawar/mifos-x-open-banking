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

/** OBP counterparty / beneficiary linked to an account. */
@Serializable
data class Counterparty(
    @SerialName("counterparty_id") val counterpartyId: String = "",
    val name: String = "",
    val description: String = "",
    @SerialName("other_bank_routing_scheme") val otherBankRoutingScheme: String = "",
    @SerialName("other_bank_routing_address") val otherBankRoutingAddress: String = "",
    @SerialName("other_account_routing_scheme") val otherAccountRoutingScheme: String = "",
    @SerialName("other_account_routing_address") val otherAccountRoutingAddress: String = "",
    @SerialName("is_beneficiary") val isBeneficiary: Boolean = false,
)

/** Wrapper for the OBP counterparties-list response (`{ "counterparties": [...] }`). */
@Serializable
data class CounterpartiesResponse(
    val counterparties: List<Counterparty> = emptyList(),
)
