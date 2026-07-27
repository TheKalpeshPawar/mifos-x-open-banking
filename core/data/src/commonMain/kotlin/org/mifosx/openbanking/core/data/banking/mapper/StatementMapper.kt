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

import org.mifosx.openbanking.core.model.banking.StatementPeriod
import org.mifosx.openbanking.core.network.model.ais.statements.Statement
import org.mifosx.openbanking.core.network.model.ais.statements.StatementsResponse

private const val CLOSING_BALANCE_TYPE = "ClosingBalance"

/**
 * Maps the OBIE `OBReadStatement2` payload into the statement-period list, sorted newest-first.
 *
 * The sort is by `StartDateTime` descending, so the most recent period leads the list — the order a
 * user scans a statement history in. ISO-8601 timestamps sort correctly as plain strings, so no parse
 * is needed to order them.
 *
 * Statements without a `StatementId` are dropped: the id is what the file download and detail calls
 * are keyed by, so a row that cannot be opened is worse than an absent one. Every other field
 * degrades to an empty string instead, so a period that simply declared no closing balance still
 * renders.
 */
internal fun StatementsResponse.toStatementPeriods(): List<StatementPeriod> =
    data?.statement.orEmpty()
        .mapNotNull { it.toStatementPeriodOrNull() }
        .sortedByDescending { it.startDateTime }

private fun Statement.toStatementPeriodOrNull(): StatementPeriod? {
    val id = statementId?.takeIf { it.isNotBlank() } ?: return null
    val closingBalance = statementAmount?.firstOrNull { it.type == CLOSING_BALANCE_TYPE }
    return StatementPeriod(
        statementId = id,
        statementReference = statementReference.orEmpty(),
        startDateTime = startDateTime.orEmpty(),
        endDateTime = endDateTime.orEmpty(),
        closingBalanceAmount = closingBalance?.amount?.amount.orEmpty(),
        closingBalanceCurrency = closingBalance?.amount?.currency.orEmpty(),
    )
}
