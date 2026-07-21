/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.model.hsbcProduct

/**
 * HSBC's *product* classification, which is finer than OBIE's account type.
 *
 * HSBC gates several AIS endpoints on product type (see [HsbcProductCapability]), but OBIE's
 * `AccountTypeCode` enum is closed to six values — `CHAR`, `CARD`, `CACC`, `LOAN`, `MORT`, `SVGS`
 * — and has no code for a multi-currency wallet. So Global Money reports `CACC`, byte-identical to
 * an ordinary current account, and the standard field alone cannot tell them apart.
 *
 * [Unknown] is deliberately *permissive* downstream: an unrecognised product keeps every
 * capability and is corrected at runtime by the `U000` response, rather than having features
 * hidden from it on a guess.
 */
enum class HsbcProductType {
    PersonalCurrentAccount,
    Savings,
    CreditCard,
    ForeignCurrency,
    GlobalMoney,
    Unknown,
    ;

    companion object {
        /**
         * The only runtime signal that separates Global Money from a real current account.
         *
         * This is the most fragile line in the capability feature: it matches free text the bank
         * can reword at any time. Two things keep that from being a correctness bug — the
         * description is consulted *before* the type code (checking the code first resolves Global
         * Money to [PersonalCurrentAccount] and the hidden features come straight back), and the
         * runtime `U000` check corrects the answer if the wording ever changes.
         */
        private const val GLOBAL_MONEY_MARKER = "GLOBAL MONEY"

        /**
         * Resolves the product from whichever fields the bank populated.
         *
         * Accepts the OBIE `AccountSubType` enum case-insensitively plus the ISO-20022 cash-account
         * codes (`CACC`, `SVGS`, `CCRD`), mirroring the vocabulary the accounts screen already
         * normalises, so classification works whether the bank fills `AccountSubType` or only
         * `AccountTypeCode`.
         *
         * Returns [Unknown] rather than echoing an unrecognised input — callers branch on the enum,
         * and a pass-through string would silently miss every branch.
         */
        fun resolve(
            accountSubType: String,
            accountTypeCode: String,
            description: String,
        ): HsbcProductType {
            if (description.isGlobalMoney()) return GlobalMoney

            return accountSubType.toProductType()
                ?: accountTypeCode.toProductType()
                ?: Unknown
        }

        private fun String.isGlobalMoney(): Boolean =
            normalised().uppercase().contains(GLOBAL_MONEY_MARKER)

        private fun String.toProductType(): HsbcProductType? = when (normalised().lowercase()) {
            "currentaccount", "current", "cacc" -> PersonalCurrentAccount
            "savings", "savingsaccount", "svgs" -> Savings
            "creditcard", "credit", "card", "ccrd" -> CreditCard
            "foreigncurrency", "foreigncurrencyaccount" -> ForeignCurrency
            "globalmoney" -> GlobalMoney
            else -> null
        }

        /** Collapses runs of whitespace so `"GLOBAL  MONEY"` matches `"GLOBAL MONEY"`. */
        private fun String.normalised(): String = trim().replace(WHITESPACE_RUN, " ")

        private val WHITESPACE_RUN = Regex("\\s+")
    }
}
