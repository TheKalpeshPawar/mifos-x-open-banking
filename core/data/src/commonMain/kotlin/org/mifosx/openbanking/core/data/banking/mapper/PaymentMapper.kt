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

import org.mifosx.openbanking.core.common.formatMinorUnits
import org.mifosx.openbanking.core.model.banking.BankAccount
import org.mifosx.openbanking.core.model.banking.BeneficiaryScheme
import org.mifosx.openbanking.core.model.banking.payment.CreditorSelection
import org.mifosx.openbanking.core.model.banking.payment.PaymentDraft
import org.mifosx.openbanking.core.model.banking.payment.PaymentReceipt
import org.mifosx.openbanking.core.model.banking.payment.PaymentStatus
import org.mifosx.openbanking.core.network.model.pisp.domesticPayment.request.CreditorAccount
import org.mifosx.openbanking.core.network.model.pisp.domesticPayment.request.Data
import org.mifosx.openbanking.core.network.model.pisp.domesticPayment.request.DebtorAccount
import org.mifosx.openbanking.core.network.model.pisp.domesticPayment.request.DomesticPaymentConsentRequest
import org.mifosx.openbanking.core.network.model.pisp.domesticPayment.request.DomesticPaymentRequest
import org.mifosx.openbanking.core.network.model.pisp.domesticPayment.request.Initiation
import org.mifosx.openbanking.core.network.model.pisp.domesticPayment.request.InstructedAmount
import org.mifosx.openbanking.core.network.model.pisp.domesticPayment.request.RemittanceInformation
import org.mifosx.openbanking.core.network.model.pisp.domesticPayment.request.Risk
import org.mifosx.openbanking.core.network.model.pisp.domesticPayment.response.DomesticPaymentConsentResponse
import org.mifosx.openbanking.core.network.model.pisp.domesticPayment.response.DomesticPaymentResponse

private const val MINOR_UNITS_PER_MAJOR = 100L
private const val FRACTION_DIGITS = 2

private const val SCHEME_SORT_CODE = "UK.OBIE.SortCodeAccountNumber"
private const val SCHEME_IBAN = "UK.OBIE.IBAN"
private const val SCHEME_PAYM = "UK.OBIE.Paym"
private const val SCHEME_PAN = "UK.OBIE.PAN"

/** A payment the PSU makes to one of their own accounts, as opposed to anyone else's. */
private const val CONTEXT_TRANSFER_TO_SELF = "TransferToSelf"
private const val CONTEXT_TRANSFER_TO_THIRD_PARTY = "TransferToThirdParty"

/**
 * Builds the OBIE `Initiation` for [this] draft.
 *
 * Pure and total: the same draft always yields an equal `Initiation`, which is what lets the payment
 * be staged and then submitted from one draft and still satisfy the byte-identity rule the bank
 * enforces with `U008`. Nothing here reads a clock, a random source, or any state outside the draft
 * — the two identifiers and the amount were all fixed when the draft was created.
 */
internal fun PaymentDraft.toInitiation(): Initiation = Initiation(
    instructionIdentification = instructionIdentification,
    endToEndIdentification = endToEndIdentification,
    instructedAmount = InstructedAmount(
        amount = amountMinorUnits.toMajorUnitString(),
        currency = currency,
    ),
    debtorAccount = debtorAccount.toObieDebtor(),
    creditorAccount = creditor.toObieCreditor(),
    remittanceInformation = reference
        ?.takeIf { it.isNotBlank() }
        ?.let { RemittanceInformation(unstructured = listOf(it)) },
)

/**
 * The `Risk` block for a consumer payment.
 *
 * Carries the payment context and nothing else. The three merchant fields OBIE also allows —
 * `MerchantCategoryCode`, `MerchantCustomerIdentification`, `DeliveryAddress` — describe a merchant
 * collecting from a shopper, which this app never is; HSBC's own Postman example sends all three and
 * is the wrong shape to copy.
 */
internal fun PaymentDraft.toRisk(): Risk = Risk(
    paymentContextCode = if (creditor.isOwnAccount) {
        CONTEXT_TRANSFER_TO_SELF
    } else {
        CONTEXT_TRANSFER_TO_THIRD_PARTY
    },
)

/** The staging body: the initiation the PSU approved, with no consent id yet to reference. */
internal fun PaymentDraft.toConsentRequest(): DomesticPaymentConsentRequest = DomesticPaymentConsentRequest(
    data = Data(initiation = toInitiation()),
    risk = toRisk(),
)

/** The submission body: the same initiation, now bound to the authorised [consentId]. */
internal fun PaymentDraft.toPaymentRequest(consentId: String): DomesticPaymentRequest = DomesticPaymentRequest(
    data = Data(consentId = consentId, initiation = toInitiation()),
    risk = toRisk(),
)

private fun BankAccount.toObieDebtor(): DebtorAccount = DebtorAccount(
    schemeName = SCHEME_SORT_CODE,
    identification = rawIdentification.takeIf { it.isNotBlank() } ?: (sortCode + accountNumber),
    name = nickname.takeIf { it.isNotBlank() },
)

private fun CreditorSelection.toObieCreditor(): CreditorAccount = CreditorAccount(
    schemeName = scheme.toObieSchemeName(),
    identification = identification,
    name = name.takeIf { it.isNotBlank() },
)

private fun BeneficiaryScheme.toObieSchemeName(): String = when (this) {
    BeneficiaryScheme.SortCode, BeneficiaryScheme.Account -> SCHEME_SORT_CODE
    BeneficiaryScheme.Iban -> SCHEME_IBAN
    BeneficiaryScheme.Paym -> SCHEME_PAYM
    BeneficiaryScheme.Card -> SCHEME_PAN
}

/**
 * Renders minor units as the major-unit decimal string OBIE expects, e.g. `85000` to `"850.00"`.
 *
 * Done with integer arithmetic rather than a floating-point divide: a payment amount must not pick
 * up a representation error on its way to the wire.
 */
private fun Long.toMajorUnitString(): String {
    val major = this / MINOR_UNITS_PER_MAJOR
    val minor = (this % MINOR_UNITS_PER_MAJOR).toString().padStart(FRACTION_DIGITS, '0')
    return "$major.$minor"
}

/** Reads the staged consent's id and status back off the response. */
internal fun DomesticPaymentConsentResponse.consentIdOrNull(): String? =
    data?.consentId?.takeIf { it.isNotBlank() }

internal fun DomesticPaymentConsentResponse.statusOrEmpty(): String = data?.status.orEmpty()

/**
 * Maps a submitted payment onto the receipt the success and status screens render.
 *
 * Amount and creditor are taken from the `Initiation` the bank echoes rather than from the local
 * draft, so what is shown is what the bank recorded.
 */
internal fun DomesticPaymentResponse.toPaymentReceipt(): PaymentReceipt {
    val initiation = data?.initiation
    val amount = initiation?.instructedAmount
    return PaymentReceipt(
        domesticPaymentId = data?.domesticPaymentId.orEmpty(),
        consentId = data?.consentId.orEmpty(),
        status = PaymentStatus.fromWire(data?.status),
        statusUpdateDateTime = data?.statusUpdateDateTime.orEmpty(),
        amountLabel = formatMinorUnits(
            minorUnits = amount?.amount.toMinorUnits(),
            currency = amount?.currency.orEmpty(),
        ),
        creditorName = initiation?.creditorAccount?.name.orEmpty(),
        reference = initiation?.remittanceInformation?.unstructured?.firstOrNull().orEmpty(),
        debtorIdentification = initiation?.debtorAccount?.identification.orEmpty(),
    )
}

/**
 * Parses an OBIE major-unit amount string back to minor units.
 *
 * Kept local rather than reusing `parseMinorUnits`, which returns null on a malformed value: a
 * receipt renders whatever the bank echoed, and an unreadable amount should show as zero rather
 * than fail the whole mapping.
 */
private fun String?.toMinorUnits(): Long {
    val raw = this?.trim().orEmpty()
    if (raw.isEmpty()) return 0L
    val negative = raw.startsWith('-')
    val digits = raw.removePrefix("-").removePrefix("+")
    val major = digits.substringBefore('.').toLongOrNull() ?: 0L
    val minor = digits.substringAfter('.', "")
        .padEnd(FRACTION_DIGITS, '0')
        .take(FRACTION_DIGITS)
        .toLongOrNull() ?: 0L
    val total = major * MINOR_UNITS_PER_MAJOR + minor
    return if (negative) -total else total
}
