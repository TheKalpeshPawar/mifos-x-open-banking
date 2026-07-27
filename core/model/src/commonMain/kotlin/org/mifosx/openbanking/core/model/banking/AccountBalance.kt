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

/**
 * UI-facing balance for a single account, resolved from the OBIE `OBReadBalance1` list.
 *
 * OBIE returns several balance rows per account keyed by `Type`; the mapper collapses them to
 * the two figures the hero card shows. Amounts stay as raw decimal strings (OBIE emits them as
 * strings) and are formatted for display at the presentation layer.
 *
 * @property accountId OBIE `AccountId` these balances belong to.
 * @property currency ISO-4217 currency code.
 * @property currentAmount Booked balance (OBIE `InterimBooked`, falling back to the first row).
 * @property availableAmount Available-to-spend balance (OBIE `InterimAvailable`).
 */
data class AccountBalance(
    val accountId: String,
    val currency: String,
    val currentAmount: String,
    val availableAmount: String,
)
