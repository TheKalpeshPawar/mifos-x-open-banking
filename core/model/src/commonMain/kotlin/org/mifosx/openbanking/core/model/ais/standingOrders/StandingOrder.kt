/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.model.ais.standingOrders

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StandingOrder(
    @SerialName("AccountId")
    val accountId: String? = null,
    @SerialName("StandingOrderStatusCode")
    val standingOrderStatusCode: String? = null,
    @SerialName("NextPaymentDateTime")
    val nextPaymentDateTime: String? = null,
    @SerialName("NextPaymentAmount")
    val nextPaymentAmount: NextPaymentAmount? = null,
    @SerialName("CreditorAccount")
    val creditorAccount: CreditorAccount? = null,
    @SerialName("MandateRelatedInformation")
    val mandateRelatedInformation: MandateRelatedInformation? = null,
    @SerialName("RemittanceInformation")
    val remittanceInformation: RemittanceInformation? = null,
)
