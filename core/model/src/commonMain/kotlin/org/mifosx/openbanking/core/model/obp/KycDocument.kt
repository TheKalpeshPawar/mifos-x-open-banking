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

/** OBP KYC document for a customer. */
@Serializable
data class KycDocument(
    val id: String = "",
    @SerialName("customer_id") val customerId: String = "",
    val type: String = "",
    val number: String = "",
    @SerialName("issue_date") val issueDate: String = "",
    @SerialName("issue_place") val issuePlace: String = "",
    @SerialName("expiry_date") val expiryDate: String = "",
)

@Serializable
data class KycDocumentsResponse(
    @SerialName("documents") val documents: List<KycDocument> = emptyList(),
)

/** Body for submitting a KYC check result. */
@Serializable
data class KycCheckRequest(
    @SerialName("customer_id") val customerId: String,
    val date: String,
    val how: String,
    @SerialName("staff_user_id") val staffUserId: String,
    @SerialName("staff_name") val staffName: String,
    val satisfied: Boolean,
    val comments: String = "",
)
