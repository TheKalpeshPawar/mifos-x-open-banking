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

import org.mifosx.openbanking.core.network.model.ais.standingOrders.CreditorAccount
import org.mifosx.openbanking.core.network.model.ais.standingOrders.Data
import org.mifosx.openbanking.core.network.model.ais.standingOrders.Frequency
import org.mifosx.openbanking.core.network.model.ais.standingOrders.MandateRelatedInformation
import org.mifosx.openbanking.core.network.model.ais.standingOrders.NextPaymentAmount
import org.mifosx.openbanking.core.network.model.ais.standingOrders.StandingOrder
import org.mifosx.openbanking.core.network.model.ais.standingOrders.StandingOrdersResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers the OBIE `OBReadStandingOrder6` mapping: the ISO 20022 frequency decode, the active-first
 * sort the spec mandates, the summary tallies and the degradation rules for missing fields.
 */
class StandingOrderMapperTest {

    private fun order(
        payee: String? = "Jameson Lettings",
        status: String? = "Active",
        orderId: String? = "SO-001",
        amount: String? = "1200.00",
        currency: String? = "GBP",
        frequencyType: String? = "IntrvlMnthDay",
        frequencyPoint: String? = "01:01",
        nextPayment: String? = "2026-07-01T00:00:00Z",
        finalPayment: String? = null,
        identification: String? = "40-12-09 65872310",
        reference: String? = "RENT-FLAT12",
    ) = StandingOrder(
        accountId = "40051512345678",
        standingOrderId = orderId,
        reference = reference,
        nextPaymentDateTime = nextPayment,
        standingOrderStatusCode = status,
        nextPaymentAmount = amount?.let { NextPaymentAmount(amount = it, currency = currency) },
        creditorAccount = CreditorAccount(
            schemeName = "UK.OBIE.SortCodeAccountNumber",
            identification = identification,
            name = payee,
        ),
        mandateRelatedInformation = MandateRelatedInformation(
            finalPaymentDateTime = finalPayment,
            frequency = if (frequencyType == null && frequencyPoint == null) {
                null
            } else {
                Frequency(type = frequencyType, pointInTime = frequencyPoint)
            },
        ),
    )

    private fun response(vararg orders: StandingOrder) =
        StandingOrdersResponse(data = Data(standingOrder = orders.toList()))

    private fun frequencyLabelFor(type: String?, point: String?): String =
        response(order(frequencyType = type, frequencyPoint = point))
            .toStandingOrderItems()
            .single()
            .frequencyLabel

    @Test
    fun mapsEveryDeclaredFieldFromTheObiePayload() {
        val item = response(order()).toStandingOrderItems().single()

        assertEquals("SO-001", item.standingOrderId)
        assertEquals("Jameson Lettings", item.payeeName)
        assertEquals("Active", item.statusCode)
        assertEquals("1200.00", item.nextPaymentAmount)
        assertEquals("GBP", item.currency)
        assertEquals("Monthly on the 1st", item.frequencyLabel)
        assertEquals("2026-07-01T00:00:00Z", item.nextPaymentDateTime)
        assertEquals("40-12-09 65872310", item.creditorIdentification)
        assertEquals("RENT-FLAT12", item.reference)
        assertTrue(item.isActive)
    }

    @Test
    fun decodesEveryMonthlyFrequencyCodeInTheLookupTable() {
        assertEquals("Monthly on the 1st", frequencyLabelFor("IntrvlMnthDay", "01:01"))
        assertEquals("Monthly on the 15th", frequencyLabelFor("IntrvlMnthDay", "01:15"))
        assertEquals("Monthly on the 28th", frequencyLabelFor("IntrvlMnthDay", "01:28"))
        assertEquals("Quarterly on the 1st", frequencyLabelFor("IntrvlMnthDay", "03:01"))
        assertEquals("Every 6 months on the 1st", frequencyLabelFor("IntrvlMnthDay", "06:01"))
    }

    @Test
    fun decodesEveryWeeklyFrequencyCodeInTheLookupTable() {
        assertEquals("Weekly every Monday", frequencyLabelFor("IntrvlWkDay", "01:1"))
        assertEquals("Weekly every Tuesday", frequencyLabelFor("IntrvlWkDay", "01:2"))
        assertEquals("Weekly every Wednesday", frequencyLabelFor("IntrvlWkDay", "01:3"))
        assertEquals("Weekly every Thursday", frequencyLabelFor("IntrvlWkDay", "01:4"))
        assertEquals("Weekly every Friday", frequencyLabelFor("IntrvlWkDay", "01:5"))
        assertEquals("Every 2 weeks on Monday", frequencyLabelFor("IntrvlWkDay", "02:1"))
    }

    @Test
    fun decodesTheDayAndYearIntervalFrequencyCodes() {
        assertEquals("Every 14 days", frequencyLabelFor("IntrvlDay", "14"))
        assertEquals("Every 28 days", frequencyLabelFor("IntrvlDay", "28"))
        assertEquals("Annually on 1 January", frequencyLabelFor("IntrvlYear", "01:01:01"))
    }

    /** TC-SO-013: an unmapped code must reach the screen verbatim rather than blank or crashing. */
    @Test
    fun unknownFrequencyCodeFallsBackToTheRawCompositeCode() {
        assertEquals("IntrvlMnthDay:02:10", frequencyLabelFor("IntrvlMnthDay", "02:10"))
        assertEquals("IntrvlWkDay:04:7", frequencyLabelFor("IntrvlWkDay", "04:7"))
        assertEquals("SomeFutureCode:99", frequencyLabelFor("SomeFutureCode", "99"))
    }

    @Test
    fun frequencyWithOnlyATypeUsesTheTypeAloneAsTheCode() {
        assertEquals("EvryDay", frequencyLabelFor("EvryDay", null))
        assertEquals("EvryDay", frequencyLabelFor("EvryDay", ""))
    }

    @Test
    fun frequencyWithOnlyAPointInTimeUsesThatAloneRatherThanALeadingColon() {
        assertEquals("01:01", frequencyLabelFor(null, "01:01"))
    }

    @Test
    fun absentFrequencyBlockYieldsABlankLabelRatherThanFailing() {
        val item = response(order(frequencyType = null, frequencyPoint = null))
            .toStandingOrderItems()
            .single()

        assertEquals("", item.frequencyLabel)
    }

    @Test
    fun sortsActiveOrdersBeforeInactiveOnes() {
        val mapped = response(
            order(payee = "Oxfam GB", status = "Inactive"),
            order(payee = "Jameson Lettings", status = "Active"),
            order(payee = "Cancelled Co", status = "Inactive"),
            order(payee = "ISA Saver", status = "Active"),
        ).toStandingOrderItems()

        assertEquals(
            listOf("Jameson Lettings", "ISA Saver", "Oxfam GB", "Cancelled Co"),
            mapped.map { it.payeeName },
        )
    }

    @Test
    fun sortIsStableSoBankOrderSurvivesWithinEachStatusGroup() {
        val mapped = response(
            order(payee = "Jameson Lettings", status = "Active"),
            order(payee = "ISA Saver", status = "Active"),
            order(payee = "PureGym", status = "Active"),
        ).toStandingOrderItems()

        assertEquals(listOf("Jameson Lettings", "ISA Saver", "PureGym"), mapped.map { it.payeeName })
    }

    /** TC-SO-001: the demo account's `4 Active · 1 Inactive` summary. */
    @Test
    fun summaryCountsFourActiveAndOneInactiveForTheDemoAccount() {
        val summary = response(
            order(payee = "Jameson Lettings", status = "Active"),
            order(payee = "ISA Saver", status = "Active"),
            order(payee = "PureGym", status = "Active"),
            order(payee = "Marcus Savings", status = "Active"),
            order(payee = "Oxfam GB", status = "Inactive"),
        ).toStandingOrdersSummary()

        assertEquals(5, summary.items.size)
        assertEquals(4, summary.activeCount)
        assertEquals(1, summary.inactiveCount)
        assertFalse(summary.isEmpty)
    }

    @Test
    fun statusMatchingIgnoresCase() {
        val mapped = response(order(status = "ACTIVE")).toStandingOrderItems().single()
        assertTrue(mapped.isActive)
    }

    @Test
    fun anyStatusOtherThanActiveCountsAsInactive() {
        val summary = response(
            order(payee = "Suspended Co", status = "Suspended"),
            order(payee = "Expired Co", status = "Expired"),
        ).toStandingOrdersSummary()

        assertEquals(0, summary.activeCount)
        assertEquals(2, summary.inactiveCount)
        assertTrue(summary.items.none { it.isActive })
    }

    @Test
    fun unknownStatusIsPreservedVerbatimRatherThanNormalised() {
        val item = response(order(status = "Suspended")).toStandingOrderItems().single()
        assertEquals("Suspended", item.statusCode)
    }

    @Test
    fun finalPaymentDateSetsTheFlagAndCarriesTheValue() {
        val item = response(order(finalPayment = "2026-12-25T00:00:00Z"))
            .toStandingOrderItems()
            .single()

        assertTrue(item.hasFinalPayment)
        assertEquals("2026-12-25T00:00:00Z", item.finalPaymentDateTime)
    }

    @Test
    fun absentFinalPaymentDateClearsTheFlagAndBlanksTheValue() {
        val item = response(order(finalPayment = null)).toStandingOrderItems().single()

        assertFalse(item.hasFinalPayment)
        assertEquals("", item.finalPaymentDateTime)
    }

    @Test
    fun blankFinalPaymentDateIsTreatedAsAbsentRatherThanPresent() {
        val item = response(order(finalPayment = "   ")).toStandingOrderItems().single()

        assertFalse(item.hasFinalPayment)
    }

    @Test
    fun orderWithoutAPayeeNameIsDroppedBecauseTheCardWouldBeUnidentifiable() {
        val mapped = response(
            order(payee = null),
            order(payee = "   "),
            order(payee = "Jameson Lettings"),
        ).toStandingOrderItems()

        assertEquals(listOf("Jameson Lettings"), mapped.map { it.payeeName })
    }

    @Test
    fun orderWithoutACreditorAccountBlockIsDropped() {
        val mapped = response(order().copy(creditorAccount = null)).toStandingOrderItems()

        assertTrue(mapped.isEmpty())
    }

    @Test
    fun missingOptionalFieldsDegradeToEmptyStringsRatherThanDroppingTheOrder() {
        val item = response(
            order(
                status = null,
                orderId = null,
                amount = null,
                nextPayment = null,
                identification = null,
                reference = null,
            ),
        ).toStandingOrderItems().single()

        assertEquals("", item.standingOrderId)
        assertEquals("", item.statusCode)
        assertEquals("", item.nextPaymentAmount)
        assertEquals("", item.currency)
        assertEquals("", item.nextPaymentDateTime)
        assertEquals("", item.creditorIdentification)
        assertEquals("", item.reference)
        assertFalse(item.isActive)
    }

    /** A cancelled order has no next payment but still renders, dimmed, with its final one. */
    @Test
    fun cancelledOrderWithNoNextPaymentStillMaps() {
        val item = response(
            order(
                payee = "Oxfam GB",
                status = "Inactive",
                nextPayment = null,
                finalPayment = "2025-12-28T00:00:00Z",
            ),
        ).toStandingOrderItems().single()

        assertEquals("Oxfam GB", item.payeeName)
        assertEquals("", item.nextPaymentDateTime)
        assertTrue(item.hasFinalPayment)
        assertFalse(item.isActive)
    }

    @Test
    fun amountPresentWithoutCurrencyKeepsTheAmount() {
        val item = response(order(amount = "24.99", currency = null)).toStandingOrderItems().single()

        assertEquals("24.99", item.nextPaymentAmount)
        assertEquals("", item.currency)
    }

    @Test
    fun emptyStandingOrderArrayMapsToAnEmptySummary() {
        val summary = response().toStandingOrdersSummary()

        assertTrue(summary.items.isEmpty())
        assertEquals(0, summary.activeCount)
        assertEquals(0, summary.inactiveCount)
        assertTrue(summary.isEmpty)
    }

    @Test
    fun absentDataBlockMapsToAnEmptySummaryRatherThanFailing() {
        val summary = StandingOrdersResponse().toStandingOrdersSummary()

        assertTrue(summary.items.isEmpty())
        assertTrue(summary.isEmpty)
    }

    @Test
    fun absentStandingOrderArrayMapsToAnEmptySummary() {
        val summary = StandingOrdersResponse(data = Data(standingOrder = null)).toStandingOrdersSummary()

        assertTrue(summary.items.isEmpty())
        assertTrue(summary.isEmpty)
    }
}
