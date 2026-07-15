/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.model.hsbcPermission.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Data(
    @SerialName("ConsentId")
    val consentId: String,
    @SerialName("CreationDateTime")
    val creationDateTime: String,
    @SerialName("ExpirationDateTime")
    val expirationDateTime: String,
    @SerialName("Permissions")
    val permissions: List<String>,
    @SerialName("Status")
    val status: String,
    @SerialName("StatusUpdateDateTime")
    val statusUpdateDateTime: String,
    @SerialName("TransactionFromDateTime")
    val transactionFromDateTime: String,
    @SerialName("TransactionToDateTime")
    val transactionToDateTime: String,
)
