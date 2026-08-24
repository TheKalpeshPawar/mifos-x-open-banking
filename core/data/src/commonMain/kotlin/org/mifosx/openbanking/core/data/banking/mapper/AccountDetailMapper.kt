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
import org.mifosx.openbanking.core.model.banking.AccountBalanceLine
import org.mifosx.openbanking.core.model.banking.AccountDetail
import org.mifosx.openbanking.core.network.model.ais.accountDetails.Account
import org.mifosx.openbanking.core.network.model.ais.accountDetails.AccountDetailsResponse
import org.mifosx.openbanking.core.network.model.ais.balances.Balance
import org.mifosx.openbanking.core.network.model.ais.balances.BalancesResponse

private const val SORT_CODE_SCHEME = "SortCode"
private const val PAN_SCHEME = "PAN"

/**
 * Flattens the OBIE `OBReadAccount6` detail payload into a single [AccountDetail].
 *
 * The endpoint is account-scoped so the array holds at most one entry; entries without an
 * `AccountId` are skipped because the id keys every downstream lookup. Returns `null` when the
 * payload carries no usable account, which the store surfaces as an empty read.
 */
fun AccountDetailsResponse.toAccountDetail(): AccountDetail? =
    data?.account.orEmpty().firstNotNullOfOrNull { it.toAccountDetailOrNull() }

private fun Account.toAccountDetailOrNull(): AccountDetail? {
    val id = accountId ?: return null
    val (identification, scheme) = resolveIdentification()
    return AccountDetail(
        accountId = id,
        accountTypeCode = accountTypeCode ?: "",
        currency = currency ?: "",
        identification = identification,
        scheme = scheme,
        servicerIdentification = servicer?.identification ?: "",
        statusUpdateDateTime = statusUpdateDateTime ?: "",
        description = description ?: "",
        accountHolderName = account?.firstOrNull()?.name.orEmpty(),
    )
}

/**
 * Picks the identifier and its scheme, preferring the sort-code entry, then the PAN, then whatever
 * the bank sent first.
 */
private fun Account.resolveIdentification(): Pair<String, AccountScheme> {
    val entries = account.orEmpty()
    val entry = entries.firstOrNull { it.schemeName?.contains(SORT_CODE_SCHEME, ignoreCase = true) == true }
        ?: entries.firstOrNull { it.schemeName?.contains(PAN_SCHEME, ignoreCase = true) == true }
        ?: entries.firstOrNull()
    val value = entry?.identification ?: identification ?: ""
    return value to AccountScheme.fromSchemeName(entry?.schemeName)
}

/**
 * Maps the OBIE `OBReadBalance1` payload into one [AccountBalanceLine] per typed row, preserving
 * the order the bank returned. Rows missing a `Type` or an `Amount.Amount` are dropped — the
 * account-detail list renders both, so a row without either has nothing to show.
 */
fun BalancesResponse.toAccountBalanceLines(): List<AccountBalanceLine> =
    data?.balance.orEmpty().mapNotNull { it.toAccountBalanceLineOrNull() }

private fun Balance.toAccountBalanceLineOrNull(): AccountBalanceLine? {
    val rowType = type
    val rowAmount = amount?.amount
    return if (rowType == null || rowAmount == null) {
        null
    } else {
        AccountBalanceLine(
            type = rowType,
            amount = rowAmount,
            currency = amount?.currency ?: "",
            dateTime = dateTime ?: "",
        )
    }
}
