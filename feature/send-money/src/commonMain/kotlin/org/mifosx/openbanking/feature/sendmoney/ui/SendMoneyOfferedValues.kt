/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.sendmoney.ui

import org.mifosx.openbanking.core.model.banking.payment.ChargeBearer

/**
 * The currencies either selector offers: HSBC's own routing list, nineteen of them.
 *
 * Not a Global Money allowlist — the two-value `USD`/`EUR` list this replaced was one, and it was
 * wrong twice over. `CurrencyOfTransfer: GBP` stages `201`/`AWAU` (INT-04), and no allowlist is
 * applied at the consent stage at all: even `JPY`, which is not on this list, stages `201` (INT-14).
 * So the list is a statement about what HSBC documents itself as routing, not about what the consent
 * endpoint will accept.
 *
 * GBP leads because it is the default on both rails and the only currency proven end to end.
 * Three of the nineteen — GBP, USD, EUR — are exercised against this sandbox; the other sixteen come
 * from the implementation guide and are unverified here.
 */
internal val OFFERED_CURRENCIES: List<String> = listOf(
    "GBP", "EUR", "USD", "AUD", "CAD", "CHF", "CNY", "HKD", "SGD", "NZD",
    "AED", "CZK", "DKK", "NOK", "PLN", "SAR", "SEK", "ZAR", "THB",
)

/**
 * The charge bearers the PSU may choose — three of the four OBIE defines.
 *
 * [ChargeBearer.FollowingServiceLevel] is deliberately absent. HSBC's implementation guide restricts
 * the field to *"BorneByCreditor, BorneByDebtor, Shared"*, and sending the fourth is refused with
 * `400 UK.OBIE.Field.Invalid`. It stays in the enum because the enum is the OBIE codeset and
 * `ChargeBearer.fromWire` must still parse a value read back from the bank or from storage; what it
 * must not do is offer it.
 *
 * Only [ChargeBearer.BorneByCreditor] has settled a payment against this sandbox. `Shared` and
 * `BorneByDebtor` both staged `201` (INT-11, INT-12) but neither was carried through to submission.
 */
internal val OFFERED_CHARGE_BEARERS: List<ChargeBearer> = listOf(
    ChargeBearer.BorneByCreditor,
    ChargeBearer.BorneByDebtor,
    ChargeBearer.Shared,
)

/**
 * Whether `InstructedAmount.Currency` is one the bank will accept for this instruction.
 *
 * HSBC requires it to equal **either** the debtor account's currency **or** the currency of
 * transfer. Two independent selectors make the failing combination reachable — 250 USD out of a GBP
 * account arriving as EUR is three currencies and no relationship between them — so this is the only
 * thing standing between the PSU and a `400`, and it gates the review rather than warning beside it.
 *
 * [debtorCurrency] is blank when the PSU asked the bank to choose the account. There is no known
 * debtor currency in that case, so only the currency-of-transfer clause can hold.
 */
internal fun instructedCurrencyIsAcceptable(
    instructedCurrency: String,
    debtorCurrency: String,
    currencyOfTransfer: String,
): Boolean = (debtorCurrency.isNotBlank() && instructedCurrency.equals(debtorCurrency, ignoreCase = true)) ||
    instructedCurrency.equals(currencyOfTransfer, ignoreCase = true)
