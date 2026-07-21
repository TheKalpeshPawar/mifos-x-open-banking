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
 * Full account model for the account-detail screen, derived from OBIE `GET /accounts/{AccountId}`.
 *
 * Carries the servicer identification and status timestamp that [BankAccount], the list-oriented
 * model, does not. Balances arrive from a separate OBIE endpoint and are modelled as
 * [AccountBalanceLine].
 *
 * @property accountId OBIE `AccountId`, the key for balance, direct-debit and standing-order lookups.
 * @property nickname Human-readable account name (OBIE `Nickname`, falling back to `Name`).
 * @property accountSubType OBIE `AccountSubType` (e.g. `CurrentAccount`), rendered uppercase.
 * @property currency ISO-4217 currency code.
 * @property sortCode Six-digit UK sort code, unformatted (e.g. `400515`).
 * @property accountNumber Eight-digit UK account number.
 * @property servicerIdentification OBIE `Servicer.Identification` — the BIC of the servicing
 *   institution (e.g. `MIDLGB2105V`). Empty string when the payload carried no servicer.
 * @property statusUpdateDateTime OBIE `StatusUpdateDateTime` as an ISO-8601 string; formatted for
 *   display in the ViewModel. Empty string when absent.
 */
@Serializable
data class AccountDetail(
    val accountId: String,
    val nickname: String,
    val accountSubType: String,
    val currency: String,
    val sortCode: String,
    val accountNumber: String,
    val servicerIdentification: String = "",
    val statusUpdateDateTime: String = "",
)
