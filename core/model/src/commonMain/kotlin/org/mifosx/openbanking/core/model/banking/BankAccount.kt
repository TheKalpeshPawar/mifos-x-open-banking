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
import org.mifosx.openbanking.core.common.AccountScheme

/**
 * UI-facing account model derived from the OBIE `OBReadAccount6` payload.
 *
 * The OBIE shape nests the identifiers under `Account.Account[]`; this model flattens the fields the
 * home dashboard renders. Empty strings stand in for absent OBIE values so the UI never has to
 * null-check.
 *
 * @property accountId OBIE `AccountId`, the key for balance and transaction lookups.
 * @property accountTypeCode OBIE `AccountTypeCode` (e.g. `CACC`, `CARD`, `SVGS`).
 * @property currency ISO-4217 currency code.
 * @property identification The OBIE `Identification` in its raw form — a 14-digit sort-code + account
 *   number, a masked card number, or an IBAN. [scheme] says how to read it. Empty when the payload
 *   carried no identification.
 * @property scheme The identifier scheme [identification] is expressed in, resolved from OBIE
 *   `SchemeName`.
 * @property description OBIE `Description`, free text. Carried because it is the **only** signal that
 *   distinguishes a Global Money wallet: HSBC reports its `AccountTypeCode` as `CACC`, exactly as for
 *   an ordinary current account. Without it a wallet cannot be recognised until the bank refuses the
 *   payment. Empty when absent, and frequently filler in the sandbox, so it is a hint rather than a
 *   guarantee.
 * @property accountHolderName The account holder's name (OBIE nested `Account[].Name`) — the single
 *   display-name field; the bank never returns a `Nickname`/top-level `Name`.
 */
@Serializable
data class BankAccount(
    val accountId: String,
    val accountTypeCode: String,
    val currency: String,
    val identification: String = "",
    val scheme: AccountScheme = AccountScheme.Other,
    val description: String = "",
    val accountHolderName: String = "",
)
