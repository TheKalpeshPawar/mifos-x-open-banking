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
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class StatementFee(
    @SerialName("Description")
    val description: String? = null,
    @SerialName("CreditDebitIndicator")
    val creditDebitIndicator: String? = null,
    @SerialName("Type")
    val type: String? = null,
    @SerialName("RateType")
    val rateType: String? = null,
    @SerialName("Frequency")
    val frequency: String? = null,
    // HSBC sandbox returns this as an unquoted number (e.g. "Rate": 1) despite the
    // swagger string pattern, so JsonPrimitive tolerates both a bare number and a string.
    @SerialName("Rate")
    val rate: JsonPrimitive? = null,
    @SerialName("Amount")
    val amount: Amount? = null,
)
