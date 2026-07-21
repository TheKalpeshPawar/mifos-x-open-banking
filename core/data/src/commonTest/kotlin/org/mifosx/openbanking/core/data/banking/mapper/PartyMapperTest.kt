/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking.mapper

import org.mifosx.openbanking.core.network.model.ais.party.Data
import org.mifosx.openbanking.core.network.model.ais.party.Party
import org.mifosx.openbanking.core.network.model.ais.party.PartyAddress
import org.mifosx.openbanking.core.network.model.ais.party.PartyResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The OBIE party payload sends every field optionally and in several combinations, so the
 * resolution rules live in one mapper and are pinned here.
 */
class PartyMapperTest {

    private fun response(party: Party?) = PartyResponse(data = party?.let { Data(party = it) })

    private fun party(
        name: String? = "Priya Sharma",
        fullLegalName: String? = "Mrs Priya Sharma",
        partyType: String? = "Sole",
        emailAddress: String? = "priya.sharma@example.co.uk",
        phone: String? = "+44 20 7946 0000",
        mobile: String? = "+44 7700 900482",
        address: List<PartyAddress>? = null,
    ) = Party(
        partyId = "55786146",
        name = name,
        fullLegalName = fullLegalName,
        partyType = partyType,
        emailAddress = emailAddress,
        phone = phone,
        mobile = mobile,
        address = address,
    )

    private fun address(
        streetName: String? = "Baker Street",
        buildingNumber: String? = "12",
        townName: String? = "London",
        postCode: String? = "W1U 6TZ",
        addressLine: List<String>? = null,
    ) = PartyAddress(
        addressType = "Residential",
        streetName = streetName,
        buildingNumber = buildingNumber,
        townName = townName,
        postCode = postCode,
        addressLine = addressLine,
    )

    @Test
    fun fullPartyMapsEveryFieldToTheProfile() {
        val profile = response(party(address = listOf(address()))).toPartyProfile()

        assertEquals("55786146", profile.partyId)
        assertEquals("Priya Sharma", profile.displayName)
        assertEquals("Personal Account Holder", profile.roleLabel)
        assertEquals("PS", profile.initials)
        assertEquals("priya.sharma@example.co.uk", profile.email)
        assertEquals("+44 7700 900482", profile.mobile)
        assertEquals("12 Baker Street, London, W1U 6TZ", profile.addressLine)
    }

    @Test
    fun nameFallsBackToFullLegalNameWhenNameIsAbsent() {
        val profile = response(party(name = null)).toPartyProfile()

        assertEquals("Mrs Priya Sharma", profile.displayName)
    }

    @Test
    fun mobileFallsBackToPhoneWhenMobileIsAbsent() {
        val profile = response(party(mobile = null)).toPartyProfile()

        assertEquals("+44 20 7946 0000", profile.mobile)
    }

    @Test
    fun initialsAreDerivedFromTheFirstAndLastNameTokens() {
        assertEquals("PS", response(party(name = "Priya Sharma")).toPartyProfile().initials)
        assertEquals("PJ", response(party(name = "Priya Anne Jones")).toPartyProfile().initials)
    }

    @Test
    fun initialsFromASingleTokenNameUseOneLetter() {
        assertEquals("P", response(party(name = "Priya", fullLegalName = null)).toPartyProfile().initials)
    }

    @Test
    fun initialsAreBlankWhenNoNameWasReturned() {
        val profile = response(party(name = null, fullLegalName = null)).toPartyProfile()

        assertEquals("", profile.initials)
    }

    @Test
    fun addressIsJoinedToOneLineInObiePropertyOrder() {
        val profile = response(party(address = listOf(address()))).toPartyProfile()

        assertEquals("12 Baker Street, London, W1U 6TZ", profile.addressLine)
    }

    /** A bank may send only the unstructured list; it is used verbatim. */
    @Test
    fun anUnstructuredAddressLineListIsUsedWhenNoStructuredFieldsArePresent() {
        val unstructured = address(
            streetName = null,
            buildingNumber = null,
            townName = null,
            postCode = null,
            addressLine = listOf("12 Baker Street", "London W1U 6TZ"),
        )

        val profile = response(party(address = listOf(unstructured))).toPartyProfile()

        assertEquals("12 Baker Street, London W1U 6TZ", profile.addressLine)
    }

    /** The HSBC sandbox never returns Address — the row is hidden on a blank line. */
    @Test
    fun aPartyWithNoAddressMapsToABlankAddressLine() {
        val profile = response(party()).toPartyProfile()

        assertEquals("", profile.addressLine)
    }

    @Test
    fun aPartyWithEveryFieldAbsentMapsToAProfileReportingIsEmpty() {
        val profile = response(
            party(name = null, fullLegalName = null, emailAddress = null, phone = null, mobile = null),
        ).toPartyProfile()

        assertTrue(profile.isEmpty)
        assertEquals("", profile.displayName)
    }

    /** A 200 with no Data block is the consent-does-not-cover-identity case, not a failure. */
    @Test
    fun aResponseWithNoDataBlockMapsToAnEmptyProfile() {
        val profile = PartyResponse().toPartyProfile()

        assertTrue(profile.isEmpty)
        assertEquals("", profile.partyId)
        assertEquals("", profile.email)
    }

    @Test
    fun anUnfamiliarPartyTypeFallsBackToTheRawTokenRatherThanBlank() {
        val profile = response(party(partyType = "Joint")).toPartyProfile()

        assertEquals("Joint", profile.roleLabel)
    }
}
