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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OBP bank account. Shape covers the union of fields returned by the account-list
 * (`/banks/{bankId}/accounts`, `/my/accounts`) and account-detail endpoints; absent
 * fields fall back to defaults so a single model serves both responses.
 */
@Serializable
data class Account(
    val id: String = "",
    val label: String = "",
    @SerialName("bank_id") val bankId: String = "",
    @SerialName("account_type") val accountType: String = "",
    val number: String = "",
    val iban: String = "",
    @SerialName("swift_bic") val swiftBic: String = "",
    val balance: AmountOfMoney = AmountOfMoney(),
    @SerialName("account_routings") val accountRoutings: List<AccountRouting> = emptyList(),
    val owners: List<AccountOwner> = emptyList(),
)

@Serializable
data class AccountOwner(
    val id: String = "",
    @SerialName("provider") val provider: String = "",
    @SerialName("display_name") val displayName: String = "",
)

/** Wrapper for the OBP account-list responses (`{ "accounts": [...] }`). */
@Serializable
data class AccountsResponse(
    val accounts: List<Account> = emptyList(),
)
