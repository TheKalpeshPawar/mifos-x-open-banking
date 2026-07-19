/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.model.pisp.domesticScheduledPayment.request

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DomesticScheduledPaymentRequestSerializationTest {

    private val json = Json { prettyPrint = false }

    private fun creditorPostalAddress() = CreditorPostalAddress(
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
    )

    private fun initiation() = Initiation(
        instructionIdentification = "instr-1",
        endToEndIdentification = "e2e-1",
        localInstrument = "UK.OBIE.FPS",
        requestedExecutionDateTime = "2026-02-01T10:00:00Z",
        instructedAmount = InstructedAmount(amount = "100.00", currency = "GBP"),
        debtorAccount = DebtorAccount(
            schemeName = "UK.OBIE.SortCodeAccountNumber",
            identification = "40000000000001",
            name = "John Doe",
        ),
        creditorAccount = CreditorAccount(
            schemeName = "UK.OBIE.SortCodeAccountNumber",
            identification = "40000000000002",
            name = "Jane Doe",
        ),
        creditorPostalAddress = creditorPostalAddress(),
        remittanceInformation = RemittanceInformation(unstructured = listOf("Invoice 1")),
    )

    private fun sampleRequest() = DomesticScheduledPaymentRequest(
        data = Data(
            permission = "Create",
            readRefundAccount = "Yes",
            initiation = initiation(),
            authorisation = Authorisation(
                authorisationType = "Single",
                completionDateTime = "2026-01-02T10:00:00Z",
            ),
            scaSupportData = SCASupportData(
                requestedSCAExemptionType = "BillPayment",
                appliedAuthenticationApproach = "CA",
                referencePaymentOrderId = "order-1",
            ),
            consentId = "consent-1",
        ),
        risk = Risk(
            paymentContextCode = "EcommerceGoods",
            merchantCategoryCode = "5732",
            merchantCustomerIdentification = "cust-1",
            deliveryAddress = DeliveryAddress(
                streetName = "High Street",
                buildingNumber = "1",
                postCode = "EC1A 1BB",
                townName = "London",
                countrySubDivision = "England",
                country = "GB",
            ),
        ),
    )

    @Test
    fun `full domestic scheduled payment request round-trips through JSON`() {
        val original = sampleRequest()
        val encoded = json.encodeToString(DomesticScheduledPaymentRequest.serializer(), original)
        val decoded = json.decodeFromString(DomesticScheduledPaymentRequest.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `domestic scheduled payment request nested fields survive the round-trip`() {
        val original = sampleRequest()
        val encoded = json.encodeToString(DomesticScheduledPaymentRequest.serializer(), original)
        val decoded = json.decodeFromString(DomesticScheduledPaymentRequest.serializer(), encoded)
        assertEquals("consent-1", decoded.data?.consentId)
        assertEquals("Single", decoded.data?.authorisation?.authorisationType)
        assertEquals("order-1", decoded.data?.scaSupportData?.referencePaymentOrderId)
        assertEquals("2026-02-01T10:00:00Z", decoded.data?.initiation?.requestedExecutionDateTime)
        assertEquals("Jane Doe", decoded.data?.initiation?.creditorAccount?.name)
        assertEquals("GB", decoded.risk?.deliveryAddress?.country)
    }

    @Test
    fun `domestic scheduled payment request data class helpers are exercised`() {
        val init = initiation()
        assertEquals(init, init.copy())
        assertEquals(init.hashCode(), init.copy().hashCode())
        assertTrue(init.toString().contains("e2e-1"))
        val request = sampleRequest()
        assertEquals(request, request.copy())
        assertEquals(request.hashCode(), request.copy().hashCode())
        assertTrue(request.toString().contains("consent-1"))
        val defaults = DomesticScheduledPaymentRequest()
        assertEquals(DomesticScheduledPaymentRequest(), defaults)
    }
}
