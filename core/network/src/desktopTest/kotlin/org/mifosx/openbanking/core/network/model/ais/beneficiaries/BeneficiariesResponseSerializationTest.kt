/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.model.ais.beneficiaries

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BeneficiariesResponseSerializationTest {

    private val json = Json { prettyPrint = false }

    private fun postalAddress() = PostalAddress(
        addressType = "Business",
        buildingNumber = "1",
        department = "Ops",
        subDepartment = "Payments",
        streetName = "High Street",
        townName = "London",
        postCode = "EC1A 1BB",
        countrySubDivision = "England",
        country = "GB",
    )

    private fun beneficiary() = Beneficiary(
        accountId = "acc-1",
        reference = "ref-1",
        creditorAccount = CreditorAccount(
            schemeName = "UK.OBIE.SortCodeAccountNumber",
            identification = "40000000000001",
            name = "Jane Doe",
            secondaryIdentification = "sec-1",
        ),
        creditorAgent = CreditorAgent(
            schemeName = "UK.OBIE.BICFI",
            identification = "HBUKGB4B",
            name = "HSBC",
            postalAddress = postalAddress(),
        ),
    )

    private fun sampleResponse() = BeneficiariesResponse(
        data = Data(beneficiary = listOf(beneficiary())),
        links = Links(self = "/self"),
    )

    @Test
    fun `full beneficiaries response round-trips through JSON`() {
        val original = sampleResponse()
        val encoded = json.encodeToString(BeneficiariesResponse.serializer(), original)
        val decoded = json.decodeFromString(BeneficiariesResponse.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `beneficiary nested fields survive the round-trip`() {
        val original = sampleResponse()
        val encoded = json.encodeToString(BeneficiariesResponse.serializer(), original)
        val decoded = json.decodeFromString(BeneficiariesResponse.serializer(), encoded)
        val item = decoded.data?.beneficiary?.first()
        assertEquals("acc-1", item?.accountId)
        assertEquals("Jane Doe", item?.creditorAccount?.name)
        assertEquals("HSBC", item?.creditorAgent?.name)
        assertEquals("London", item?.creditorAgent?.postalAddress?.townName)
        assertEquals("/self", decoded.links?.self)
    }

    @Test
    fun `beneficiaries data class helpers are exercised`() {
        val item = beneficiary()
        assertEquals(item, item.copy())
        assertEquals(item.hashCode(), item.copy().hashCode())
        assertTrue(item.toString().contains("acc-1"))
        val response = sampleResponse()
        assertEquals(response, response.copy())
        assertEquals(response.hashCode(), response.copy().hashCode())
        assertTrue(response.toString().contains("acc-1"))
        val defaults = BeneficiariesResponse()
        assertEquals(BeneficiariesResponse(), defaults)
    }
}
