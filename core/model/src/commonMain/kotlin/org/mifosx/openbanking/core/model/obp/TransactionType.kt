/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.model.obp

/**
 * Standard transaction-type short codes provisioned on the OBP bank and carried per
 * transaction via the `TXN_TYPE` attribute (see `scripts/sandbox`). The codes are the
 * single source of truth on the client; OBP attribute definitions only declare the name,
 * not the allowed values, so unknown/legacy values fall back to [UNKNOWN].
 *
 * [isCardInstrument] marks the types a physical/virtual card actually produces
 * (point-of-sale, online, ATM) — useful when distinguishing card spend from transfers.
 */
enum class TransactionType(val code: String, val label: String, val isCardInstrument: Boolean) {
    POS("POS", "Card payment", true),
    ECOM("ECOM", "Online payment", true),
    ATM("ATM", "ATM withdrawal", true),
    TFR("TFR", "Transfer", false),
    DD("DD", "Direct debit", false),
    SO("SO", "Standing order", false),
    REF("REF", "Refund", false),
    FEE("FEE", "Fee", false),
    INT("INT", "Interest", false),
    SAL("SAL", "Salary", false),
    DEP("DEP", "Deposit", false),
    CHG("CHG", "Charge", false),
    UNKNOWN("", "Transaction", false),
    ;

    companion object {
        /** Resolve a TXN_TYPE code (case-insensitive) to its standard type, or [UNKNOWN]. */
        fun fromCode(code: String?): TransactionType =
            entries.firstOrNull { it.code.isNotEmpty() && it.code.equals(code, ignoreCase = true) } ?: UNKNOWN
    }
}
