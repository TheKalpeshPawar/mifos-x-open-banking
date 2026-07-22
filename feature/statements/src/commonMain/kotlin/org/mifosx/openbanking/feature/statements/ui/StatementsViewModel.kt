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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.mifosx.openbanking.core.common.formatMoney
import org.mifosx.openbanking.core.data.banking.StatementFileRepository
import org.mifosx.openbanking.core.data.banking.StatementsRepository
import org.mifosx.openbanking.core.model.banking.StatementPeriod
import org.mifosx.openbanking.feature.statements.StatementFileHandler
import template.core.base.common.screen.ScreenState
import template.core.base.common.screen.emptyIfContent
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import template.core.base.ui.viewmodel.BaseViewModel
import kotlin.time.Instant

/**
 * Drives the statements list and per-statement file downloads.
 *
 * The list is a pure state machine over the statement stream — navigation (back and row) is the
 * screen's concern. Downloads are the reason this is the first feature with a non-`Nothing` event
 * type: a download can fail in two ways the list cannot show inline, so those are emitted as one-shot
 * events for the screen to surface as a snackbar. A successful download is handed to the injected
 * [StatementFileHandler] here rather than emitted, keeping the delivery testable against a fake.
 *
 * All display formatting happens here; the composables receive finished strings, which keeps currency
 * and date rendering testable on the JVM without a Compose runtime.
 */
class StatementsViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: StatementsRepository,
    private val fileRepository: StatementFileRepository,
    private val fileHandler: StatementFileHandler,
) : BaseViewModel<StatementsState, StatementsEvent, StatementsAction>(
    initialState = StatementsState(
        accountId = savedStateHandle.get<String>(ACCOUNT_ID_ARG).orEmpty(),
    ),
) {

    /** The stream this screen renders, scoped to this view model. */
    private val stream = repository.statementsStream(state.accountId, viewModelScope)

    init {
        stream.state
            .emptyIfContent { statements -> statements.isEmpty() }
            .onEach { screenState -> updateState { copy(uiState = screenState.toUiState()) } }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: StatementsAction) {
        when (action) {
            StatementsAction.RetryLoad -> stream.refresh()
            is StatementsAction.DownloadStatement -> download(action.statementId)
        }
    }

    private fun ScreenState<List<StatementPeriod>>.toUiState(): StatementsUiState = when (this) {
        is ScreenState.Content -> StatementsUiState.Content(data.map { it.toRowUiModel() })
        is ScreenState.Error -> StatementsUiState.Error(classifyStatementsError(error))
        is ScreenState.NoNetwork -> StatementsUiState.Error(StatementsErrorKind.NetworkError)
        ScreenState.Unauthenticated -> StatementsUiState.Error(StatementsErrorKind.TokenExpired)
        ScreenState.Empty -> StatementsUiState.Empty
        ScreenState.Loading -> StatementsUiState.Loading
    }

    private fun StatementPeriod.toRowUiModel(): StatementRowUiModel {
        val start = parseUtcDate(startDateTime)
        val end = parseUtcDate(endDateTime)
        return StatementRowUiModel(
            statementId = statementId,
            statementReference = statementReference,
            periodLabel = start?.toPeriodLabel() ?: startDateTime,
            closingBalanceFormatted = formatMoney(closingBalanceAmount, closingBalanceCurrency),
            startDateFormatted = start?.toDayLabel() ?: startDateTime,
            endDateFormatted = end?.toDayLabel() ?: endDateTime,
        )
    }

    /**
     * Fetches one statement's file, flipping its row into [DownloadState.InProgress] for the duration
     * so the row swaps its download icon for a spinner. A successful fetch is delivered to the platform
     * via [fileHandler]; a failure is emitted as a snackbar event. The row returns to Idle either way.
     */
    private fun download(statementId: String) {
        viewModelScope.launch {
            updateState {
                copy(downloadState = downloadState + (statementId to DownloadState.InProgress))
            }
            when (val result = fileRepository.downloadStatementFile(state.accountId, statementId)) {
                is NetworkResult.Success -> fileHandler.deliver(
                    fileName = "$STATEMENT_FILE_PREFIX$statementId$STATEMENT_FILE_EXTENSION",
                    mimeType = STATEMENT_FILE_MIME,
                    bytes = result.data,
                )

                is NetworkResult.Error -> emitDownloadFailure(result.error)
            }
            updateState {
                copy(downloadState = downloadState + (statementId to DownloadState.Idle))
            }
        }
    }

    /**
     * A 501 is the bank saying this statement has no downloadable file — a definitive answer, shown as
     * "not available". Every other failure is treated as transient and offered as "try again".
     */
    private fun emitDownloadFailure(error: NetworkError) {
        val unsupported = error is NetworkError.Server && error.statusCode == DOWNLOAD_UNSUPPORTED_STATUS
        sendEvent(
            if (unsupported) StatementsEvent.ShowDownloadUnavailable else StatementsEvent.ShowDownloadError,
        )
    }

    companion object {
        /** Must match the [StatementsRoute] property name — type-safe nav uses it as the key. */
        const val ACCOUNT_ID_ARG: String = "accountId"
    }
}

private const val DOWNLOAD_UNSUPPORTED_STATUS = 501
private const val STATEMENT_FILE_PREFIX = "statement-"
private const val STATEMENT_FILE_EXTENSION = ".pdf"
private const val STATEMENT_FILE_MIME = "application/pdf"
private const val MONTH_ABBREVIATION_LENGTH = 3

/** Parses an ISO-8601 UTC instant to its calendar date, or null when the string is unparseable. */
private fun parseUtcDate(isoDateTime: String): LocalDate? =
    runCatching { Instant.parse(isoDateTime).toLocalDateTime(TimeZone.UTC).date }.getOrNull()

/** `May`, `September` — the month name in title case. */
private fun LocalDate.monthDisplayName(): String =
    month.name.lowercase().replaceFirstChar { it.uppercase() }

/** `May 2026` — the period's month and year. */
private fun LocalDate.toPeriodLabel(): String = "${monthDisplayName()} $year"

/** `1 May 2026` — day, short month, year. */
private fun LocalDate.toDayLabel(): String =
    "$day ${monthDisplayName().take(MONTH_ABBREVIATION_LENGTH)} $year"
