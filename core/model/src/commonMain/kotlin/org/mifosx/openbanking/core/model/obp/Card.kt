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

/** OBP issued card (physical or virtual). */
@Serializable
data class Card(
    val id: String = "",
    @SerialName("bank_id") val bankId: String = "",
    @SerialName("bank_card_number") val bankCardNumber: String = "",
    @SerialName("name_on_card") val nameOnCard: String = "",
    @SerialName("card_type") val cardType: String = "",
    @SerialName("card_network") val cardNetwork: String = "",
    @SerialName("expires_date") val expiresDate: String = "",
    val allows: List<String> = emptyList(),
    val enabled: Boolean = false,
    val cancelled: Boolean = false,
)

/** Wrapper for the OBP cards-list response (`{ "cards": [...] }`). */
@Serializable
data class CardsResponse(
    val cards: List<Card> = emptyList(),
)
