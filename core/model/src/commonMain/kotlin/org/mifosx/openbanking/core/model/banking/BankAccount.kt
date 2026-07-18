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
 * UI-facing account model derived from the OBIE `OBReadAccount6` payload.
 *
 * The OBIE shape nests the sort-code / account-number under `Account.Account[]`; this model
 * flattens the fields the home dashboard renders. Empty strings stand in for absent OBIE
 * values so the UI never has to null-check.
 *
 * @property accountId OBIE `AccountId`, the key for balance and transaction lookups.
 * @property nickname Human-readable account name (OBIE `Nickname`/`Name`).
 * @property accountSubType OBIE `AccountSubType` (e.g. `CurrentAccount`, `Savings`).
 * @property currency ISO-4217 currency code.
 * @property sortCode Six-digit UK sort code, unformatted (e.g. `400515`).
 * @property accountNumber Eight-digit UK account number.
 */
@Serializable
data class BankAccount(
    val accountId: String,
    val nickname: String,
    val accountSubType: String,
    val currency: String,
    val sortCode: String,
    val accountNumber: String,
)
