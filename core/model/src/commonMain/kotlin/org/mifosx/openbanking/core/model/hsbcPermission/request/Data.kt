/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.model.hsbcPermission.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Data(
    @SerialName("ExpirationDateTime")
    val expirationDateTime: String,
    @SerialName("Permissions")
    val permissions: List<String>,
    @SerialName("TransactionFromDateTime")
    val transactionFromDateTime: String,
    @SerialName("TransactionToDateTime")
    val transactionToDateTime: String,
)
