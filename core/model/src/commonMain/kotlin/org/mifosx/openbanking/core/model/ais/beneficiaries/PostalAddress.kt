/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.model.ais.beneficiaries

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostalAddress(
    @SerialName("AddressType")
    val addressType: String? = null,
    @SerialName("BuildingNumber")
    val buildingNumber: String? = null,
    @SerialName("Department")
    val department: String? = null,
    @SerialName("SubDepartment")
    val subDepartment: String? = null,
    @SerialName("StreetName")
    val streetName: String? = null,
    @SerialName("TownName")
    val townName: String? = null,
    @SerialName("PostCode")
    val postCode: String? = null,
    @SerialName("CountrySubDivision")
    val countrySubDivision: String? = null,
    @SerialName("Country")
    val country: String? = null,
)
