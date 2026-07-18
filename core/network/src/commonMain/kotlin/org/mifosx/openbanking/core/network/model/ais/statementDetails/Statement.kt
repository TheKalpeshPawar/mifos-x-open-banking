/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.model.ais.statementDetails

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Statement(
    @SerialName("AccountId")
    val accountId: String? = null,
    @SerialName("StatementId")
    val statementId: String? = null,
    @SerialName("StatementReference")
    val statementReference: String? = null,
    @SerialName("Type")
    val type: String? = null,
    @SerialName("StartDateTime")
    val startDateTime: String? = null,
    @SerialName("EndDateTime")
    val endDateTime: String? = null,
    @SerialName("CreationDateTime")
    val creationDateTime: String? = null,
    @SerialName("StatementDateTime")
    val statementDateTime: List<StatementDateTime>? = null,
    @SerialName("StatementValue")
    val statementValue: List<StatementValue>? = null,
    @SerialName("StatementAmount")
    val statementAmount: List<StatementAmount>? = null,
    @SerialName("StatementDescription")
    val statementDescription: List<String>? = null,
    @SerialName("StatementBenefit")
    val statementBenefit: List<StatementBenefit>? = null,
    @SerialName("StatementFee")
    val statementFee: List<StatementFee>? = null,
    @SerialName("StatementInterest")
    val statementInterest: List<StatementInterest>? = null,
    @SerialName("StatementRate")
    val statementRate: List<StatementRate>? = null,
)
