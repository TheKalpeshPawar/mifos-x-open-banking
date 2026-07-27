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

import org.mifosx.openbanking.core.model.banking.ScheduledPaymentType
import org.mifosx.openbanking.core.network.model.ais.scheduledPayments.CreditorAccount
import org.mifosx.openbanking.core.network.model.ais.scheduledPayments.Data
import org.mifosx.openbanking.core.network.model.ais.scheduledPayments.InstructedAmount
import org.mifosx.openbanking.core.network.model.ais.scheduledPayments.ScheduledPayment
import org.mifosx.openbanking.core.network.model.ais.scheduledPayments.ScheduledPaymentsResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers [ScheduledPaymentsResponse.toScheduledPaymentItems]: field projection, the case-insensitive
 * ScheduledType parse, absent-field degradation and the empty payload.
 */
class ScheduledPaymentMapperTest {

    private companion object {
        const val ACCOUNT_ID = "40051512345678"
    }

    @Test
    fun everyFieldIsProjectedFromTheObiePayload() {
        val item = response(
            ScheduledPayment(
                scheduledPaymentId = "SP-001",
                accountId = ACCOUNT_ID,
                scheduledPaymentDateTime = "2026-07-31T00:00:00Z",
                scheduledType = "Execution",
                reference = "HMRC-SA-2526",
                instructedAmount = InstructedAmount(amount = "842.00", currency = "GBP"),
                creditorAccount = CreditorAccount(
                    schemeName = "UK.OBIE.SortCodeAccountNumber",
                    identification = "08-32-00 12001039",
                    name = "HMRC Self Assessment",
                ),
            ),
        ).toScheduledPaymentItems(ACCOUNT_ID).single()

        assertEquals("SP-001", item.scheduledPaymentId)
        assertEquals(ACCOUNT_ID, item.accountId)
        assertEquals("HMRC Self Assessment", item.payeeName)
        assertEquals("842.00", item.amount)
        assertEquals("GBP", item.currency)
        assertEquals("2026-07-31T00:00:00Z", item.scheduledDateTime)
        assertEquals(ScheduledPaymentType.Execution, item.scheduledType)
        assertEquals("HMRC-SA-2526", item.reference)
        assertEquals("08-32-00 12001039", item.creditorIdentification)
    }

    @Test
    fun theScheduledTypeIsParsedCaseInsensitivelyIntoTheDomainEnum() {
        val types = response(
            payment(id = "SP-1", type = "Execution"),
            payment(id = "SP-2", type = "arrival"),
            payment(id = "SP-3", type = "ARRIVAL"),
            payment(id = "SP-4", type = "something-else"),
            payment(id = "SP-5", type = null),
        ).toScheduledPaymentItems(ACCOUNT_ID).map { it.scheduledType }

        assertEquals(
            listOf(
                ScheduledPaymentType.Execution,
                ScheduledPaymentType.Arrival,
                ScheduledPaymentType.Arrival,
                ScheduledPaymentType.Unknown,
                ScheduledPaymentType.Unknown,
            ),
            types,
        )
    }

    @Test
    fun absentFieldsDegradeToEmptyStringsRatherThanDroppingThePayment() {
        val item = response(ScheduledPayment(scheduledPaymentId = "SP-9"))
            .toScheduledPaymentItems(ACCOUNT_ID)
            .single()

        assertEquals("SP-9", item.scheduledPaymentId)
        assertEquals(ACCOUNT_ID, item.accountId)
        assertEquals("", item.payeeName)
        assertEquals("", item.amount)
        assertEquals("", item.currency)
        assertEquals("", item.scheduledDateTime)
        assertEquals("", item.reference)
        assertEquals("", item.creditorIdentification)
        assertEquals(ScheduledPaymentType.Unknown, item.scheduledType)
    }

    @Test
    fun theRequestedAccountIdIsUsedWhenThePayloadOmitsItsOwn() {
        val item = response(ScheduledPayment(scheduledPaymentId = "SP-9", accountId = null))
            .toScheduledPaymentItems(ACCOUNT_ID)
            .single()

        assertEquals(ACCOUNT_ID, item.accountId)
    }

    @Test
    fun anEmptyPayloadMapsToAnEmptyList() {
        val emptyData = ScheduledPaymentsResponse(data = Data(scheduledPayment = emptyList()))
        val nullData = ScheduledPaymentsResponse(data = null)
        assertTrue(emptyData.toScheduledPaymentItems(ACCOUNT_ID).isEmpty())
        assertTrue(nullData.toScheduledPaymentItems(ACCOUNT_ID).isEmpty())
    }

    private fun response(vararg payments: ScheduledPayment): ScheduledPaymentsResponse =
        ScheduledPaymentsResponse(data = Data(scheduledPayment = payments.toList()))

    private fun payment(id: String, type: String?): ScheduledPayment =
        ScheduledPayment(scheduledPaymentId = id, scheduledType = type)
}
