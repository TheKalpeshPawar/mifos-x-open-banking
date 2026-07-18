/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.common

private const val UK_SORT_CODE_LENGTH = 6
private const val SORT_CODE_GROUP = 2
private const val CARD_MASK_VISIBLE = 4
private const val CARD_MASK_PREFIX = "•••• "
private const val IBAN_GROUP = 4
private const val SORT_CODE_SEPARATOR = "  "
private const val SUBTYPE_CURRENT = "CurrentAccount"
private const val SUBTYPE_SAVINGS = "Savings"
private const val SUBTYPE_CREDIT_CARD = "CreditCard"
private const val SUBTYPE_GLOBAL_MONEY = "GlobalMoney"
private const val SUBTYPE_GLOBAL_WALLET = "GlobalWallet"

/**
 * Formats a six-digit UK sort code into the conventional dash-grouped form, e.g. `"400515"` →
 * `"40-05-15"`. Inputs that are not exactly six digits are returned unchanged.
 */
fun formatSortCode(sortCode: String): String =
    if (sortCode.length == UK_SORT_CODE_LENGTH) {
        sortCode.chunked(SORT_CODE_GROUP).joinToString("-")
    } else {
        sortCode
    }

/**
 * Masks all but the last four digits of a card number, e.g. `"4111111111117654"` → `"•••• 7654"`.
 * Shorter inputs simply keep whatever trailing characters they have.
 */
fun maskCardNumber(raw: String): String = CARD_MASK_PREFIX + raw.takeLast(CARD_MASK_VISIBLE)

/**
 * Groups an IBAN into space-separated blocks of four, e.g. `"GB29HBUK40051512345678"` →
 * `"GB29 HBUK 4005 1512 3456 78"`. Inputs of four characters or fewer are returned unchanged.
 */
fun formatIban(raw: String): String =
    if (raw.length <= IBAN_GROUP) raw else raw.chunked(IBAN_GROUP).joinToString(" ")

/**
 * Renders the account identifier per account subtype, choosing the display form the type expects:
 * sort code + account number for current/savings, a masked card number for credit cards, a grouped
 * IBAN for the global-money wallets, and the raw identification for anything else.
 */
fun formatAccountIdentifier(
    subType: String,
    rawIdentification: String,
    sortCode: String,
    accountNumber: String,
): String = when (subType) {
    SUBTYPE_CURRENT, SUBTYPE_SAVINGS -> formatSortCode(sortCode) + SORT_CODE_SEPARATOR + accountNumber
    SUBTYPE_CREDIT_CARD -> maskCardNumber(rawIdentification)
    SUBTYPE_GLOBAL_MONEY, SUBTYPE_GLOBAL_WALLET -> formatIban(rawIdentification)
    else -> rawIdentification
}
