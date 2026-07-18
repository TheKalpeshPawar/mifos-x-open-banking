/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.model.pisp.domesticStandingOrder.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MandateRelatedInformation(
    @SerialName("Frequency")
    val frequency: Frequency? = null,
    @SerialName("FirstPaymentDateTime")
    val firstPaymentDateTime: String? = null,
    @SerialName("FinalPaymentDateTime")
    val finalPaymentDateTime: String? = null,
)
