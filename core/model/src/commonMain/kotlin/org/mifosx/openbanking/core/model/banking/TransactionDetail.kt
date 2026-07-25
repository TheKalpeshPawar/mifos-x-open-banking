/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.model.banking

import kotlinx.serialization.Serializable

/**
 * Rich single-transaction domain model for the transaction-detail screen.
 *
 * OBIE v4.0 AIS has no single-transaction endpoint, so the detail record is resolved by filtering the
 * account's `OBReadTransaction6` list by `transactionId`. The slim [TransactionItem] the transactions
 * list caches drops most of the payload; this model carries the fields the detail view needs — the
 * merchant, status, both dates, the client-derived [category], the running balance and the proprietary
 * bank code.
 *
 * @property transactionId OBIE `TransactionId` (may be blank for pending entries).
 * @property accountId OBIE `AccountId` the transaction belongs to.
 * @property amount Raw decimal amount string (major units, e.g. `"42.17"`).
 * @property currency ISO-4217 currency code of [amount].
 * @property isCredit True when OBIE `CreditDebitIndicator` is `Credit` (money in), false for debits.
 * @property merchantName OBIE `MerchantDetails.MerchantName`, or null when the payload carries none
 *   (direct credits, bank transfers) — callers fall back to [transactionInformation].
 * @property status OBIE `Status`, e.g. `Booked` / `Pending`.
 * @property bookingDateTime Raw ISO-8601 `BookingDateTime`.
 * @property valueDateTime Raw ISO-8601 `ValueDateTime`.
 * @property category Client-derived personal-finance category (from the MCC / proprietary code).
 * @property merchantCategoryCode ISO-18245 `MerchantDetails.MerchantCategoryCode`, or null when the
 *   transaction is not card-originated — the MCC row hides when this is null.
 * @property balanceAmount Raw decimal running-balance amount (OBIE `Balance.Amount.Amount`).
 * @property balanceCurrency ISO-4217 currency of [balanceAmount].
 * @property balanceIsCredit True when OBIE `Balance.CreditDebitIndicator` is `Credit`. **Do not use
 *   this to sign [balanceAmount] for display.** HSBC reports it per transaction as the *transaction's*
 *   direction rather than the balance's, so a debit on an account in credit arrives as `Debit`: the
 *   sandbox returns `Balance {Debit, ITBD, 21530.92}` for an account whose `/balances` reports
 *   `{Credit, ITBD, 21530.92}` — the same figure and type, the opposite indicator. Signing on it
 *   renders a healthy balance as overdrawn. Kept because it faithfully records the payload.
 * @property transactionInformation Free-text reference (OBIE `TransactionInformation`).
 * @property proprietaryCode HSBC proprietary transaction code (`ProprietaryBankTransactionCode.Code`).
 * @property proprietaryIssuer Issuer of [proprietaryCode] (`ProprietaryBankTransactionCode.Issuer`).
 */
@Serializable
data class TransactionDetail(
    val transactionId: String,
    val accountId: String,
    val amount: String,
    val currency: String,
    val isCredit: Boolean,
    val merchantName: String?,
    val status: String,
    val bookingDateTime: String,
    val valueDateTime: String,
    val category: TransactionCategory,
    val merchantCategoryCode: String?,
    val balanceAmount: String,
    val balanceCurrency: String,
    val balanceIsCredit: Boolean,
    val transactionInformation: String,
    val proprietaryCode: String,
    val proprietaryIssuer: String,
)
