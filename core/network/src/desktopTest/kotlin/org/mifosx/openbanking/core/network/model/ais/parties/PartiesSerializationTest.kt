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

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PartiesSerializationTest {

    private val json = Json { prettyPrint = false }

    private fun sampleParty() = Party(
        partyId = "party-1",
        partyNumber = "PN-1",
        partyType = "Sole",
        name = "Acme Ltd",
        fullLegalName = "Acme Limited",
        legalStructure = "UK.OBIE.PrivateLimited",
        accountRole = "UK.OBIE.Principal",
        emailAddress = "info@acme.test",
        phone = "+441234567890",
        mobile = "+447700900000",
        relationships = Relationships(
            account = Account(
                related = "/accounts/acc-1",
                id = "acc-1",
            ),
        ),
    )

    private fun sampleResponse() = PartiesResponse(
        data = Data(party = listOf(sampleParty())),
        links = Links(self = "/self"),
        meta = Meta(totalPages = 3),
    )

    @Test
    fun `full parties response round-trips through JSON`() {
        val original = sampleResponse()
        val encoded = json.encodeToString(PartiesResponse.serializer(), original)
        val decoded = json.decodeFromString(PartiesResponse.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `party fields survive the round-trip`() {
        val original = sampleResponse()
        val encoded = json.encodeToString(PartiesResponse.serializer(), original)
        val decoded = json.decodeFromString(PartiesResponse.serializer(), encoded)
        val party = decoded.data?.party?.first()
        assertEquals("party-1", party?.partyId)
        assertEquals("Acme Ltd", party?.name)
        assertEquals("acc-1", party?.relationships?.account?.id)
        assertEquals("info@acme.test", party?.emailAddress)
        assertEquals(3, decoded.meta?.totalPages)
    }

    @Test
    fun `data class helpers are exercised`() {
        val party = sampleParty()
        assertEquals(party, party.copy())
        assertEquals(party.hashCode(), party.copy().hashCode())
        assertTrue(party.toString().contains("party-1"))
        val defaults = PartiesResponse()
        assertEquals(PartiesResponse(), defaults)
    }
}
