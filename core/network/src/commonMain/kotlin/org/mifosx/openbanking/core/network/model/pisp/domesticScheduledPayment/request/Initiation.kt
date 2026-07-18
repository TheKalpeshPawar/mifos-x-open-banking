/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.model.pisp.domesticScheduledPayment.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Initiation(
    @SerialName("InstructionIdentification")
    val instructionIdentification: String? = null,
    @SerialName("EndToEndIdentification")
    val endToEndIdentification: String? = null,
    @SerialName("LocalInstrument")
    val localInstrument: String? = null,
    @SerialName("RequestedExecutionDateTime")
    val requestedExecutionDateTime: String? = null,
    @SerialName("InstructedAmount")
    val instructedAmount: InstructedAmount? = null,
    @SerialName("DebtorAccount")
    val debtorAccount: DebtorAccount? = null,
    @SerialName("CreditorAccount")
    val creditorAccount: CreditorAccount? = null,
    @SerialName("CreditorPostalAddress")
    val creditorPostalAddress: CreditorPostalAddress? = null,
    @SerialName("RemittanceInformation")
    val remittanceInformation: RemittanceInformation? = null,
)
