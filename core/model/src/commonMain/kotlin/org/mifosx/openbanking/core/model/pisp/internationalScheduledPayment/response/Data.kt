/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.model.pisp.internationalScheduledPayment.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Data(
    @SerialName("ConsentId")
    val consentId: String? = null,
    @SerialName("Status")
    val status: String? = null,
    @SerialName("CreationDateTime")
    val creationDateTime: String? = null,
    @SerialName("StatusUpdateDateTime")
    val statusUpdateDateTime: String? = null,
    @SerialName("Permission")
    val permission: String? = null,
    @SerialName("ReadRefundAccount")
    val readRefundAccount: String? = null,
    @SerialName("Initiation")
    val initiation: Initiation? = null,
    @SerialName("Authorisation")
    val authorisation: Authorisation? = null,
    @SerialName("SCASupportData")
    val scaSupportData: SCASupportData? = null,
    @SerialName("InternationalScheduledPaymentId")
    val internationalScheduledPaymentId: String? = null,
)
