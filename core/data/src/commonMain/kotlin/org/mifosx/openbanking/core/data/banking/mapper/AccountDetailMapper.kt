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

import org.mifosx.openbanking.core.model.banking.AccountBalanceLine
import org.mifosx.openbanking.core.model.banking.AccountDetail
import org.mifosx.openbanking.core.network.model.ais.accountDetails.Account
import org.mifosx.openbanking.core.network.model.ais.accountDetails.AccountDetailsResponse
import org.mifosx.openbanking.core.network.model.ais.balances.Balance
import org.mifosx.openbanking.core.network.model.ais.balances.BalancesResponse

private const val SORT_CODE_LENGTH = 6
private const val ACCOUNT_NUMBER_LENGTH = 8
private const val SORT_CODE_SCHEME = "SortCode"

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
    val flattened = resolveIdentification()
    return AccountDetail(
        accountId = id,
        nickname = resolveNickname(fallback = id),
        accountSubType = resolveSubType(),
        currency = currency ?: "",
        sortCode = flattened.take(SORT_CODE_LENGTH),
        accountNumber = flattened.drop(SORT_CODE_LENGTH).take(ACCOUNT_NUMBER_LENGTH),
        servicerIdentification = servicer?.identification ?: "",
        statusUpdateDateTime = statusUpdateDateTime ?: "",
        // Carried verbatim, deliberately bypassing resolveSubType()'s fallback chain: product
        // classification has to know whether a value came from AccountTypeCode or Description, and
        // the chain collapses that distinction away.
        accountTypeCode = accountTypeCode ?: "",
        description = description ?: "",
    )
}

/**
 * Picks the sort-code-scheme sub-account identification when the bank tags one, otherwise the
 * first sub-account, otherwise the top-level value.
 */
private fun Account.resolveIdentification(): String {
    val subAccounts = account.orEmpty()
    val bySortCodeScheme = subAccounts
        .firstOrNull { it.schemeName?.contains(SORT_CODE_SCHEME, ignoreCase = true) == true }
        ?.identification
    return bySortCodeScheme
        ?: subAccounts.firstOrNull()?.identification
        ?: identification
        ?: ""
}

/** OBIE `Nickname` is the user's own label; `Name` and `Description` are the bank's fallbacks. */
private fun Account.resolveNickname(fallback: String): String =
    nickname ?: name ?: description ?: fallback

/** `AccountSubType` is the precise value; the coarser type and category stand in when it is absent. */
private fun Account.resolveSubType(): String =
    accountSubType ?: accountTypeCode ?: accountCategory ?: description ?: ""

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
