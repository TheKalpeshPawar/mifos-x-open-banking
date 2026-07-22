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

import org.mifosx.openbanking.core.model.banking.StatementBalanceLine
import org.mifosx.openbanking.core.model.banking.StatementCharge
import org.mifosx.openbanking.core.model.banking.StatementDetail
import org.mifosx.openbanking.core.network.model.ais.statementDetails.Statement
import org.mifosx.openbanking.core.network.model.ais.statementDetails.StatementAmount
import org.mifosx.openbanking.core.network.model.ais.statementDetails.StatementDetailsResponse
import org.mifosx.openbanking.core.network.model.ais.statementDetails.StatementFee
import org.mifosx.openbanking.core.network.model.ais.statementDetails.StatementInterest

private const val CREDIT = "Credit"

/**
 * Maps the OBIE `OBReadStatement2` payload into a single [StatementDetail].
 *
 * OBIE returns statements as a one-element `Data.Statement` array; the entry whose `StatementId`
 * matches the requested id is preferred, falling back to the first (the single-statement detail call
 * only ever returns one). Returns `null` when the array is empty — the caller (the store fetcher)
 * turns that into a not-found error rather than a blank screen.
 */
internal fun StatementDetailsResponse.toStatementDetail(
    accountId: String,
    statementId: String,
): StatementDetail? {
    val statement = data?.statement.orEmpty().let { list ->
        list.firstOrNull { it.statementId == statementId } ?: list.firstOrNull()
    } ?: return null
    return statement.toStatementDetail(accountId, statementId)
}

private fun Statement.toStatementDetail(
    fallbackAccountId: String,
    fallbackStatementId: String,
): StatementDetail = StatementDetail(
    accountId = accountId ?: fallbackAccountId,
    statementId = statementId ?: fallbackStatementId,
    reference = statementReference.orEmpty(),
    type = type.orEmpty(),
    periodStart = startDateTime.orEmpty(),
    periodEnd = endDateTime.orEmpty(),
    created = creationDateTime.orEmpty(),
    balances = statementAmount.orEmpty().map { it.toBalanceLine() },
    fees = statementFee.orEmpty().map { it.toCharge() },
    interest = statementInterest.orEmpty().map { it.toCharge() },
)

private fun StatementAmount.toBalanceLine(): StatementBalanceLine = StatementBalanceLine(
    type = type.orEmpty(),
    amount = amount?.amount.orEmpty(),
    currency = amount?.currency.orEmpty(),
    isCredit = creditDebitIndicator.equals(CREDIT, ignoreCase = true),
)

private fun StatementFee.toCharge(): StatementCharge = StatementCharge(
    description = description.orEmpty(),
    amount = amount?.amount.orEmpty(),
    currency = amount?.currency.orEmpty(),
    isCredit = creditDebitIndicator.equals(CREDIT, ignoreCase = true),
)

private fun StatementInterest.toCharge(): StatementCharge = StatementCharge(
    description = description.orEmpty(),
    amount = amount?.amount.orEmpty(),
    currency = amount?.currency.orEmpty(),
    isCredit = creditDebitIndicator.equals(CREDIT, ignoreCase = true),
)
