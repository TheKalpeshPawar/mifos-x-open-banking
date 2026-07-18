/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking.mapper

import org.mifosx.openbanking.core.model.banking.BankAccount
import org.mifosx.openbanking.core.network.model.ais.accounts.Account
import org.mifosx.openbanking.core.network.model.ais.accounts.AccountsResponse

private const val SORT_CODE_LENGTH = 6
private const val ACCOUNT_NUMBER_LENGTH = 8
private const val SORT_CODE_SCHEME = "SortCode"

/**
 * Flattens the OBIE `OBReadAccount6` payload into UI-facing [BankAccount]s. Accounts without an
 * `AccountId` are dropped — they cannot key a balance or transaction lookup.
 */
fun AccountsResponse.toBankAccounts(): List<BankAccount> =
    data?.account.orEmpty().mapNotNull { it.toBankAccountOrNull() }

private fun Account.toBankAccountOrNull(): BankAccount? {
    val id = accountId ?: return null
    val identification = account.orEmpty()
        .firstOrNull { it.schemeName?.contains(SORT_CODE_SCHEME, ignoreCase = true) == true }
        ?.identification
        ?: account.orEmpty().firstOrNull()?.identification
        ?: identification
        ?: ""
    return BankAccount(
        accountId = id,
        nickname = name ?: description ?: id,
        accountSubType = accountSubType ?: accountTypeCode ?: accountCategory ?: description ?: "",
        currency = currency ?: "",
        sortCode = identification.take(SORT_CODE_LENGTH),
        accountNumber = identification.drop(SORT_CODE_LENGTH).take(ACCOUNT_NUMBER_LENGTH),
        rawIdentification = identification,
    )
}
