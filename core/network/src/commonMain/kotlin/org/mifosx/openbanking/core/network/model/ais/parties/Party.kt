/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.model.ais.parties

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Party(
    @SerialName("PartyId")
    val partyId: String? = null,
    @SerialName("PartyNumber")
    val partyNumber: String? = null,
    @SerialName("PartyType")
    val partyType: String? = null,
    @SerialName("Name")
    val name: String? = null,
    @SerialName("FullLegalName")
    val fullLegalName: String? = null,
    @SerialName("LegalStructure")
    val legalStructure: String? = null,
    @SerialName("AccountRole")
    val accountRole: String? = null,
    @SerialName("EmailAddress")
    val emailAddress: String? = null,
    @SerialName("Phone")
    val phone: String? = null,
    @SerialName("Mobile")
    val mobile: String? = null,
    @SerialName("Relationships")
    val relationships: Relationships? = null,
)
