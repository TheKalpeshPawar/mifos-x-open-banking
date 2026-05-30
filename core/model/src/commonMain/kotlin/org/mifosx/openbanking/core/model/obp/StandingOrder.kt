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

/** OBP standing order (recurring outgoing payment). */
@Serializable
data class StandingOrder(
    @SerialName("standing_order_id") val id: String = "",
    @SerialName("bank_id") val bankId: String = "",
    @SerialName("account_id") val accountId: String = "",
    @SerialName("counterparty_name") val counterpartyName: String = "",
    @SerialName("amount_value") val amountValue: String = "",
    @SerialName("amount_currency") val amountCurrency: String = "",
    @SerialName("frequency") val frequency: String = "",
    @SerialName("start_date") val startDate: String = "",
    @SerialName("next_payment_date") val nextPaymentDate: String = "",
    @SerialName("final_date") val finalDate: String = "",
    val status: String = "",
    val active: Boolean = false,
)

@Serializable
data class StandingOrdersResponse(
    @SerialName("standing_orders") val standingOrders: List<StandingOrder> = emptyList(),
)
