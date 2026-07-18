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

/** OBP bank agent (field-officer agent-registration). */
@Serializable
data class Agent(
    @SerialName("agent_id") val agentId: String = "",
    @SerialName("legal_name") val legalName: String = "",
    @SerialName("mobile_phone_number") val mobilePhoneNumber: String = "",
    @SerialName("is_pending_agent") val isPendingAgent: Boolean = false,
    @SerialName("is_confirmed_agent") val isConfirmedAgent: Boolean = false,
)

/** Body for registering the field officer as an OBP agent. */
@Serializable
data class AgentRequest(
    @SerialName("legal_name") val legalName: String,
    @SerialName("mobile_phone_number") val mobilePhoneNumber: String,
    @SerialName("agent_number") val agentNumber: String,
    val currency: String,
    @SerialName("supported_services") val supportedServices: List<String> = emptyList(),
    @SerialName("commission_rate") val commissionRate: String = "",
)
