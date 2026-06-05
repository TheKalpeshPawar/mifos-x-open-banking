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
    @SerialName("account_id") val accountId: String = "",
    val label: String = "",
    @SerialName("bank_id") val bankId: String = "",
    @SerialName("account_type") val accountType: String = "",
    @SerialName("product_code") val productCode: String = "",
    val number: String = "",
    val iban: String = "",
    @SerialName("swift_bic") val swiftBic: String = "",
    val balance: AmountOfMoney = AmountOfMoney(),
    @SerialName("account_routings") val accountRoutings: List<AccountRouting> = emptyList(),
    @SerialName("account_attributes") val accountAttributes: List<AccountAttribute> = emptyList(),
    val owners: List<AccountOwner> = emptyList(),
) {
    /** Stable id across list (`id`) and v7 detail (`account_id`) responses. */
    val accountIdOrId: String get() = id.ifBlank { accountId }

    /** Account type across list (`account_type`) and v7 detail (`product_code`) responses. */
    val typeOrProduct: String get() = accountType.ifBlank { productCode }

    /**
     * IBAN for this account, if known: the top-level `iban` field (older responses) else the
     * `IBAN`-scheme routing address. Blank when the account has no IBAN routing.
     */
    val ibanOrEmpty: String
        get() = iban.ifBlank {
            accountRoutings.firstOrNull { it.scheme.equals("IBAN", ignoreCase = true) }?.address.orEmpty()
        }

    /**
     * Human-facing account identifier for the list/detail rows: prefer IBAN, then the
     * account number, then any routing address, falling back to the raw id.
     */
    val displayIdentifier: String
        get() = iban.ifBlank {
            number.ifBlank {
                accountRoutings.firstOrNull { it.scheme.equals("IBAN", ignoreCase = true) }?.address
                    ?: accountRoutings.firstOrNull()?.address.orEmpty()
            }
        }
}

@Serializable
data class AccountOwner(
    val id: String = "",
    @SerialName("provider") val provider: String = "",
    @SerialName("display_name") val displayName: String = "",
)

/**
 * Product-level attribute attached to an account (OBP `account_attributes`), e.g.
 * `{name: "MONTHLY_FEE", type: "DOUBLE", value: "25.00"}` or
 * `{name: "PRIMARY_CCY", type: "STRING", value: "EUR"}`.
 */
@Serializable
data class AccountAttribute(
    val name: String = "",
    val type: String = "",
    val value: String = "",
)

/** Wrapper for the OBP account-list responses (`{ "accounts": [...] }`). */
@Serializable
data class AccountsResponse(
    val accounts: List<Account> = emptyList(),
)
