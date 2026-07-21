/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.model.ais.accountDetails

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Account(
    @SerialName("AccountId")
    val accountId: String? = null,
    @SerialName("Currency")
    val currency: String? = null,
    @SerialName("AccountCategory")
    val accountCategory: String? = null,
    @SerialName("AccountTypeCode")
    val accountTypeCode: String? = null,
    @SerialName("AccountSubType")
    val accountSubType: String? = null,
    @SerialName("Nickname")
    val nickname: String? = null,
    @SerialName("StatusUpdateDateTime")
    val statusUpdateDateTime: String? = null,
    @SerialName("Description")
    val description: String? = null,
    @SerialName("Servicer")
    val servicer: Servicer? = null,
    @SerialName("SchemeName")
    val schemeName: String? = null,
    @SerialName("Identification")
    val identification: String? = null,
    @SerialName("Name")
    val name: String? = null,
    @SerialName("Account")
    val account: List<Account>? = null,
)
