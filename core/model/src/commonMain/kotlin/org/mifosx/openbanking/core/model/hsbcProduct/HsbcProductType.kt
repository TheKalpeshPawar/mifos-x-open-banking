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
 * HSBC's product classification, finer than OBIE's account type.
 *
 * Global Money reports `CACC` like a current account, so the description tells them apart. [Unknown]
 * is permissive: an unrecognised product keeps every capability.
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
        /** The description marker that distinguishes a Global Money wallet (which reports `CACC`). */
        private const val GLOBAL_MONEY_MARKER = "GLOBAL MONEY"

        /**
         * Resolves the product from the `AccountTypeCode` and the `GLOBAL MONEY` description marker.
         * Returns [Unknown] for an unrecognised code.
         */
        fun resolve(
            accountTypeCode: String,
            description: String,
        ): HsbcProductType {
            if (description.isGlobalMoney()) return GlobalMoney

            return accountTypeCode.toProductType() ?: Unknown
        }

        private fun String.isGlobalMoney(): Boolean =
            uppercase().contains(GLOBAL_MONEY_MARKER)

        private fun String.toProductType(): HsbcProductType? {
            return when (lowercase()) {
                "cacc" -> PersonalCurrentAccount
                "svgs" -> Savings
                "card", "ccrd" -> CreditCard
                else -> null
            }
        }
    }
}
