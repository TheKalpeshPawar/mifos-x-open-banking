/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.model.banking.payment

import kotlinx.serialization.Serializable
import org.mifosx.openbanking.core.model.banking.BankAccount

/**
 * A completed payment instruction, fixed at the moment the PSU finishes the form.
 *
 * Everything the OBIE `Initiation` needs is here, and nothing is derived later. That is deliberate:
 * the payment is staged once and then submitted against the consent, and the submitted `Initiation`
 * must be byte-identical to the staged one or the bank refuses it with `U008`. Because building the
 * wire body from this draft is a pure function, staging and submitting from the same draft cannot
 * diverge — whereas regenerating any of these values between the two calls would break it.
 *
 * That is also why [instructionIdentification] and [endToEndIdentification] are stored rather than
 * minted per call: a retry that regenerates them is not a retry, it is a second payment instruction
 * the bank has no way to recognise as a duplicate.
 *
 * @property amountMinorUnits The amount in minor units; formatting to a major-unit decimal string
 *   happens once, at the wire boundary.
 * @property consentIdempotencyKey Guards the consent POST against duplication. Fixed for the life of
 *   the draft so a retry of *that* call is recognised as the same request.
 * @property paymentIdempotencyKey Guards the submission POST, and is deliberately a different value
 *   from [consentIdempotencyKey]. The two calls send different bodies — the submission carries
 *   `Data.ConsentId` and the consent does not — and the Read/Write profile forbids reusing one key
 *   across differing bodies, answering a repeat with `400 U029` (or `422` from v4.0.1) and
 *   permitting the ASPSP to treat it as fraudulent. Both are random per draft rather than derived
 *   from its contents, matching the `uuid.v4()` the bank's own reference collection mints before
 *   every write: a key derived from payer, payee, amount and reference would make two legitimate
 *   identical payments in the same day collide, and silently swallow the second.
 *
 * @property debtorAccount The account to pay from, or null to let the PSU choose it at the bank.
 *   Omitting `DebtorAccount` is a sanctioned shape — it is absent from HSBC's own international
 *   sample, and both rails accept a consent without it, after which the bank fills one in during
 *   authorisation. Note the account it picks is not predictable and may be a product this app would
 *   never have offered as a payer, so nothing downstream may assume a debtor it recognises.
 * @property reference Domestic only. International refuses `RemittanceInformation` with `U005`.
 * @property chargeBearer International only. Domestic refuses it; international requires it.
 */
@Serializable
data class PaymentDraft(
    val debtorAccount: BankAccount?,
    val creditor: CreditorSelection,
    val amountMinorUnits: Long,
    val currency: String,
    val reference: String?,
    val instructionIdentification: String,
    val endToEndIdentification: String,
    val consentIdempotencyKey: String,
    val paymentIdempotencyKey: String,
    val currencyOfTransfer: String? = null,
    val chargeBearer: ChargeBearer? = null,
)
