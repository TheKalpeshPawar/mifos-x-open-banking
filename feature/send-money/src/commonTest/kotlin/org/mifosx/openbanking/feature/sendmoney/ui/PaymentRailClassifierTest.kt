/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.sendmoney.ui

import org.mifosx.openbanking.core.model.obp.Counterparty
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun ibanPayee(
    iban: String,
    currency: String = "EUR",
    bankScheme: String = "BIC",
    bankAddress: String = "COBADEFFXXX",
) = Counterparty(
    counterpartyId = "cp-1",
    name = "Payee",
    currency = currency,
    otherAccountRoutingScheme = "IBAN",
    otherAccountRoutingAddress = iban,
    otherBankRoutingScheme = bankScheme,
    otherBankRoutingAddress = bankAddress,
    isBeneficiary = true,
)

private fun obpPayee(currency: String = "EUR", bankId: String = "ac.bank.uk") = Counterparty(
    counterpartyId = "cp-2",
    name = "Internal Payee",
    currency = currency,
    otherAccountRoutingScheme = "OBP",
    otherAccountRoutingAddress = "ac.savings.001",
    otherBankRoutingScheme = "OBP",
    otherBankRoutingAddress = bankId,
    isBeneficiary = true,
)

class PaymentRailClassifierTest {

    @Test
    fun gbSource_gbIban_sameCurrency_recommendsDomestic() {
        val result = PaymentRailClassifier.classify(
            sourceCurrency = "GBP",
            sourceBankCountry = "GB",
            beneficiary = ibanPayee("GB29NWBK60161331926819", currency = "GBP", bankAddress = "NWBKGB2L"),
        )
        assertEquals(PaymentType.DOMESTIC, result.recommended)
        assertTrue(result.assessments.getValue(PaymentType.DOMESTIC).eligible)
        assertTrue(result.assessments.getValue(PaymentType.SEPA).eligible)
        val international = result.assessments.getValue(PaymentType.INTERNATIONAL)
        assertFalse(international.eligible)
        assertNull(international.reason)
    }

    @Test
    fun eurSource_deIban_recommendsSepaFree() {
        val result = PaymentRailClassifier.classify(
            sourceCurrency = "EUR",
            sourceBankCountry = "GB",
            beneficiary = ibanPayee("DE89370400440532099999"),
        )
        assertEquals(PaymentType.SEPA, result.recommended)
        val sepa = result.assessments.getValue(PaymentType.SEPA)
        assertTrue(sepa.eligible)
        assertNull(sepa.conversionNote)
        assertTrue(sepa.feeText.contains("Free"))
    }

    @Test
    fun gbpSource_deIban_sepaEligibleWithConversionNote() {
        val result = PaymentRailClassifier.classify(
            sourceCurrency = "GBP",
            sourceBankCountry = "GB",
            beneficiary = ibanPayee("DE89370400440532099999"),
        )
        assertEquals(PaymentType.SEPA, result.recommended)
        val sepa = result.assessments.getValue(PaymentType.SEPA)
        assertTrue(sepa.eligible)
        assertNotNull(sepa.conversionNote)
        assertTrue(sepa.conversionNote.contains("GBP"))
        assertFalse(sepa.feeText.contains("Free"))
    }

    @Test
    fun obpInternal_sameBankCountry_isDomestic() {
        val result = PaymentRailClassifier.classify(
            sourceCurrency = "EUR",
            sourceBankCountry = "GB",
            beneficiary = obpPayee(),
            destinationBankCountry = "GB",
        )
        assertEquals(PaymentType.DOMESTIC, result.recommended)
        assertTrue(result.assessments.getValue(PaymentType.DOMESTIC).eligible)
        val sepa = result.assessments.getValue(PaymentType.SEPA)
        assertFalse(sepa.eligible)
        assertNotNull(sepa.reason)
        assertFalse(result.assessments.getValue(PaymentType.INTERNATIONAL).eligible)
    }

    @Test
    fun obpCrossCountry_fallsBackToInternational() {
        val result = PaymentRailClassifier.classify(
            sourceCurrency = "GBP",
            sourceBankCountry = "GB",
            beneficiary = obpPayee(bankId = "neon.bank.eu"),
            destinationBankCountry = "DE",
        )
        assertEquals(PaymentType.INTERNATIONAL, result.recommended)
        assertFalse(result.assessments.getValue(PaymentType.DOMESTIC).eligible)
        assertFalse(result.assessments.getValue(PaymentType.SEPA).eligible)
        assertTrue(result.assessments.getValue(PaymentType.INTERNATIONAL).eligible)
    }

    @Test
    fun nonSepaIban_internationalOnly() {
        val result = PaymentRailClassifier.classify(
            sourceCurrency = "EUR",
            sourceBankCountry = "GB",
            beneficiary = ibanPayee("AE070331234567890123456", currency = "AED", bankAddress = "EBILAEAD"),
        )
        assertEquals(PaymentType.INTERNATIONAL, result.recommended)
        val sepa = result.assessments.getValue(PaymentType.SEPA)
        assertFalse(sepa.eligible)
        assertNotNull(sepa.reason)
        assertFalse(result.assessments.getValue(PaymentType.DOMESTIC).eligible)
    }

    @Test
    fun unknownSourceCountry_domesticIneligibleWithReason() {
        val result = PaymentRailClassifier.classify(
            sourceCurrency = "EUR",
            sourceBankCountry = "",
            beneficiary = ibanPayee("DE89370400440532099999"),
        )
        val domestic = result.assessments.getValue(PaymentType.DOMESTIC)
        assertFalse(domestic.eligible)
        assertNotNull(domestic.reason)
        assertEquals(PaymentType.SEPA, result.recommended)
    }

    @Test
    fun noBeneficiary_neutralAllEligible() {
        val result = PaymentRailClassifier.classify(
            sourceCurrency = "EUR",
            sourceBankCountry = "GB",
            beneficiary = null,
        )
        assertTrue(result.assessments.values.all { it.eligible })
        assertEquals(PaymentType.SEPA, result.recommended)
    }

    @Test
    fun domesticNotRecommendedOnCurrencyMismatch() {
        val result = PaymentRailClassifier.classify(
            sourceCurrency = "GBP",
            sourceBankCountry = "GB",
            beneficiary = ibanPayee("GB29TESC60161331926819", currency = "EUR", bankAddress = "TESCGB2L"),
        )
        assertTrue(result.assessments.getValue(PaymentType.DOMESTIC).eligible)
        assertEquals(PaymentType.SEPA, result.recommended)
    }
}
