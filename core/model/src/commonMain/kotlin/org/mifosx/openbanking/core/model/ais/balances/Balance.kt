/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.model.ais.balances

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Balance(
    @SerialName("AccountId")
    val accountId: String? = null,
    @SerialName("CreditDebitIndicator")
    val creditDebitIndicator: String? = null,
    @SerialName("Type")
    val type: String? = null,
    @SerialName("DateTime")
    val dateTime: String? = null,
    @SerialName("Amount")
    val amount: Amount? = null,
)
