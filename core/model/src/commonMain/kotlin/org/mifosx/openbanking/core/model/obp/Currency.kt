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

/** One currency supported by a bank (ISO 4217 alphanumeric code). */
@Serializable
data class BankCurrency(
    @SerialName("alphanumeric_code") val alphanumericCode: String = "",
)

/** Wrapper for the OBP bank-currencies response (`{ "currencies": [...] }`). */
@Serializable
data class CurrenciesResponse(
    val currencies: List<BankCurrency> = emptyList(),
)
