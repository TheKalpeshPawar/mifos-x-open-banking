/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.model.ais.product

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OBIE `OBTierBand1`: a single credit-interest tier.
 *
 * [bandLimit] arrives pre-formatted by the bank (e.g. `"£1,000"`), so it is displayed verbatim rather
 * than run through the money formatters.
 */
@Serializable
data class TierBand(
    @SerialName("TierValueMinimum")
    val tierValueMinimum: String? = null,
    @SerialName("BandLimit")
    val bandLimit: String? = null,
    /** Annual Equivalent Rate as a decimal percentage, e.g. `"0.15"`. */
    @SerialName("AER")
    val aer: String? = null,
    @SerialName("ApplicationFrequency")
    val applicationFrequency: String? = null,
)
