/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.statements.ui

import org.mifosx.openbanking.core.data.util.RemoteException
import template.core.base.network.NetworkError

/**
 * Screen state for the statements list.
 *
 * @property accountId The route argument; the statement list and every download is scoped to it.
 * @property uiState What the screen currently renders.
 * @property downloadState Per-statement download progress, keyed by `statementId`. A statement absent
 *   from the map is [DownloadState.Idle]; only an in-flight download carries an entry, so the map
 *   stays empty in the common case rather than holding one Idle entry per row.
 */
data class StatementsState(
    val accountId: String,
    val uiState: StatementsUiState = StatementsUiState.Loading,
    val downloadState: Map<String, DownloadState> = emptyMap(),
)

/**
 * The four rendered states. The statement list is unpaginated — OBIE returns the full set in one
 * response — so there is no load-more state to model here, unlike transactions.
 */
sealed interface StatementsUiState {

    data object Loading : StatementsUiState

    data class Content(val statements: List<StatementRowUiModel>) : StatementsUiState

    data object Empty : StatementsUiState

    data class Error(val kind: StatementsErrorKind) : StatementsUiState
}

/**
 * One display-ready statement row. Every field is a finished string; the view model does the
 * formatting so currency and date rendering stay testable on the JVM without a Compose runtime.
 *
 * @property periodLabel The month the period covers, e.g. `May 2026`.
 * @property closingBalanceFormatted The closing balance as a currency figure, e.g. `£2,847.63`.
 * @property startDateFormatted The period start as `1 May 2026`.
 * @property endDateFormatted The period end as `31 May 2026`.
 */
data class StatementRowUiModel(
    val statementId: String,
    val statementReference: String,
    val periodLabel: String,
    val closingBalanceFormatted: String,
    val startDateFormatted: String,
    val endDateFormatted: String,
)

/** Whether a statement's file download is currently in flight. */
enum class DownloadState {
    Idle,
    InProgress,
}

/**
 * The five failure modes the screen distinguishes, each mapped to its own message.
 *
 * Unlike direct-debits, every kind is retriable — a statements consent-scope refusal (403) is shown
 * with Retry too, because re-authorising in Consents then retrying is a path back, and withholding
 * the button would strand the user on the error screen.
 */
enum class StatementsErrorKind(val isRetriable: Boolean) {
    TokenExpired(isRetriable = true),
    ConsentScope(isRetriable = true),
    RateLimited(isRetriable = true),
    ServerError(isRetriable = true),
    NetworkError(isRetriable = true),
}

/** Actions the view model owns. Back and row navigation are the screen's lambdas, not routed here. */
sealed interface StatementsAction {

    data object RetryLoad : StatementsAction

    /** Requests the rendered file for one statement, keyed by its OBIE `StatementId`. */
    data class DownloadStatement(val statementId: String) : StatementsAction
}

/**
 * One-shot events the download flow emits for the screen to surface as a snackbar.
 *
 * The delivered bytes are handed to the injected `StatementFileHandler` inside the view model rather
 * than emitted here, so the only events left are the two failure toasts — a success needs no
 * snackbar, the platform's own save/share sheet is the feedback.
 */
sealed interface StatementsEvent {

    /** The bank does not offer this statement as a downloadable file (HTTP 501). */
    data object ShowDownloadUnavailable : StatementsEvent

    /** The download failed for a transient reason and can be retried. */
    data object ShowDownloadError : StatementsEvent
}

/**
 * Classifies a stream failure into one of the five [StatementsErrorKind]s.
 *
 * Store5 surfaces the repository's `NetworkError` wrapped in a [RemoteException]. Anything the
 * transport could not categorise falls through to [StatementsErrorKind.ServerError], which is
 * retriable: an uncategorised fault is more often transient than permanent.
 */
internal fun classifyStatementsError(throwable: Throwable): StatementsErrorKind =
    when ((throwable as? RemoteException)?.networkError) {
        is NetworkError.Client.Unauthorized -> StatementsErrorKind.TokenExpired
        is NetworkError.Client.Forbidden -> StatementsErrorKind.ConsentScope
        is NetworkError.Client.RateLimited -> StatementsErrorKind.RateLimited
        is NetworkError.Network -> StatementsErrorKind.NetworkError
        else -> StatementsErrorKind.ServerError
    }
