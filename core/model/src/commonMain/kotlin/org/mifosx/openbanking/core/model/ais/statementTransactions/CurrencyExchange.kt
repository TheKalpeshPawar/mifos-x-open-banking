/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.model.ais.statementTransactions

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CurrencyExchange(
    @SerialName("SourceCurrency")
    val sourceCurrency: String? = null,
    @SerialName("TargetCurrency")
    val targetCurrency: String? = null,
    @SerialName("UnitCurrency")
    val unitCurrency: String? = null,
    @SerialName("ExchangeRate")
    val exchangeRate: Double? = null,
    @SerialName("ContractIdentification")
    val contractIdentification: String? = null,
    @SerialName("QuotationDate")
    val quotationDate: String? = null,
    @SerialName("InstructedAmount")
    val instructedAmount: Amount? = null,
)
