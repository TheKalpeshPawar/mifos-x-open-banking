/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.model.ais.directDebits

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DirectDebit(
    @SerialName("AccountId")
    val accountId: String? = null,
    @SerialName("DirectDebitId")
    val directDebitId: String? = null,
    @SerialName("MandateRelatedInformation")
    val mandateRelatedInformation: MandateRelatedInformation? = null,
    @SerialName("DirectDebitStatusCode")
    val directDebitStatusCode: String? = null,
    @SerialName("Name")
    val name: String? = null,
    @SerialName("PreviousPaymentDateTime")
    val previousPaymentDateTime: String? = null,
    @SerialName("PreviousPaymentAmount")
    val previousPaymentAmount: PreviousPaymentAmount? = null,
)
