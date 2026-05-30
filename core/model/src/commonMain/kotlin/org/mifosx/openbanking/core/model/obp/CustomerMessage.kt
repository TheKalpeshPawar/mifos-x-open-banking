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

/** OBP customer message. */
@Serializable
data class CustomerMessage(
    val id: String = "",
    val date: String = "",
    val message: String = "",
    @SerialName("from_department") val fromDepartment: String = "",
    @SerialName("from_person") val fromPerson: String = "",
)

@Serializable
data class CustomerMessagesResponse(
    val messages: List<CustomerMessage> = emptyList(),
)

/** Body for sending a message to a customer. */
@Serializable
data class CustomerMessageRequest(
    val message: String,
    @SerialName("from_department") val fromDepartment: String,
    @SerialName("from_person") val fromPerson: String,
)
