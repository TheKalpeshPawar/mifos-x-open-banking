/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.model.ais.party

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OBIE `OBPostalAddress6`, carried by `OBParty2.Address` as an array.
 *
 * Every field is optional in the specification: a bank may send structured components
 * ([streetName], [buildingNumber], [townName], [postCode]) or an unstructured
 * [addressLine] list, or a mixture. Consumers should tolerate any combination.
 *
 * The HSBC UK Personal sandbox does not return `Address` on the party endpoints, so in
 * that environment this decodes to `null` and the profile screen's address row is hidden.
 */
@Serializable
data class PartyAddress(
    @SerialName("AddressType")
    val addressType: String? = null,
    @SerialName("StreetName")
    val streetName: String? = null,
    @SerialName("BuildingNumber")
    val buildingNumber: String? = null,
    @SerialName("PostCode")
    val postCode: String? = null,
    @SerialName("TownName")
    val townName: String? = null,
    @SerialName("Country")
    val country: String? = null,
    @SerialName("AddressLine")
    val addressLine: List<String>? = null,
)
