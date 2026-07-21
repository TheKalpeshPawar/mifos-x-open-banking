/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.network.model.ais.directDebits

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DirectDebitsResponseSerializationTest {

    private val json = Json { prettyPrint = false }
    private val lenient = Json { ignoreUnknownKeys = true }

    private fun directDebit() = DirectDebit(
        accountId = "40051512345678",
        directDebitId = "DD-001",
        mandateRelatedInformation = MandateRelatedInformation(
            mandateIdentification = "DD-BG-44120",
            frequency = Frequency(type = "EvryMnth"),
        ),
        directDebitStatusCode = "Active",
        name = "British Gas",
        previousPaymentDateTime = "2026-06-15T00:00:00Z",
        previousPaymentAmount = PreviousPaymentAmount(amount = "78.00", currency = "GBP"),
    )

    private fun sampleResponse() = DirectDebitsResponse(
        data = Data(directDebit = listOf(directDebit())),
        links = Links(self = "/self"),
        meta = Meta(totalPages = 1),
    )

    @Test
    fun `full direct debits response round-trips through JSON`() {
        val original = sampleResponse()
        val encoded = json.encodeToString(DirectDebitsResponse.serializer(), original)
        val decoded = json.decodeFromString(DirectDebitsResponse.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `direct debit nested fields survive the round-trip`() {
        val original = sampleResponse()
        val encoded = json.encodeToString(DirectDebitsResponse.serializer(), original)
        val decoded = json.decodeFromString(DirectDebitsResponse.serializer(), encoded)
        val mandate = decoded.data?.directDebit?.first()
        assertEquals("DD-001", mandate?.directDebitId)
        assertEquals("British Gas", mandate?.name)
        assertEquals("Active", mandate?.directDebitStatusCode)
        assertEquals("78.00", mandate?.previousPaymentAmount?.amount)
        assertEquals("GBP", mandate?.previousPaymentAmount?.currency)
        assertEquals("DD-BG-44120", mandate?.mandateRelatedInformation?.mandateIdentification)
        assertEquals("EvryMnth", mandate?.mandateRelatedInformation?.frequency?.type)
        assertEquals(1, decoded.meta?.totalPages)
    }

    @Test
    fun `OBIE PascalCase wire names decode into the camelCase model`() {
        val wire = """
            {"Data":{"DirectDebit":[
              {"AccountId":"40051512345678","DirectDebitId":"DD-004",
               "DirectDebitStatusCode":"Inactive","Name":"TV Licensing",
               "PreviousPaymentDateTime":"2026-03-01T00:00:00Z",
               "PreviousPaymentAmount":{"Amount":"13.25","Currency":"GBP"},
               "MandateRelatedInformation":{"MandateIdentification":"DD-TVL-55667"}}
            ]},"Links":{"Self":"/self"},"Meta":{"TotalPages":1}}
        """.trimIndent()

        val decoded = lenient.decodeFromString(DirectDebitsResponse.serializer(), wire)

        val mandate = decoded.data?.directDebit?.single()
        assertEquals("40051512345678", mandate?.accountId)
        assertEquals("DD-004", mandate?.directDebitId)
        assertEquals("Inactive", mandate?.directDebitStatusCode)
        assertEquals("TV Licensing", mandate?.name)
        assertEquals("2026-03-01T00:00:00Z", mandate?.previousPaymentDateTime)
        assertEquals("13.25", mandate?.previousPaymentAmount?.amount)
        assertEquals("DD-TVL-55667", mandate?.mandateRelatedInformation?.mandateIdentification)
        assertEquals("/self", decoded.links?.self)
    }

    @Test
    fun `an empty DirectDebit array decodes to an empty list rather than null`() {
        val decoded = lenient.decodeFromString(
            DirectDebitsResponse.serializer(),
            """{"Data":{"DirectDebit":[]},"Meta":{"TotalPages":1}}""",
        )

        assertEquals(emptyList(), decoded.data?.directDebit)
    }

    @Test
    fun `absent optional blocks decode to null without failing`() {
        val decoded = lenient.decodeFromString(
            DirectDebitsResponse.serializer(),
            """{"Data":{"DirectDebit":[{"Name":"British Gas"}]}}""",
        )

        val mandate = decoded.data?.directDebit?.single()
        assertEquals("British Gas", mandate?.name)
        assertNull(mandate?.previousPaymentAmount)
        assertNull(mandate?.mandateRelatedInformation)
        assertNull(mandate?.directDebitStatusCode)
        assertNull(decoded.links)
        assertNull(decoded.meta)
    }

    @Test
    fun `direct debits data class helpers are exercised`() {
        val mandate = directDebit()
        assertEquals(mandate, mandate.copy())
        assertEquals(mandate.hashCode(), mandate.copy().hashCode())
        assertTrue(mandate.toString().contains("British Gas"))
        val response = sampleResponse()
        assertEquals(response, response.copy())
        assertEquals(response.hashCode(), response.copy().hashCode())
        assertTrue(response.toString().contains("British Gas"))
        val defaults = DirectDebitsResponse()
        assertEquals(DirectDebitsResponse(), defaults)
        assertEquals(Data(), Data())
        assertEquals(Links(), Links())
        assertEquals(Meta(), Meta())
        assertEquals(Frequency(), Frequency())
        assertEquals(PreviousPaymentAmount(), PreviousPaymentAmount())
        assertEquals(MandateRelatedInformation(), MandateRelatedInformation())
    }
}
