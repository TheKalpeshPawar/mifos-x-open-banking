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

/** OBP ATM location. */
@Serializable
data class Atm(
    val id: String = "",
    @SerialName("bank_id") val bankId: String = "",
    val name: String = "",
    val address: PostalAddress = PostalAddress(),
    val location: GeoLocation = GeoLocation(),
)

@Serializable
data class PostalAddress(
    val line1: String = "",
    val line2: String = "",
    val city: String = "",
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
