/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.statementdetail

import org.mifosx.openbanking.core.model.banking.StatementBalanceLine
import org.mifosx.openbanking.core.model.banking.StatementCharge
import org.mifosx.openbanking.core.model.banking.StatementDetail
import org.mifosx.openbanking.core.model.banking.TransactionItem
import org.mifosx.openbanking.feature.statementdetail.ui.DownloadState
import org.mifosx.openbanking.feature.statementdetail.ui.StatementAmountColor
import org.mifosx.openbanking.feature.statementdetail.ui.StatementDetailErrorCode
import org.mifosx.openbanking.feature.statementdetail.ui.StatementDetailState
import org.mifosx.openbanking.feature.statementdetail.ui.StatementDetailUiModel
import org.mifosx.openbanking.feature.statementdetail.ui.StatementDetailUiState
import org.mifosx.openbanking.feature.statementdetail.ui.StatementLineUiModel
import org.mifosx.openbanking.feature.statementdetail.ui.StatementTxnRowUiModel
import template.core.base.common.screen.DataFreshness
import template.core.base.common.screen.ScreenState

/**
 * The statement-detail states the suites render, carrying the MAY-2026 HSBC-sandbox demo the design was
 * drawn against (accountId `40051512345678`, statement `MAY-2026-STMT`, six transactions).
 *
 * Shared by the view-model, Compose, Robolectric and screenshot suites so all assert against one screen.
 */
object StatementDetailFixtures {

    const val ACCOUNT_ID: String = "40051512345678"
    const val STATEMENT_ID: String = "STMT-2026-05-40051512345678"
    const val REFERENCE: String = "MAY-2026-STMT"
    const val FIRST_TRANSACTION_ID: String = "TXN-2026-05-001"
    const val PAYROLL_TRANSACTION_ID: String = "TXN-2026-05-002"
    const val EXPECTED_TRANSACTION_COUNT: Int = 6

    /** The statement as the repository stream hands it to the view model — raw OBIE strings. */
    fun statementDetail(): StatementDetail = StatementDetail(
        accountId = ACCOUNT_ID,
        statementId = STATEMENT_ID,
        reference = REFERENCE,
        type = "RegularPeriodic",
        periodStart = "2026-05-01T00:00:00Z",
        periodEnd = "2026-05-31T23:59:59Z",
        created = "2026-06-01T06:00:00Z",
        balances = listOf(
            StatementBalanceLine("OpeningBalance", "2610.40", "GBP", isCredit = true),
            StatementBalanceLine("ClosingBalance", "2847.63", "GBP", isCredit = true),
        ),
        fees = listOf(StatementCharge("Monthly maintenance fee", "0.00", "GBP", isCredit = false)),
        interest = listOf(StatementCharge("In-credit interest", "0.21", "GBP", isCredit = true)),
    )

    /** The empty-period scenario: balances still present, no fees, no interest, no transactions. */
    fun emptyStatementDetail(): StatementDetail = StatementDetail(
        accountId = ACCOUNT_ID,
        statementId = STATEMENT_ID,
        reference = "MAY-2026-NEW",
        type = "RegularPeriodic",
        periodStart = "2026-05-01T00:00:00Z",
        periodEnd = "2026-05-31T23:59:59Z",
        created = "2026-06-01T06:00:00Z",
        balances = listOf(
            StatementBalanceLine("OpeningBalance", "500.00", "GBP", isCredit = true),
            StatementBalanceLine("ClosingBalance", "500.00", "GBP", isCredit = true),
        ),
        fees = emptyList(),
        interest = emptyList(),
    )

    /** The six statement-period transactions as the mapper projects them. */
    fun transactionItems(): List<TransactionItem> = listOf(
        txn("TXN-2026-05-001", "2026-05-03T09:14:22Z", "TESCO STORES 3225 LONDON", "82.50", isCredit = false),
        txn("TXN-2026-05-002", "2026-05-10T00:00:00Z", "BACS CREDIT ACME CORP PAYROLL", "3200.00", isCredit = true),
        txn("TXN-2026-05-003", "2026-05-15T11:30:00Z", "SO LANDLORD RENT MAY 2026", "650.00", isCredit = false),
        txn("TXN-2026-05-004", "2026-05-22T18:45:09Z", "BRITISH GAS ENERGY BILLS", "230.48", isCredit = false),
        txn("TXN-2026-05-005", "2026-05-24T13:22:00Z", "TFL TRAVEL LONDON", "45.00", isCredit = false),
        txn("TXN-2026-05-006", "2026-05-28T07:10:55Z", "NETFLIX.COM", "12.99", isCredit = false),
    )

    /** The header/sections model the view model derives, every string pre-formatted. */
    fun statementUiModel(): StatementDetailUiModel = StatementDetailUiModel(
        reference = REFERENCE,
        periodLabel = "1 May 2026 – 31 May 2026",
        type = "RegularPeriodic",
        createdDateLabel = "1 Jun 2026",
        balances = listOf(
            StatementLineUiModel("Opening Balance", "£2,610.40", StatementAmountColor.Credit),
            StatementLineUiModel("Closing Balance", "£2,847.63", StatementAmountColor.Credit),
        ),
        fees = listOf(StatementLineUiModel("Monthly maintenance fee", "£0.00", StatementAmountColor.Neutral)),
        interest = listOf(StatementLineUiModel("In-credit interest", "£0.21", StatementAmountColor.Credit)),
    )

    /** The empty-period model: balances only, no fees or interest. */
    fun emptyStatementUiModel(): StatementDetailUiModel = StatementDetailUiModel(
        reference = "MAY-2026-NEW",
        periodLabel = "1 May 2026 – 31 May 2026",
        type = "RegularPeriodic",
        createdDateLabel = "1 Jun 2026",
        balances = listOf(
            StatementLineUiModel("Opening Balance", "£500.00", StatementAmountColor.Credit),
            StatementLineUiModel("Closing Balance", "£500.00", StatementAmountColor.Credit),
        ),
        fees = emptyList(),
        interest = emptyList(),
    )

    /** The six transaction rows the view model derives, every string pre-formatted. */
    fun transactionRows(): List<StatementTxnRowUiModel> = listOf(
        row("TXN-2026-05-001", "3 May 2026", "TESCO STORES 3225 LONDON", "-£82.50", isCredit = false),
        row("TXN-2026-05-002", "10 May 2026", "BACS CREDIT ACME CORP PAYROLL", "+£3,200.00", isCredit = true),
        row("TXN-2026-05-003", "15 May 2026", "SO LANDLORD RENT MAY 2026", "-£650.00", isCredit = false),
        row("TXN-2026-05-004", "22 May 2026", "BRITISH GAS ENERGY BILLS", "-£230.48", isCredit = false),
        row("TXN-2026-05-005", "24 May 2026", "TFL TRAVEL LONDON", "-£45.00", isCredit = false),
        row("TXN-2026-05-006", "28 May 2026", "NETFLIX.COM", "-£12.99", isCredit = false),
    )

    fun statementContentStream(): ScreenState<StatementDetail> =
        ScreenState.Content(data = statementDetail(), freshness = DataFreshness.FRESH)

    fun transactionsContentStream(): ScreenState<List<TransactionItem>> =
        ScreenState.Content(data = transactionItems(), freshness = DataFreshness.FRESH)

    fun emptyTransactionsStream(): ScreenState<List<TransactionItem>> =
        ScreenState.Content(data = emptyList(), freshness = DataFreshness.FRESH)

    fun contentState(downloadState: DownloadState = DownloadState.Idle): StatementDetailState =
        StatementDetailState(
            accountId = ACCOUNT_ID,
            statementId = STATEMENT_ID,
            uiState = StatementDetailUiState.Content(statementUiModel(), transactionRows()),
            downloadState = downloadState,
        )

    fun emptyState(): StatementDetailState = StatementDetailState(
        accountId = ACCOUNT_ID,
        statementId = STATEMENT_ID,
        uiState = StatementDetailUiState.Empty(emptyStatementUiModel()),
    )

    fun errorState(
        code: StatementDetailErrorCode = StatementDetailErrorCode.StatementNotFound,
    ): StatementDetailState = StatementDetailState(
        accountId = ACCOUNT_ID,
        statementId = STATEMENT_ID,
        uiState = StatementDetailUiState.Error(code),
    )

    fun loadingState(): StatementDetailState = StatementDetailState(
        accountId = ACCOUNT_ID,
        statementId = STATEMENT_ID,
        uiState = StatementDetailUiState.Loading,
    )

    private fun txn(
        id: String,
        bookingDateTime: String,
        info: String,
        amount: String,
        isCredit: Boolean,
    ): TransactionItem = TransactionItem(
        transactionId = id,
        accountId = ACCOUNT_ID,
        description = info,
        bookingDateTime = bookingDateTime,
        amount = amount,
        currency = "GBP",
        isCredit = isCredit,
    )

    private fun row(
        id: String,
        dateLabel: String,
        info: String,
        amountFormatted: String,
        isCredit: Boolean,
    ): StatementTxnRowUiModel = StatementTxnRowUiModel(
        transactionId = id,
        accountId = ACCOUNT_ID,
        dateLabel = dateLabel,
        info = info,
        amountFormatted = amountFormatted,
        isCredit = isCredit,
    )
}
