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

/** OBP foreign-exchange rate between two currencies. */
@Serializable
data class FxRate(
    @SerialName("bank_id") val bankId: String = "",
    @SerialName("from_currency_code") val fromCurrencyCode: String = "",
    @SerialName("to_currency_code") val toCurrencyCode: String = "",
    @SerialName("conversion_value") val conversionValue: Double = 0.0,
    @SerialName("inverse_conversion_value") val inverseConversionValue: Double = 0.0,
    @SerialName("effective_date") val effectiveDate: String = "",
)
