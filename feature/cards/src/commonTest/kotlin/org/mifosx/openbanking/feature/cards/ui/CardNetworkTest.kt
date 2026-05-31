/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.cards.ui

import org.mifosx.openbanking.core.model.obp.Card
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Guards the card-network resolution that drives logo selection in [CardsScreen].
 *
 * The per-account OBP endpoint (v3.0.0) returns a singular `card_network`, while the
 * current-user endpoint (v7.0.0 — the carousel source) returns a `networks[]` array.
 * [Card.network] must serve both without the screen caring which endpoint produced it.
 */
class CardNetworkTest {

    @Test
    fun prefersSingularCardNetworkWhenPresent() {
        val card = Card(cardNetwork = "VISA", networks = listOf("MASTERCARD"))
        assertEquals("VISA", card.network)
    }

    @Test
    fun fallsBackToFirstNetworkFromArray() {
        val card = Card(cardNetwork = "", networks = listOf("MASTERCARD", "VISA"))
        assertEquals("MASTERCARD", card.network)
    }

    @Test
    fun emptyWhenNeitherProvided() {
        assertEquals("", Card().network)
    }
}
