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

/** OBP account application (field-officer review queue). */
@Serializable
data class AccountApplication(
    @SerialName("account_application_id") val id: String = "",
    @SerialName("product_code") val productCode: String = "",
    @SerialName("date_of_application") val dateOfApplication: String = "",
    val status: String = "",
)

@Serializable
data class AccountApplicationsResponse(
    @SerialName("account_applications") val accountApplications: List<AccountApplication> = emptyList(),
)

/** Body for updating an application's status. */
@Serializable
data class AccountApplicationStatusRequest(
    val status: String,
)
