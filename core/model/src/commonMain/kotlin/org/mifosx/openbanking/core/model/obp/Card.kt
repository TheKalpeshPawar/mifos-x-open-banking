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
    // Per-account endpoint (v3.0.0) returns a singular card_network; the current-user
    // endpoint (v7.0.0) returns a networks[] array. Both are kept so the same model
    // serves both responses — prefer [network] for display.
    @SerialName("card_network") val cardNetwork: String = "",
    val networks: List<String> = emptyList(),
    @SerialName("expires_date") val expiresDate: String = "",
    val allows: List<String> = emptyList(),
    val enabled: Boolean = false,
    val cancelled: Boolean = false,
    @SerialName("on_hot_list") val onHotList: Boolean = false,
    // Present on GET /obp/v7.0.0/cards — the owning account, used to fetch the card's
    // transactions. Absent on the per-account endpoint (defaults to empty).
    val account: CardAccountRef = CardAccountRef(),
) {
    /** Card network for logo selection — falls back from the array to the singular field. */
    val network: String get() = cardNetwork.ifBlank { networks.firstOrNull().orEmpty() }
}

/** Embedded account summary returned inside a [Card] from GET /obp/v7.0.0/cards. */
@Serializable
data class CardAccountRef(
    val id: String = "",
    val label: String = "",
    @SerialName("bank_id") val bankId: String = "",
)

/** Wrapper for the OBP cards-list response (`{ "cards": [...] }`). */
@Serializable
data class CardsResponse(
    val cards: List<Card> = emptyList(),
)
