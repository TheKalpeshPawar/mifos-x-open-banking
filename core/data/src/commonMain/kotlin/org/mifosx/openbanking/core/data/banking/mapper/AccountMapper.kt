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

import org.mifosx.openbanking.core.common.AccountScheme
import org.mifosx.openbanking.core.model.banking.BankAccount
import org.mifosx.openbanking.core.network.model.ais.accounts.Account
import org.mifosx.openbanking.core.network.model.ais.accounts.AccountsResponse

private const val SORT_CODE_SCHEME = "SortCode"
private const val PAN_SCHEME = "PAN"

/**
 * Flattens the OBIE `OBReadAccount6` payload into UI-facing [BankAccount]s. Accounts without an
 * `AccountId` are dropped — they cannot key a balance or transaction lookup.
 */
fun AccountsResponse.toBankAccounts(): List<BankAccount> =
    data?.account.orEmpty().mapNotNull { it.toBankAccountOrNull() }

private fun Account.toBankAccountOrNull(): BankAccount? {
    val id = accountId ?: return null
    val (identification, scheme) = resolveIdentification()
    return BankAccount(
        accountId = id,
        accountTypeCode = accountTypeCode ?: "",
        currency = currency ?: "",
        identification = identification,
        scheme = scheme,
        description = description ?: "",
        accountHolderName = account?.firstOrNull()?.name.orEmpty(),
    )
}

/**
 * Picks the identifier and its scheme, preferring the sort-code entry — the one the domestic payment
 * rail and the UI both want — then the PAN, then whatever the bank sent first.
 */
private fun Account.resolveIdentification(): Pair<String, AccountScheme> {
    val entries = account.orEmpty()
    val entry = entries.firstOrNull { it.schemeName?.contains(SORT_CODE_SCHEME, ignoreCase = true) == true }
        ?: entries.firstOrNull { it.schemeName?.contains(PAN_SCHEME, ignoreCase = true) == true }
        ?: entries.firstOrNull()
    val value = entry?.identification ?: identification ?: ""
    return value to AccountScheme.fromSchemeName(entry?.schemeName)
}
