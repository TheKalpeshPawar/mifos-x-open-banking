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
 * @property accountHolderName The account holder's name (OBIE nested `Account[].Name`).
 * @property accountSubType OBIE `AccountSubType` (e.g. `CurrentAccount`, `Savings`).
 * @property currency ISO-4217 currency code.
 * @property sortCode Six-digit UK sort code, unformatted (e.g. `400515`).
 * @property accountNumber Eight-digit UK account number.
 * @property rawIdentification The unflattened OBIE `Identification` value (e.g. a full card number
 *   or IBAN), preserved so the UI can render the card/IBAN form the sort-code split would lose.
 *   Empty string when the OBIE payload carried no identification.
 * @property description OBIE `Description`, free text. Carried because it is the **only** signal
 *   that distinguishes a Global Money wallet: HSBC reports its `AccountTypeCode` as `CACC`, exactly
 *   as for an ordinary current account, and `HsbcProductType.resolve` matches on this string.
 *   Without it a wallet cannot be recognised until the bank refuses the payment. Empty when absent,
 *   and frequently filler in the sandbox, so it is a hint rather than a guarantee.
 */
@Serializable
data class BankAccount(
    val accountId: String,
    val nickname: String,
    val accountSubType: String,
    val currency: String,
    val sortCode: String,
    val accountNumber: String,
    val rawIdentification: String = "",
    val description: String = "",
    val accountHolderName: String = "",
)
