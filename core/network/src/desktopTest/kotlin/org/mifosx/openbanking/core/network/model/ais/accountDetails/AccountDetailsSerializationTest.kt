/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.model.ais.accountDetails

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AccountDetailsSerializationTest {

    private val json = Json { prettyPrint = false }

    private fun nestedAccount() = Account(
        accountId = "acc-nested-1",
        currency = "GBP",
        accountCategory = "Personal",
        accountTypeCode = "CACC",
        description = "Nested current account",
        servicer = Servicer(
            schemeName = "UK.OBIE.BICFI",
            identification = "HBUKGB4B",
        ),
        schemeName = "UK.OBIE.SortCodeAccountNumber",
        identification = "40000000000002",
        name = "John Doe Nested",
        account = null,
    )

    private fun sampleAccount() = Account(
        accountId = "acc-1",
        currency = "GBP",
        accountCategory = "Business",
        accountTypeCode = "CACC",
        description = "Primary current account",
        servicer = Servicer(
            schemeName = "UK.OBIE.BICFI",
            identification = "HBUKGB4B",
        ),
        schemeName = "UK.OBIE.SortCodeAccountNumber",
        identification = "40000000000001",
        name = "Jane Doe",
        account = listOf(nestedAccount()),
    )

    private fun sampleResponse() = AccountDetailsResponse(
        data = Data(account = listOf(sampleAccount())),
        links = Links(self = "/self"),
        meta = Meta(totalPages = 3),
    )

    @Test
    fun `full account details response round-trips through JSON`() {
        val original = sampleResponse()
        val encoded = json.encodeToString(AccountDetailsResponse.serializer(), original)
        val decoded = json.decodeFromString(AccountDetailsResponse.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `account fields survive the round-trip`() {
        val original = sampleResponse()
        val encoded = json.encodeToString(AccountDetailsResponse.serializer(), original)
        val decoded = json.decodeFromString(AccountDetailsResponse.serializer(), encoded)
        val account = decoded.data?.account?.first()
        assertEquals("acc-1", account?.accountId)
        assertEquals("GBP", account?.currency)
        assertEquals("HBUKGB4B", account?.servicer?.identification)
        assertEquals("acc-nested-1", account?.account?.first()?.accountId)
        assertEquals(3, decoded.meta?.totalPages)
    }

    @Test
    fun `data class helpers are exercised`() {
        val account = sampleAccount()
        assertEquals(account, account.copy())
        assertEquals(account.hashCode(), account.copy().hashCode())
        assertTrue(account.toString().contains("acc-1"))
        val servicer = Servicer(schemeName = "UK.OBIE.BICFI", identification = "HBUKGB4B")
        assertEquals(servicer, servicer.copy())
        assertEquals(servicer.hashCode(), servicer.copy().hashCode())
        assertTrue(servicer.toString().contains("HBUKGB4B"))
        val defaults = AccountDetailsResponse()
        assertEquals(AccountDetailsResponse(), defaults)
    }
}
