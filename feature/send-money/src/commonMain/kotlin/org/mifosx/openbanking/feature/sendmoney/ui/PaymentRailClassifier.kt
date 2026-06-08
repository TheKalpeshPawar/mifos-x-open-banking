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

/** An OBP-hosted payee resolved to {bank, account} so it can be paid via the SANDBOX_TAN rail. */
@Immutable
data class SandboxTanDestination(val bankId: String, val accountId: String)

/** Per-rail assessments plus the rail the app auto-selects. */
@Immutable
data class RailClassification(
    val assessments: Map<PaymentType, RailAssessment>,
    val recommended: PaymentType,
    /**
     * Non-null only on a sandbox build for an OBP-hosted payee: the payment is routed through
     * SANDBOX_TAN (whose challenge the maker can self-answer) and the other rails are disabled.
     */
    val sandboxTan: SandboxTanDestination? = null,
)

/** Amount (in the account currency, EUR on the sandbox) at/above which OBP issues an SCA challenge. */
const val SANDBOX_SCA_THRESHOLD = 1000.0

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
        isSandbox: Boolean = false,
    ): RailClassification {
        // On a sandbox build, an OBP-hosted payee is paid via SANDBOX_TAN so the SCA self-completes;
        // the production rails are disabled for that payee.
        val obpDest = if (isSandbox && beneficiary != null) beneficiary.obpDestination() else null
        return when {
            obpDest != null -> sandboxTanClassification(obpDest)
            beneficiary == null -> neutral(sourceCurrency)
            else -> classifyRails(sourceCurrency, sourceBankCountry, beneficiary, destinationBankCountry)
        }
    }

    private fun classifyRails(
        sourceCurrency: String,
        sourceBankCountry: String,
        beneficiary: Counterparty,
        destinationBankCountry: String?,
    ): RailClassification {
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

    /** An OBP-hosted payee: SANDBOX_TAN is the only rail; the others are disabled with a note. */
    private fun sandboxTanClassification(dest: SandboxTanDestination): RailClassification {
        val reason = "Sent instantly as an internal bank transfer"
        return RailClassification(
            assessments = mapOf(
                PaymentType.SEPA to RailAssessment(eligible = false, reason = reason),
                PaymentType.DOMESTIC to RailAssessment(eligible = false, reason = reason),
                PaymentType.INTERNATIONAL to RailAssessment(eligible = false, reason = reason),
            ),
            recommended = PaymentType.SEPA,
            sandboxTan = dest,
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

/**
 * The OBP-hosted destination when both bank and account routing schemes are OBP — i.e. the payee is
 * an account on an OBP bank, payable by {bank_id, account_id} via SANDBOX_TAN. Null otherwise
 * (external/IBAN-only payees).
 */
internal fun Counterparty.obpDestination(): SandboxTanDestination? {
    val bothObp = otherBankRoutingScheme.equals("OBP", ignoreCase = true) &&
        otherAccountRoutingScheme.equals("OBP", ignoreCase = true)
    val hasAddresses = otherBankRoutingAddress.isNotBlank() && otherAccountRoutingAddress.isNotBlank()
    return if (bothObp && hasAddresses) {
        SandboxTanDestination(bankId = otherBankRoutingAddress, accountId = otherAccountRoutingAddress)
    } else {
        null
    }
}

/** The beneficiary's IBAN from primary or secondary routing, or "" when absent. */
internal fun Counterparty.ibanOrEmpty(): String = when {
    otherAccountRoutingScheme.equals("IBAN", ignoreCase = true) -> otherAccountRoutingAddress
    otherAccountSecondaryRoutingScheme.equals("IBAN", ignoreCase = true) -> otherAccountSecondaryRoutingAddress
    else -> ""
}
