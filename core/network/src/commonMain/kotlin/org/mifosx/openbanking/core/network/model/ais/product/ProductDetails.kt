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

/** OBIE `ProductDetails`: the headline charge and the marketing feature list for a product. */
@Serializable
data class ProductDetails(
    /** Decimal major units, e.g. `"0.00"` for a fee-free account. */
    @SerialName("MonthlyMaximumCharge")
    val monthlyMaximumCharge: String? = null,
    @SerialName("Features")
    val features: List<String>? = null,
)
