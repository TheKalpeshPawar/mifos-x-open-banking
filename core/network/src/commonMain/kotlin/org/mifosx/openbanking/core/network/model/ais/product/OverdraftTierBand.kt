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

/** OBIE `OBOverdraftTierBand1`: a single overdraft tier, Arranged or Unarranged. */
@Serializable
data class OverdraftTierBand(
    @SerialName("TierValueMinimum")
    val tierValueMinimum: String? = null,
    /** `Arranged` or `Unarranged`. */
    @SerialName("OverdraftType")
    val overdraftType: String? = null,
    /** Effective Annual Rate as a decimal percentage, e.g. `"39.9"`. */
    @SerialName("EAR")
    val ear: String? = null,
)
