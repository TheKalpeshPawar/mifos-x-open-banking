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

import org.mifosx.openbanking.core.network.model.ais.directDebits.Data
import org.mifosx.openbanking.core.network.model.ais.directDebits.DirectDebit
import org.mifosx.openbanking.core.network.model.ais.directDebits.DirectDebitsResponse
import org.mifosx.openbanking.core.network.model.ais.directDebits.MandateRelatedInformation
import org.mifosx.openbanking.core.network.model.ais.directDebits.PreviousPaymentAmount
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers the OBIE `OBReadDirectDebit2` mapping: the active-first sort the spec mandates, the
 * summary tallies, status handling and the degradation rules for missing fields.
 */
class DirectDebitMapperTest {

    private fun mandate(
        name: String? = "British Gas",
        status: String? = "Active",
        mandateId: String? = "DD-BG-44120",
        amount: String? = "78.00",
        currency: String? = "GBP",
        previousPayment: String? = "2026-06-15T00:00:00Z",
    ) = DirectDebit(
        accountId = "40051512345678",
        directDebitId = "DD-001",
        mandateRelatedInformation = mandateId?.let { MandateRelatedInformation(mandateIdentification = it) },
        directDebitStatusCode = status,
        name = name,
        previousPaymentDateTime = previousPayment,
        previousPaymentAmount = amount?.let { PreviousPaymentAmount(amount = it, currency = currency) },
    )

    private fun response(vararg mandates: DirectDebit) =
        DirectDebitsResponse(data = Data(directDebit = mandates.toList()))

    @Test
    fun mapsEveryDeclaredFieldFromTheObiePayload() {
        val item = response(mandate()).toDirectDebitItems().single()

        assertEquals("DD-BG-44120", item.mandateId)
        assertEquals("British Gas", item.name)
        assertEquals("Active", item.statusCode)
        assertEquals("78.00", item.previousPaymentAmount)
        assertEquals("GBP", item.currency)
        assertEquals("2026-06-15T00:00:00Z", item.previousPaymentDateTime)
        assertTrue(item.isActive)
    }

    @Test
    fun sortsActiveMandatesBeforeInactiveOnes() {
        val mapped = response(
            mandate(name = "TV Licensing", status = "Inactive"),
            mandate(name = "British Gas", status = "Active"),
            mandate(name = "Cancelled Co", status = "Inactive"),
            mandate(name = "Vodafone", status = "Active"),
        ).toDirectDebitItems()

        assertEquals(
            listOf("British Gas", "Vodafone", "TV Licensing", "Cancelled Co"),
            mapped.map { it.name },
        )
    }

    @Test
    fun sortIsStableSoBankOrderSurvivesWithinEachStatusGroup() {
        val mapped = response(
            mandate(name = "Aviva Insurance", status = "Active"),
            mandate(name = "British Gas", status = "Active"),
            mandate(name = "Vodafone", status = "Active"),
        ).toDirectDebitItems()

        assertEquals(listOf("Aviva Insurance", "British Gas", "Vodafone"), mapped.map { it.name })
    }

    @Test
    fun summaryCountsActiveAndInactiveMandates() {
        val summary = response(
            mandate(name = "British Gas", status = "Active"),
            mandate(name = "Vodafone", status = "Active"),
            mandate(name = "Aviva Insurance", status = "Active"),
            mandate(name = "TV Licensing", status = "Inactive"),
        ).toDirectDebitsSummary()

        assertEquals(4, summary.items.size)
        assertEquals(3, summary.activeCount)
        assertEquals(1, summary.inactiveCount)
        assertFalse(summary.isEmpty)
    }

    @Test
    fun statusMatchingIgnoresCase() {
        val mapped = response(mandate(status = "ACTIVE")).toDirectDebitItems().single()
        assertTrue(mapped.isActive)
    }

    @Test
    fun anyStatusOtherThanActiveCountsAsInactive() {
        val summary = response(
            mandate(name = "Suspended Co", status = "Suspended"),
            mandate(name = "Expired Co", status = "Expired"),
        ).toDirectDebitsSummary()

        assertEquals(0, summary.activeCount)
        assertEquals(2, summary.inactiveCount)
        assertTrue(summary.items.none { it.isActive })
    }

    @Test
    fun unknownStatusIsPreservedVerbatimRatherThanNormalised() {
        val item = response(mandate(status = "Suspended")).toDirectDebitItems().single()
        assertEquals("Suspended", item.statusCode)
    }

    @Test
    fun mandateWithoutANameIsDroppedBecauseTheCardWouldBeUnidentifiable() {
        val mapped = response(
            mandate(name = null),
            mandate(name = "   "),
            mandate(name = "British Gas"),
        ).toDirectDebitItems()

        assertEquals(listOf("British Gas"), mapped.map { it.name })
    }

    @Test
    fun missingOptionalFieldsDegradeToEmptyStringsRatherThanDroppingTheMandate() {
        val item = response(
            mandate(status = null, mandateId = null, amount = null, previousPayment = null),
        ).toDirectDebitItems().single()

        assertEquals("", item.mandateId)
        assertEquals("", item.statusCode)
        assertEquals("", item.previousPaymentAmount)
        assertEquals("", item.currency)
        assertEquals("", item.previousPaymentDateTime)
        assertFalse(item.isActive)
    }

    @Test
    fun amountPresentWithoutCurrencyKeepsTheAmount() {
        val item = response(mandate(amount = "13.25", currency = null)).toDirectDebitItems().single()

        assertEquals("13.25", item.previousPaymentAmount)
        assertEquals("", item.currency)
    }

    @Test
    fun emptyMandateArrayMapsToAnEmptySummary() {
        val summary = response().toDirectDebitsSummary()

        assertTrue(summary.items.isEmpty())
        assertEquals(0, summary.activeCount)
        assertEquals(0, summary.inactiveCount)
        assertTrue(summary.isEmpty)
    }

    @Test
    fun absentDataBlockMapsToAnEmptySummaryRatherThanFailing() {
        val summary = DirectDebitsResponse().toDirectDebitsSummary()

        assertTrue(summary.items.isEmpty())
        assertTrue(summary.isEmpty)
    }

    @Test
    fun absentDirectDebitArrayMapsToAnEmptySummary() {
        val summary = DirectDebitsResponse(data = Data(directDebit = null)).toDirectDebitsSummary()

        assertTrue(summary.items.isEmpty())
        assertTrue(summary.isEmpty)
    }
}
