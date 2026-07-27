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

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PartySerializationTest {

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

    private fun sampleAddress() = PartyAddress(
        addressType = "Residential",
        streetName = "Baker Street",
        buildingNumber = "12",
        postCode = "W1U 6TZ",
        townName = "London",
        country = "GB",
    )

    private fun sampleResponse() = PartyResponse(
        data = Data(party = sampleParty()),
        links = Links(self = "/self"),
        meta = Meta(totalPages = 3),
    )

    @Test
    fun `full party response round-trips through JSON`() {
        val original = sampleResponse()
        val encoded = json.encodeToString(PartyResponse.serializer(), original)
        val decoded = json.decodeFromString(PartyResponse.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `party fields survive the round-trip`() {
        val original = sampleResponse()
        val encoded = json.encodeToString(PartyResponse.serializer(), original)
        val decoded = json.decodeFromString(PartyResponse.serializer(), encoded)
        val party = decoded.data?.party
        assertEquals("party-1", party?.partyId)
        assertEquals("Acme Ltd", party?.name)
        assertEquals("acc-1", party?.relationships?.account?.id)
        assertEquals("info@acme.test", party?.emailAddress)
        assertEquals(3, decoded.meta?.totalPages)
    }

    @Test
    fun `party address survives the round-trip`() {
        val original = sampleResponse().let {
            it.copy(data = it.data?.copy(party = sampleParty().copy(address = listOf(sampleAddress()))))
        }
        val decoded = json.decodeFromString(
            PartyResponse.serializer(),
            json.encodeToString(PartyResponse.serializer(), original),
        )
        val address = decoded.data?.party?.address?.firstOrNull()
        assertEquals("Residential", address?.addressType)
        assertEquals("Baker Street", address?.streetName)
        assertEquals("12", address?.buildingNumber)
        assertEquals("London", address?.townName)
        assertEquals("W1U 6TZ", address?.postCode)
        assertEquals("GB", address?.country)
    }

    /**
     * The HSBC UK Personal sandbox returns no `Address` on the party endpoints, so absent must
     * decode to null rather than an empty list — the profile row is hidden on null.
     */
    @Test
    fun `a party with no address decodes to null`() {
        val decoded = json.decodeFromString(
            PartyResponse.serializer(),
            json.encodeToString(PartyResponse.serializer(), sampleResponse()),
        )
        assertEquals(null, decoded.data?.party?.address)
    }

    @Test
    fun `multiple addresses are all decoded in order`() {
        val correspondence = sampleAddress().copy(addressType = "Correspondence", townName = "Leeds")
        val original = sampleResponse().let {
            it.copy(
                data = it.data?.copy(
                    party = sampleParty().copy(address = listOf(sampleAddress(), correspondence)),
                ),
            )
        }
        val decoded = json.decodeFromString(
            PartyResponse.serializer(),
            json.encodeToString(PartyResponse.serializer(), original),
        )
        assertEquals(
            listOf("Residential", "Correspondence"),
            decoded.data?.party?.address?.map { it.addressType },
        )
    }

    /** Some banks send only the unstructured `AddressLine` list and no structured components. */
    @Test
    fun `address line array decodes when the bank sends no structured fields`() {
        val unstructured = PartyAddress(addressLine = listOf("12 Baker Street", "London W1U 6TZ"))
        val original = sampleResponse().let {
            it.copy(data = it.data?.copy(party = sampleParty().copy(address = listOf(unstructured))))
        }
        val decoded = json.decodeFromString(
            PartyResponse.serializer(),
            json.encodeToString(PartyResponse.serializer(), original),
        )
        val address = decoded.data?.party?.address?.firstOrNull()
        assertEquals(listOf("12 Baker Street", "London W1U 6TZ"), address?.addressLine)
        assertEquals(null, address?.streetName)
    }

    @Test
    fun `data class helpers are exercised`() {
        val party = sampleParty()
        assertEquals(party, party.copy())
        assertEquals(party.hashCode(), party.copy().hashCode())
        assertTrue(party.toString().contains("party-1"))
        val defaults = PartyResponse()
        assertEquals(PartyResponse(), defaults)
    }
}
