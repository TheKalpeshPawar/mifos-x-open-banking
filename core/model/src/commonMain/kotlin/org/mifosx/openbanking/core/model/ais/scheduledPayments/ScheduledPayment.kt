/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.model.ais.scheduledPayments

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScheduledPayment(
    @SerialName("AccountId")
    val accountId: String? = null,
    @SerialName("ScheduledPaymentId")
    val scheduledPaymentId: String? = null,
    @SerialName("ScheduledPaymentDateTime")
    val scheduledPaymentDateTime: String? = null,
    @SerialName("ScheduledType")
    val scheduledType: String? = null,
    @SerialName("Reference")
    val reference: String? = null,
    @SerialName("DebtorReference")
    val debtorReference: String? = null,
    @SerialName("InstructedAmount")
    val instructedAmount: InstructedAmount? = null,
    @SerialName("CreditorAccount")
    val creditorAccount: CreditorAccount? = null,
    @SerialName("CreditorAgent")
    val creditorAgent: CreditorAgent? = null,
)
