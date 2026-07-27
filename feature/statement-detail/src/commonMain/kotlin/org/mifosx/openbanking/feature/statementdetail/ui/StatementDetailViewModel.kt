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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.mifosx.openbanking.core.common.formatMoney
import org.mifosx.openbanking.core.data.banking.StatementDetailRepository
import org.mifosx.openbanking.core.data.banking.StatementFileRepository
import org.mifosx.openbanking.core.model.banking.StatementBalanceLine
import org.mifosx.openbanking.core.model.banking.StatementCharge
import org.mifosx.openbanking.core.model.banking.StatementDetail
import org.mifosx.openbanking.core.model.banking.TransactionItem
import org.mifosx.openbanking.feature.statements.StatementFileHandler
import template.core.base.common.screen.ScreenState
import template.core.base.common.screen.combineScreenStates
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import template.core.base.ui.viewmodel.BaseViewModel
import kotlin.time.Instant

/** The merged payload of the two streams: the statement paired with its period's transactions. */
private typealias StatementDetailData = Pair<StatementDetail, List<TransactionItem>>

/**
 * Drives the statement-detail screen: a composite of two streams (the statement and its period's
 * transactions) merged into one rendered state, plus an independent PDF download.
 *
 * The two streams come from distinct OBIE endpoints and cache independently; [combineScreenStates]
 * merges them so the screen shows Loading until both settle, an Error if either fails, and Content or
 * Empty (balances still shown, no transactions) once both arrive. The download reuses the existing
 * [StatementFileRepository] and the [StatementFileHandler] delivery seam from `feature/statements`, so
 * the bytes reach the platform save/share sheet without this feature owning a second platform binding.
 * A download failure is a one-shot event; a success is confirmed the same way — the file is handed to
 * the handler here rather than emitted.
 *
 * All display formatting happens here; the composables receive finished strings.
 */
class StatementDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: StatementDetailRepository,
    private val fileRepository: StatementFileRepository,
    private val fileHandler: StatementFileHandler,
) : BaseViewModel<StatementDetailState, StatementDetailEvent, StatementDetailAction>(
    initialState = StatementDetailState(
        accountId = savedStateHandle.get<String>(ACCOUNT_ID_ARG).orEmpty(),
        statementId = savedStateHandle.get<String>(STATEMENT_ID_ARG).orEmpty(),
    ),
) {

    /** The two streams this screen renders, scoped to this view model. */
    private val statementStream =
        repository.statementStream(state.accountId, state.statementId, viewModelScope)
    private val transactionsStream =
        repository.statementTransactionsStream(state.accountId, state.statementId, viewModelScope)

    init {
        combineScreenStates(statementStream.state, transactionsStream.state) { statement, transactions ->
            statement to transactions
        }
            .onEach { screenState -> updateState { copy(uiState = screenState.toUiState()) } }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: StatementDetailAction) {
        when (action) {
            StatementDetailAction.RetryLoad -> {
                statementStream.refresh()
                transactionsStream.refresh()
            }

            StatementDetailAction.DownloadPdf -> download()
        }
    }

    private fun ScreenState<StatementDetailData>.toUiState(): StatementDetailUiState = when (this) {
        is ScreenState.Content -> data.toContentOrEmpty()
        is ScreenState.Error -> StatementDetailUiState.Error(classifyStatementDetailError(error))
        is ScreenState.NoNetwork -> StatementDetailUiState.Error(StatementDetailErrorCode.NetworkError)
        ScreenState.Unauthenticated -> StatementDetailUiState.Error(StatementDetailErrorCode.SessionExpired)
        ScreenState.Empty, ScreenState.Loading -> StatementDetailUiState.Loading
    }

    /** Both streams reached content: an empty transaction list is Empty, anything else is Content. */
    private fun StatementDetailData.toContentOrEmpty(): StatementDetailUiState {
        val (statement, transactions) = this
        val model = statement.toUiModel()
        return if (transactions.isEmpty()) {
            StatementDetailUiState.Empty(model)
        } else {
            StatementDetailUiState.Content(model, transactions.map { it.toRowUiModel() })
        }
    }

    private fun StatementDetail.toUiModel(): StatementDetailUiModel = StatementDetailUiModel(
        reference = reference,
        periodLabel = formatPeriodRange(periodStart, periodEnd),
        type = type,
        createdDateLabel = formatDayLabel(created),
        balances = balances.map { it.toBalanceLine() },
        fees = fees.map { it.toFeeLine() },
        interest = interest.map { it.toInterestLine() },
    )

    private fun StatementBalanceLine.toBalanceLine(): StatementLineUiModel = StatementLineUiModel(
        label = humanizeType(type),
        amountFormatted = signedBalance(amount, currency, isCredit),
        color = if (isCredit) StatementAmountColor.Credit else StatementAmountColor.Debit,
    )

    private fun StatementCharge.toFeeLine(): StatementLineUiModel = StatementLineUiModel(
        label = description,
        amountFormatted = formatMoney(amount, currency),
        color = StatementAmountColor.Neutral,
    )

    private fun StatementCharge.toInterestLine(): StatementLineUiModel = StatementLineUiModel(
        label = description,
        amountFormatted = formatMoney(amount, currency),
        color = if (isCredit) StatementAmountColor.Credit else StatementAmountColor.Debit,
    )

    private fun TransactionItem.toRowUiModel(): StatementTxnRowUiModel = StatementTxnRowUiModel(
        transactionId = transactionId,
        accountId = accountId,
        dateLabel = formatDayLabel(bookingDateTime),
        info = description,
        amountFormatted = signedTransaction(amount, currency, isCredit),
        isCredit = isCredit,
    )

    /**
     * Fetches the statement's PDF, holding [DownloadState.Downloading] for the duration so the button
     * disables and the linear progress bar shows. A success is delivered to the platform via
     * [fileHandler] and confirmed with [StatementDetailEvent.DownloadSucceeded]; a failure is emitted as
     * [StatementDetailEvent.DownloadFailed]. The state returns to Idle either way.
     */
    private fun download() {
        viewModelScope.launch {
            updateState { copy(downloadState = DownloadState.Downloading) }
            when (val result = fileRepository.downloadStatementFile(state.accountId, state.statementId)) {
                is NetworkResult.Success -> {
                    fileHandler.deliver(
                        fileName = "$STATEMENT_FILE_PREFIX${state.statementId}$STATEMENT_FILE_EXTENSION",
                        mimeType = STATEMENT_FILE_MIME,
                        bytes = result.data,
                    )
                    sendEvent(StatementDetailEvent.DownloadSucceeded)
                }

                is NetworkResult.Error ->
                    sendEvent(StatementDetailEvent.DownloadFailed(result.error.toDownloadReason()))
            }
            updateState { copy(downloadState = DownloadState.Idle) }
        }
    }

    private fun NetworkError.toDownloadReason(): String = when (this) {
        is NetworkError.Client.NotFound -> StatementDetailErrorCode.StatementFileNotFound.name
        else -> StatementDetailErrorCode.NetworkError.name
    }

    companion object {
        /** Must match the `StatementDetailRoute.accountId` property name — nav uses it as the key. */
        const val ACCOUNT_ID_ARG: String = "accountId"

        /** Must match the `StatementDetailRoute.statementId` property name — nav's key. */
        const val STATEMENT_ID_ARG: String = "statementId"
    }
}

private const val MONTH_ABBREVIATION_LENGTH = 3
private const val PERIOD_SEPARATOR = " – "
private const val STATEMENT_FILE_PREFIX = "statement-"
private const val STATEMENT_FILE_EXTENSION = ".pdf"
private const val STATEMENT_FILE_MIME = "application/pdf"

/** Parses an ISO-8601 UTC instant to its calendar date, or null when the string is unparseable. */
private fun parseUtcDate(isoDateTime: String): LocalDate? =
    runCatching { Instant.parse(isoDateTime).toLocalDateTime(TimeZone.UTC).date }.getOrNull()

/** `May`, `September` — the month name in title case. */
private fun LocalDate.monthDisplayName(): String =
    month.name.lowercase().replaceFirstChar { it.uppercase() }

/** `1 May 2026` — day, short month, year. */
private fun LocalDate.toDayLabel(): String =
    "$day ${monthDisplayName().take(MONTH_ABBREVIATION_LENGTH)} $year"

/** `1 May 2026`, falling back to the raw string when it cannot be parsed. */
private fun formatDayLabel(isoDateTime: String): String =
    parseUtcDate(isoDateTime)?.toDayLabel() ?: isoDateTime

/** `1 May 2026 – 31 May 2026` — the statement period range. */
private fun formatPeriodRange(start: String, end: String): String =
    "${formatDayLabel(start)}$PERIOD_SEPARATOR${formatDayLabel(end)}"

/** `OpeningBalance` -> `Opening Balance`: inserts a space at each lower-to-upper case boundary. */
private fun humanizeType(type: String): String = buildString {
    type.forEachIndexed { index, char ->
        if (index > 0 && char.isUpperCase() && type[index - 1].isLowerCase()) append(' ')
        append(char)
    }
}

/** `£2,610.40` in credit, `-£12.40` in debit — a balance carries a leading minus only when it is out. */
private fun signedBalance(amount: String, currency: String, isCredit: Boolean): String {
    val sign = if (isCredit) "" else "-"
    return "$sign${formatMoney(amount, currency)}"
}

/** `+£3,200.00` for a credit, `-£82.50` for a debit. */
private fun signedTransaction(amount: String, currency: String, isCredit: Boolean): String {
    val sign = if (isCredit) "+" else "-"
    return "$sign${formatMoney(amount, currency)}"
}
