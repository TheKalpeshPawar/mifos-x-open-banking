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
data class SCASupportData(
    @SerialName("RequestedSCAExemptionType")
    val requestedSCAExemptionType: String? = null,
    @SerialName("AppliedAuthenticationApproach")
    val appliedAuthenticationApproach: String? = null,
    @SerialName("ReferencePaymentOrderId")
    val referencePaymentOrderId: String? = null,
)
