/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.statementdetail.ui

import org.mifosx.openbanking.core.data.util.RemoteException
import template.core.base.network.NetworkError

/**
 * Screen state for the statement-detail view.
 *
 * @property accountId The route argument the statement, transactions and download are scoped to.
 * @property statementId The route argument identifying the single statement.
 * @property uiState What the screen currently renders.
 * @property downloadState Whether a PDF download is in flight — drives the button-disable and the
 *   inline linear progress bar, independently of [uiState].
 */
data class StatementDetailState(
    val accountId: String,
    val statementId: String,
    val uiState: StatementDetailUiState = StatementDetailUiState.Loading,
    val downloadState: DownloadState = DownloadState.Idle,
)

/**
 * The four rendered states.
 *
 * [Empty] is distinct from [Content]: the statement loaded and its balances still render, but the
 * period held no transactions — a valid OBIE edge case for new or inactive accounts, not a fault.
 */
sealed interface StatementDetailUiState {

    data object Loading : StatementDetailUiState

    data class Content(
        val statement: StatementDetailUiModel,
        val transactions: List<StatementTxnRowUiModel>,
    ) : StatementDetailUiState

    data class Empty(val statement: StatementDetailUiModel) : StatementDetailUiState

    data class Error(val code: StatementDetailErrorCode) : StatementDetailUiState
}

/**
 * The display-ready statement header and money sections. Every field is a finished string the view
 * model computed, so the composables stay testable on the JVM without a Compose runtime.
 *
 * @property periodLabel The period range, e.g. `1 May 2026 – 31 May 2026`.
 * @property createdDateLabel The generation date, e.g. `1 Jun 2026`.
 */
data class StatementDetailUiModel(
    val reference: String,
    val periodLabel: String,
    val type: String,
    val createdDateLabel: String,
    val balances: List<StatementLineUiModel>,
    val fees: List<StatementLineUiModel>,
    val interest: List<StatementLineUiModel>,
)

/**
 * One money line (a balance, fee or interest entry).
 *
 * @property amountFormatted The signed, currency-formatted amount, e.g. `£2,847.63` or `-£12.40`.
 * @property color Which colour the amount renders in.
 */
data class StatementLineUiModel(
    val label: String,
    val amountFormatted: String,
    val color: StatementAmountColor,
)

/**
 * One tappable transaction row within the statement period.
 *
 * @property dateLabel The booking date overline, e.g. `3 May 2026`.
 * @property amountFormatted The signed amount, e.g. `+£3,200.00` or `-£82.50`.
 * @property isCredit Drives the amount colour: tertiary for credit, error for debit.
 */
data class StatementTxnRowUiModel(
    val transactionId: String,
    val accountId: String,
    val dateLabel: String,
    val info: String,
    val amountFormatted: String,
    val isCredit: Boolean,
)

/** The colour role a money amount renders in. */
enum class StatementAmountColor {
    /** Money in — the tertiary colour. */
    Credit,

    /** Money out — the error colour. */
    Debit,

    /** No credit/debit sense (fees) — the neutral on-surface colour. */
    Neutral,
}

/** Whether the statement's PDF download is currently in flight. */
enum class DownloadState {
    Idle,
    Downloading,
}

/**
 * The failure modes the screen distinguishes. [StatementFileNotFound] is a download-only failure (the
 * bank has not yet rendered the PDF) surfaced through the download event, not the load error state.
 */
enum class StatementDetailErrorCode {
    StatementNotFound,
    ConsentMissingReadStatements,
    SessionExpired,
    StatementFileNotFound,
    NetworkError,
}

/** Actions the view model owns. Back and row navigation are the screen's lambdas, not routed here. */
sealed interface StatementDetailAction {

    data object RetryLoad : StatementDetailAction

    data object DownloadPdf : StatementDetailAction
}

/**
 * One-shot events the download flow emits for the screen to surface as a snackbar.
 *
 * A successful download is confirmed with [DownloadSucceeded]; a failure carries a [reason] code name
 * for logging while the screen shows a generic download-error message.
 */
sealed interface StatementDetailEvent {

    data object DownloadSucceeded : StatementDetailEvent

    data class DownloadFailed(val reason: String) : StatementDetailEvent
}

/**
 * Classifies a load-stream failure into one of the [StatementDetailErrorCode]s.
 *
 * Store5 surfaces the repository's `NetworkError` wrapped in a [RemoteException]; anything uncategorised
 * falls through to [StatementDetailErrorCode.NetworkError], treated as transient.
 */
internal fun classifyStatementDetailError(throwable: Throwable): StatementDetailErrorCode =
    when ((throwable as? RemoteException)?.networkError) {
        is NetworkError.Client.Unauthorized -> StatementDetailErrorCode.SessionExpired
        is NetworkError.Client.Forbidden -> StatementDetailErrorCode.ConsentMissingReadStatements
        is NetworkError.Client.NotFound -> StatementDetailErrorCode.StatementNotFound
        is NetworkError.Network -> StatementDetailErrorCode.NetworkError
        else -> StatementDetailErrorCode.NetworkError
    }
