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

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TransactionAttributesTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun decodesV6TransactionIdAndInlineAttributes() {
        // Shape returned by GET /obp/v6.0.0/.../transactions — id is `transaction_id`,
        // attributes are inline under `transaction_attributes`.
        val raw = """
            {
              "transaction_id": "t-77",
              "details": { "type": "COUNTERPARTY", "description": "Costa Coffee",
                           "value": { "currency": "GBP", "amount": "-3.50" } },
              "transaction_attributes": [
                { "name": "CARD_ID", "type": "STRING", "value": "4000123456789010" },
                { "name": "TXN_TYPE", "type": "STRING", "value": "POS" }
              ]
            }
        """.trimIndent()
        val tx = json.decodeFromString<Transaction>(raw)
        assertEquals("t-77", tx.txId)
        assertEquals("4000123456789010", tx.cardId)
        assertEquals("POS", tx.txnTypeCode)
    }

    @Test
    fun v3IdFallsBack_andUntaggedTransactionHasNullAttributes() {
        // v3.0.0 core shape — id is `id`, no attributes.
        val tx = json.decodeFromString<Transaction>("""{ "id": "v3-1", "details": { "type": "SEPA" } }""")
        assertEquals("v3-1", tx.txId)
        assertNull(tx.cardId)
        assertNull(tx.txnTypeCode)
    }

    @Test
    fun blankAttributeValueResolvesToNull() {
        val raw = """{ "transaction_id": "t-1",
            "transaction_attributes": [ { "name": "CARD_ID", "type": "STRING", "value": "" } ] }"""
        assertNull(json.decodeFromString<Transaction>(raw).cardId)
    }

    @Test
    fun transactionTypeMapsCodesCaseInsensitivelyWithUnknownFallback() {
        assertEquals(TransactionType.POS, TransactionType.fromCode("POS"))
        assertEquals(TransactionType.POS, TransactionType.fromCode("pos"))
        assertEquals(TransactionType.ATM, TransactionType.fromCode("ATM"))
        assertEquals(TransactionType.UNKNOWN, TransactionType.fromCode("NOPE"))
        assertEquals(TransactionType.UNKNOWN, TransactionType.fromCode(null))
        assertEquals(TransactionType.UNKNOWN, TransactionType.fromCode(""))
    }

    @Test
    fun cardInstrumentTypesAreFlagged() {
        assertTrue(TransactionType.POS.isCardInstrument)
        assertTrue(TransactionType.ECOM.isCardInstrument)
        assertTrue(TransactionType.ATM.isCardInstrument)
        assertFalse(TransactionType.TFR.isCardInstrument)
        assertFalse(TransactionType.DD.isCardInstrument)
        assertFalse(TransactionType.SAL.isCardInstrument)
    }
}
