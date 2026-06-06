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

import androidx.compose.runtime.Immutable
import org.mifosx.openbanking.core.model.obp.Counterparty

/** Eligibility verdict for one payment rail given the (source account, beneficiary) pair. */
@Immutable
data class RailAssessment(
    val eligible: Boolean,
    /** Why the rail is unavailable; null when eligible. */
    val reason: String? = null,
    /** FX hint when the rail is usable but needs conversion first (e.g. GBP account on SEPA). */
    val conversionNote: String? = null,
    val feeText: String = "",
)

/** Per-rail assessments plus the rail the app auto-selects. */
@Immutable
data class RailClassification(
    val assessments: Map<PaymentType, RailAssessment>,
    val recommended: PaymentType,
)

/**
 * Derives which payment rails a (source account, beneficiary) pair qualifies for, mirroring how
 * real banking apps infer the rail from recipient routing details instead of asking the user:
 * - SEPA moves EUR only, but the funding account may be any currency (the bank converts first),
 *   so eligibility keys on the recipient IBAN's country being in the SEPA zone.
 * - Domestic requires the recipient's bank to be in the same country as the source bank.
 * - International is the SWIFT fallback and needs a BIC (or resolvable OBP routing).
 *
 * Pure and synchronous: callers resolve bank countries (via BanksRepository) up front.
 */
object PaymentRailClassifier {

    fun classify(
        sourceCurrency: String,
        sourceBankCountry: String,
        beneficiary: Counterparty?,
        destinationBankCountry: String? = null,
    ): RailClassification {
        if (beneficiary == null) return neutral(sourceCurrency)
        val iban = beneficiary.ibanOrEmpty()
        val recipientCountry = recipientCountry(beneficiary, iban, destinationBankCountry)
        val domestic = assessDomestic(sourceBankCountry, recipientCountry)
        val sepa = assessSepa(sourceCurrency, iban)
        val international = assessInternational(beneficiary, sourceBankCountry, recipientCountry)
        return RailClassification(
            assessments = mapOf(
                PaymentType.SEPA to sepa,
                PaymentType.DOMESTIC to domestic,
                PaymentType.INTERNATIONAL to international,
            ),
            recommended = recommend(sourceCurrency, beneficiary.currency, domestic, sepa, international),
        )
    }

    private fun neutral(sourceCurrency: String) = RailClassification(
        assessments = mapOf(
            PaymentType.SEPA to RailAssessment(eligible = true, feeText = sepaFeeText(sourceCurrency)),
            PaymentType.DOMESTIC to RailAssessment(eligible = true, feeText = DOMESTIC_FEE),
            PaymentType.INTERNATIONAL to RailAssessment(eligible = true, feeText = INTERNATIONAL_FEE),
        ),
        recommended = PaymentType.SEPA,
    )

    private fun recipientCountry(beneficiary: Counterparty, iban: String, destinationBankCountry: String?): String =
        when {
            iban.length >= 2 -> iban.take(2).uppercase()
            beneficiary.otherBankRoutingScheme.equals("BIC", ignoreCase = true) &&
                beneficiary.otherBankRoutingAddress.length >= BIC_COUNTRY_END ->
                beneficiary.otherBankRoutingAddress.substring(BIC_COUNTRY_START, BIC_COUNTRY_END).uppercase()
            else -> destinationBankCountry?.uppercase().orEmpty()
        }

    private fun assessDomestic(sourceBankCountry: String, recipientCountry: String): RailAssessment = when {
        sourceBankCountry.isBlank() ->
            RailAssessment(eligible = false, reason = "Your bank's country is unknown")
        recipientCountry.isBlank() ->
            RailAssessment(eligible = false, reason = "Recipient's bank country is unknown")
        recipientCountry != sourceBankCountry ->
            RailAssessment(eligible = false, reason = "Recipient's bank is outside $sourceBankCountry")
        else -> RailAssessment(eligible = true, feeText = DOMESTIC_FEE)
    }

    private fun assessSepa(sourceCurrency: String, iban: String): RailAssessment {
        val country = iban.take(2).uppercase()
        return when {
            iban.isBlank() -> RailAssessment(eligible = false, reason = "Recipient has no IBAN")
            country !in SEPA_ZONE ->
                RailAssessment(eligible = false, reason = "Recipient IBAN is outside the SEPA zone")
            sourceCurrency.equals("EUR", ignoreCase = true) ->
                RailAssessment(eligible = true, feeText = SEPA_FEE_FREE)
            else -> RailAssessment(
                eligible = true,
                conversionNote = "Converted $sourceCurrency→EUR · FX fee applies",
                feeText = SEPA_FEE_FX,
            )
        }
    }

    private fun assessInternational(
        beneficiary: Counterparty,
        sourceBankCountry: String,
        recipientCountry: String,
    ): RailAssessment {
        val sameCountry = sourceBankCountry.isNotBlank() && recipientCountry == sourceBankCountry
        val hasBic = beneficiary.otherBankRoutingScheme.equals("BIC", ignoreCase = true) &&
            beneficiary.otherBankRoutingAddress.isNotBlank()
        val hasObpRouting = beneficiary.otherBankRoutingScheme.equals("OBP", ignoreCase = true) &&
            beneficiary.otherBankRoutingAddress.isNotBlank()
        return when {
            // A same-country payment never goes via SWIFT — disabled silently (the Domestic
            // chip being selected already tells the story; no reason line needed).
            sameCountry -> RailAssessment(eligible = false)
            hasBic || hasObpRouting -> RailAssessment(eligible = true, feeText = INTERNATIONAL_FEE)
            else -> RailAssessment(eligible = false, reason = "Requires the recipient bank's BIC")
        }
    }

    private fun recommend(
        sourceCurrency: String,
        recipientCurrency: String,
        domestic: RailAssessment,
        sepa: RailAssessment,
        international: RailAssessment,
    ): PaymentType {
        val currenciesMatch = recipientCurrency.isBlank() ||
            recipientCurrency.equals(sourceCurrency, ignoreCase = true)
        return when {
            domestic.eligible && currenciesMatch -> PaymentType.DOMESTIC
            sepa.eligible -> PaymentType.SEPA
            domestic.eligible -> PaymentType.DOMESTIC
            international.eligible -> PaymentType.INTERNATIONAL
            else -> PaymentType.INTERNATIONAL
        }
    }

    private fun sepaFeeText(sourceCurrency: String): String =
        if (sourceCurrency.equals("EUR", ignoreCase = true)) SEPA_FEE_FREE else SEPA_FEE_FX

    private const val SEPA_FEE_FREE = "Free (SEPA)"
    private const val SEPA_FEE_FX = "FX fee (SEPA)"
    private const val DOMESTIC_FEE = "Free (Domestic)"
    private const val INTERNATIONAL_FEE = "Varies (International)"
    private const val BIC_COUNTRY_START = 4
    private const val BIC_COUNTRY_END = 6

    /** EU-27 plus the non-EU SEPA participants (EPC list). */
    private val SEPA_ZONE = setOf(
        "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR", "DE", "GR", "HU", "IE", "IT",
        "LV", "LT", "LU", "MT", "NL", "PL", "PT", "RO", "SK", "SI", "ES", "SE",
        "AD", "CH", "GB", "GI", "IS", "LI", "MC", "NO", "SM", "VA",
    )
}

/** The beneficiary's IBAN from primary or secondary routing, or "" when absent. */
internal fun Counterparty.ibanOrEmpty(): String = when {
    otherAccountRoutingScheme.equals("IBAN", ignoreCase = true) -> otherAccountRoutingAddress
    otherAccountSecondaryRoutingScheme.equals("IBAN", ignoreCase = true) -> otherAccountSecondaryRoutingAddress
    else -> ""
}
