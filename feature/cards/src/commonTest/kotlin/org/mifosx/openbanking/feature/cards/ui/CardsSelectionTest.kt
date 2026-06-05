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

import org.mifosx.openbanking.core.model.obp.Transaction
import org.mifosx.openbanking.core.model.obp.TransactionAttribute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies the per-card selection logic that scopes the cards screen's transaction list to
 * the swiped-to card. Two cards on one account must yield disjoint lists — the case the
 * account-level transaction feed alone cannot separate.
 */
class CardsSelectionTest {

    private fun tx(id: String, card: String?, type: String? = null): Transaction = Transaction(
        transactionId = id,
        transactionAttributes = buildList {
            if (card != null) add(TransactionAttribute("CARD_ID", "STRING", card))
            if (type != null) add(TransactionAttribute("TXN_TYPE", "STRING", type))
        },
    )

    @Test
    fun forCard_returnsOnlyThatCardsTransactionsInOrder() {
        val cardA = "4000123456789010"
        val cardB = "4000123456789028"
        val all = listOf(
            tx("1", cardA, "POS"),
            tx("2", cardB, "ATM"),
            tx("3", cardA, "ECOM"),
            tx("4", card = null),
            tx("5", cardB, "POS"),
        )
        assertEquals(listOf("1", "3"), all.forCard(cardA, 10).map { it.txId })
        assertEquals(listOf("2", "5"), all.forCard(cardB, 10).map { it.txId })
    }

    @Test
    fun forCard_respectsLimit() {
        val all = (1..20).map { tx(it.toString(), "A") }
        assertEquals(5, all.forCard("A", 5).size)
        assertEquals(listOf("1", "2", "3", "4", "5"), all.forCard("A", 5).map { it.txId })
    }

    @Test
    fun forCard_emptyWhenNoTransactionsMatch() {
        val all = listOf(tx("1", "A"), tx("2", null))
        assertTrue(all.forCard("Z", 10).isEmpty())
    }
}
