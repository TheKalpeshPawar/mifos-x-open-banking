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

/**
 * OBP ATM location. [isAccessible] and [hasDepositCapability] arrive as the strings
 * `"true"`/`"false"` on the sandbox; read them through [accessible] / [acceptsDeposits].
 * The per-day opening hours (`monday`…`sunday`) are sibling fields on the ATM object.
 */
@Serializable
data class Atm(
    val id: String = "",
    @SerialName("bank_id") val bankId: String = "",
    val name: String = "",
    val address: PostalAddress = PostalAddress(),
    val location: GeoLocation = GeoLocation(),
    @SerialName("more_info") val moreInfo: String = "",
    @SerialName("is_accessible") val isAccessible: String = "",
    @SerialName("has_deposit_capability") val hasDepositCapability: String = "",
    val monday: OpeningHours? = null,
    val tuesday: OpeningHours? = null,
    val wednesday: OpeningHours? = null,
    val thursday: OpeningHours? = null,
    val friday: OpeningHours? = null,
    val saturday: OpeningHours? = null,
    val sunday: OpeningHours? = null,
) {
    val accessible: Boolean get() = isAccessible.equals("true", ignoreCase = true)
    val acceptsDeposits: Boolean get() = hasDepositCapability.equals("true", ignoreCase = true)
    val week: List<OpeningHours> get() = listOfNotNull(monday, tuesday, wednesday, thursday, friday, saturday, sunday)
}

@Serializable
data class OpeningHours(
    @SerialName("opening_time") val openingTime: String = "",
    @SerialName("closing_time") val closingTime: String = "",
)

@Serializable
data class PostalAddress(
    @SerialName("line_1") val line1: String = "",
    @SerialName("line_2") val line2: String = "",
    val city: String = "",
    val county: String = "",
    @SerialName("postcode") val postCode: String = "",
    @SerialName("country_code") val countryCode: String = "",
)

@Serializable
data class GeoLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
)

@Serializable
data class AtmsResponse(
    val atms: List<Atm> = emptyList(),
)
