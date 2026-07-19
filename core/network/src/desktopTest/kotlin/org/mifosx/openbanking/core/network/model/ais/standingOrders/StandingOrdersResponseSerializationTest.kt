/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.model.ais.standingOrders

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StandingOrdersResponseSerializationTest {

    private val json = Json { prettyPrint = false }

    private fun postalAddress() = PostalAddress(
        addressType = "Business",
        department = "Ops",
        subDepartment = "Payments",
        streetName = "High Street",
        buildingNumber = "1",
        postCode = "EC1A 1BB",
        townName = "London",
        countrySubDivision = "England",
        country = "GB",
        addressLine = listOf("Line 1", "Line 2"),
        buildingName = "HSBC Tower",
        careOf = "Front Desk",
        districtName = "City",
        floor = "8",
        postBox = "PO 12",
        room = "801",
        townLocationName = "Central",
        unitNumber = "8A",
    )

    private fun standingOrder() = StandingOrder(
        accountId = "acc-1",
        standingOrderId = "so-1",
        reference = "ref-1",
        nextPaymentDateTime = "2026-02-01T10:00:00Z",
        lastPaymentDateTime = "2026-01-01T10:00:00Z",
        numberOfPayments = "12",
        standingOrderStatusCode = "Active",
        firstPaymentAmount = NextPaymentAmount(amount = "50.00", currency = "GBP"),
        nextPaymentAmount = NextPaymentAmount(amount = "60.00", currency = "GBP"),
        lastPaymentAmount = NextPaymentAmount(amount = "70.00", currency = "GBP"),
        finalPaymentAmount = NextPaymentAmount(amount = "80.00", currency = "GBP"),
        creditorAgent = CreditorAgent(
            schemeName = "UK.OBIE.BICFI",
            identification = "HBUKGB4B",
            postalAddress = postalAddress(),
            lei = "LEI-1",
            name = "HSBC",
        ),
        creditorAccount = CreditorAccount(
            schemeName = "UK.OBIE.SortCodeAccountNumber",
            identification = "40000000000001",
            name = "Jane Doe",
            secondaryIdentification = "sec-1",
        ),
        mandateRelatedInformation = MandateRelatedInformation(
            mandateIdentification = "mandate-1",
            classification = "Fixed",
            categoryPurposeCode = "CASH",
            firstPaymentDateTime = "2026-01-01T10:00:00Z",
            finalPaymentDateTime = "2026-12-01T10:00:00Z",
            frequency = Frequency(pointInTime = "1", type = "EvryMnth"),
        ),
        remittanceInformation = RemittanceInformation(
            structured = listOf(
                Structured(
                    creditorReferenceInformation = CreditorReferenceInformation(reference = "cref-1"),
                ),
            ),
        ),
    )

    private fun sampleResponse() = StandingOrdersResponse(
        data = Data(standingOrder = listOf(standingOrder())),
        links = Links(self = "/self"),
        meta = Meta(totalPages = 3),
    )

    @Test
    fun `full standing orders response round-trips through JSON`() {
        val original = sampleResponse()
        val encoded = json.encodeToString(StandingOrdersResponse.serializer(), original)
        val decoded = json.decodeFromString(StandingOrdersResponse.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `standing order nested fields survive the round-trip`() {
        val original = sampleResponse()
        val encoded = json.encodeToString(StandingOrdersResponse.serializer(), original)
        val decoded = json.decodeFromString(StandingOrdersResponse.serializer(), encoded)
        val order = decoded.data?.standingOrder?.first()
        assertEquals("so-1", order?.standingOrderId)
        assertEquals("50.00", order?.firstPaymentAmount?.amount)
        assertEquals("London", order?.creditorAgent?.postalAddress?.townName)
        assertEquals("EvryMnth", order?.mandateRelatedInformation?.frequency?.type)
        val structured = order?.remittanceInformation?.structured?.first()
        assertEquals("cref-1", structured?.creditorReferenceInformation?.reference)
        assertEquals(3, decoded.meta?.totalPages)
    }

    @Test
    fun `standing orders data class helpers are exercised`() {
        val order = standingOrder()
        assertEquals(order, order.copy())
        assertEquals(order.hashCode(), order.copy().hashCode())
        assertTrue(order.toString().contains("so-1"))
        val response = sampleResponse()
        assertEquals(response, response.copy())
        assertEquals(response.hashCode(), response.copy().hashCode())
        assertTrue(response.toString().contains("so-1"))
        val defaults = StandingOrdersResponse()
        assertEquals(StandingOrdersResponse(), defaults)
    }
}
