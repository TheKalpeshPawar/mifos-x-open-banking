/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.model.ais.balances

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BalancesSerializationTest {

    private val json = Json { prettyPrint = false }

    private fun sampleBalance() = Balance(
        accountId = "acc-1",
        creditDebitIndicator = "Credit",
        type = "InterimBooked",
        dateTime = "2026-01-01T10:00:00Z",
        amount = Amount(
            amount = "1000.00",
            currency = "GBP",
        ),
    )

    private fun sampleResponse() = BalancesResponse(
        data = Data(balance = listOf(sampleBalance())),
        links = Links(self = "/self"),
        meta = Meta(totalPages = 3),
    )

    @Test
    fun `full balances response round-trips through JSON`() {
        val original = sampleResponse()
        val encoded = json.encodeToString(BalancesResponse.serializer(), original)
        val decoded = json.decodeFromString(BalancesResponse.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `balance fields survive the round-trip`() {
        val original = sampleResponse()
        val encoded = json.encodeToString(BalancesResponse.serializer(), original)
        val decoded = json.decodeFromString(BalancesResponse.serializer(), encoded)
        val balance = decoded.data?.balance?.first()
        assertEquals("acc-1", balance?.accountId)
        assertEquals("Credit", balance?.creditDebitIndicator)
        assertEquals("1000.00", balance?.amount?.amount)
        assertEquals("GBP", balance?.amount?.currency)
        assertEquals(3, decoded.meta?.totalPages)
    }

    @Test
    fun `data class helpers are exercised`() {
        val balance = sampleBalance()
        assertEquals(balance, balance.copy())
        assertEquals(balance.hashCode(), balance.copy().hashCode())
        assertTrue(balance.toString().contains("acc-1"))
        val amount = Amount(amount = "1000.00", currency = "GBP")
        assertEquals(amount, amount.copy())
        assertEquals(amount.hashCode(), amount.copy().hashCode())
        assertTrue(amount.toString().contains("1000.00"))
        val defaults = BalancesResponse()
        assertEquals(BalancesResponse(), defaults)
    }
}
