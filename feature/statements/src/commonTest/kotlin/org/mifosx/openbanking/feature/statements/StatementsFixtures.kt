/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.statements

import org.mifosx.openbanking.core.data.util.RemoteException
import org.mifosx.openbanking.core.model.banking.StatementPeriod
import org.mifosx.openbanking.feature.statements.ui.DownloadState
import org.mifosx.openbanking.feature.statements.ui.StatementRowUiModel
import org.mifosx.openbanking.feature.statements.ui.StatementsErrorKind
import org.mifosx.openbanking.feature.statements.ui.StatementsState
import org.mifosx.openbanking.feature.statements.ui.StatementsUiState
import template.core.base.common.screen.DataFreshness
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkError

/**
 * The statements states the suites render, carrying the six-period HSBC-sandbox demo set the design
 * was drawn against (accountId `40051512345678`, Dec 2025 – May 2026).
 *
 * Shared by the view-model, Compose, Robolectric and screenshot suites so all assert against one
 * screen rather than several that happen to look alike.
 */
object StatementsFixtures {

    const val ACCOUNT_ID: String = "40051512345678"

    /** The OBIE `StatementId` of the most recent (May 2026) period — the row the download tests tap. */
    const val MAY_STATEMENT_ID: String = "STMT-2026-05-40051512345678"

    const val MAY_STATEMENT_REFERENCE: String = "MAY-2026-STMT"

    /** How many statement periods the demo account has. */
    const val EXPECTED_ROW_COUNT: Int = 6

    /**
     * The six periods as the repository stream hands them to the view model: already sorted
     * newest-first by the mapper. Every field is the raw OBIE string; the view model does the
     * formatting.
     */
    fun statementPeriods(): List<StatementPeriod> = listOf(
        period("2026-05", MAY_STATEMENT_REFERENCE, "2026-05-01T00:00:00Z", "2026-05-31T23:59:59Z", "2847.63"),
        period("2026-04", "APR-2026-STMT", "2026-04-01T00:00:00Z", "2026-04-30T23:59:59Z", "2610.40"),
        period("2026-03", "MAR-2026-STMT", "2026-03-01T00:00:00Z", "2026-03-31T23:59:59Z", "2314.92"),
        period("2026-02", "FEB-2026-STMT", "2026-02-01T00:00:00Z", "2026-02-28T23:59:59Z", "1988.57"),
        period("2026-01", "JAN-2026-STMT", "2026-01-01T00:00:00Z", "2026-01-31T23:59:59Z", "1754.10"),
        period("2025-12", "DEC-2025-STMT", "2025-12-01T00:00:00Z", "2025-12-31T23:59:59Z", "1502.88"),
    )

    /** The rows the view model derives from [statementPeriods], with every string pre-formatted. */
    fun rowUiModels(): List<StatementRowUiModel> = listOf(
        row("2026-05", MAY_STATEMENT_REFERENCE, "May 2026", "£2,847.63", "1 May 2026", "31 May 2026"),
        row("2026-04", "APR-2026-STMT", "April 2026", "£2,610.40", "1 Apr 2026", "30 Apr 2026"),
        row("2026-03", "MAR-2026-STMT", "March 2026", "£2,314.92", "1 Mar 2026", "31 Mar 2026"),
        row("2026-02", "FEB-2026-STMT", "February 2026", "£1,988.57", "1 Feb 2026", "28 Feb 2026"),
        row("2026-01", "JAN-2026-STMT", "January 2026", "£1,754.10", "1 Jan 2026", "31 Jan 2026"),
        row("2025-12", "DEC-2025-STMT", "December 2025", "£1,502.88", "1 Dec 2025", "31 Dec 2025"),
    )

    /** A `Content` stream state carrying the six sorted periods. */
    fun contentStreamState(): ScreenState<List<StatementPeriod>> =
        ScreenState.Content(data = statementPeriods(), freshness = DataFreshness.FRESH)

    /** A `Content` stream state carrying no periods — the view model turns this into `Empty`. */
    fun emptyStreamState(): ScreenState<List<StatementPeriod>> =
        ScreenState.Content(data = emptyList(), freshness = DataFreshness.FRESH)

    /** A stream `Error` carrying the given [NetworkError] wrapped as the store surfaces it. */
    fun errorStreamState(error: NetworkError): ScreenState<List<StatementPeriod>> =
        ScreenState.Error(RemoteException(error))

    /** A rendered `Content` screen state for the Compose suites. */
    fun contentState(
        downloadState: Map<String, DownloadState> = emptyMap(),
    ): StatementsState = StatementsState(
        accountId = ACCOUNT_ID,
        uiState = StatementsUiState.Content(rowUiModels()),
        downloadState = downloadState,
    )

    fun emptyState(): StatementsState =
        StatementsState(accountId = ACCOUNT_ID, uiState = StatementsUiState.Empty)

    fun errorState(
        kind: StatementsErrorKind = StatementsErrorKind.ServerError,
    ): StatementsState = StatementsState(accountId = ACCOUNT_ID, uiState = StatementsUiState.Error(kind))

    fun loadingState(): StatementsState =
        StatementsState(accountId = ACCOUNT_ID, uiState = StatementsUiState.Loading)

    private fun period(
        month: String,
        reference: String,
        start: String,
        end: String,
        closingBalance: String,
    ): StatementPeriod = StatementPeriod(
        statementId = "STMT-$month-$ACCOUNT_ID",
        statementReference = reference,
        startDateTime = start,
        endDateTime = end,
        closingBalanceAmount = closingBalance,
        closingBalanceCurrency = "GBP",
    )

    @Suppress("LongParameterList")
    private fun row(
        month: String,
        reference: String,
        periodLabel: String,
        closingBalanceFormatted: String,
        startDateFormatted: String,
        endDateFormatted: String,
    ): StatementRowUiModel = StatementRowUiModel(
        statementId = "STMT-$month-$ACCOUNT_ID",
        statementReference = reference,
        periodLabel = periodLabel,
        closingBalanceFormatted = closingBalanceFormatted,
        startDateFormatted = startDateFormatted,
        endDateFormatted = endDateFormatted,
    )
}
