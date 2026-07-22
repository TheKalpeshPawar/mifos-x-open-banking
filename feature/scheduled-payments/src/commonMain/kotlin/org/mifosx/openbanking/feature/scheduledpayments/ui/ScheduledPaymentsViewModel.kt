/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.scheduledpayments.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.mifosx.openbanking.core.data.banking.ScheduledPaymentsRepository
import org.mifosx.openbanking.core.model.banking.ScheduledPaymentItem
import template.core.base.common.screen.ScreenState
import template.core.base.common.screen.emptyIfContent
import template.core.base.ui.viewmodel.BaseViewModel
import kotlin.time.Instant

/**
 * Drives the scheduled-payments list: one card per future-dated instruction for the account.
 *
 * Navigation is not modelled as an action — the screen owns back through its `onBack` lambda,
 * matching direct-debits — so this stays a pure state machine over the payment stream, with no
 * one-shot events (`Event = Nothing`).
 *
 * All display formatting happens here: the amount becomes `GBP 842.00` and the ISO date becomes
 * `Fri 31 Jul 2026`, so the composables receive finished strings and stay testable on the JVM.
 */
class ScheduledPaymentsViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: ScheduledPaymentsRepository,
) : BaseViewModel<ScheduledPaymentsState, Nothing, ScheduledPaymentsAction>(
    initialState = ScheduledPaymentsState(
        accountId = savedStateHandle.get<String>(ACCOUNT_ID_ARG).orEmpty(),
    ),
) {

    /** The stream this screen renders, scoped to this view model. */
    private val stream = repository.scheduledPaymentsStream(state.accountId, viewModelScope)

    init {
        stream.state
            .emptyIfContent { payments -> payments.isEmpty() }
            .onEach { screenState -> updateState { copy(uiState = screenState.toUiState()) } }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: ScheduledPaymentsAction) {
        when (action) {
            ScheduledPaymentsAction.RetryLoad -> stream.refresh()
        }
    }

    private fun ScreenState<List<ScheduledPaymentItem>>.toUiState(): ScheduledPaymentsUiState =
        when (this) {
            is ScreenState.Content -> ScheduledPaymentsUiState.Content(data.map { it.toUiModel() })
            is ScreenState.Error -> ScheduledPaymentsUiState.Error(classifyScheduledPaymentsError(error))
            is ScreenState.NoNetwork -> ScheduledPaymentsUiState.Error(ScheduledPaymentsError.NetworkError)
            ScreenState.Unauthenticated -> ScheduledPaymentsUiState.Error(ScheduledPaymentsError.TokenExpired)
            ScreenState.Empty -> ScheduledPaymentsUiState.Empty
            ScreenState.Loading -> ScheduledPaymentsUiState.Loading
        }

    private fun ScheduledPaymentItem.toUiModel(): ScheduledPaymentUiModel = ScheduledPaymentUiModel(
        scheduledPaymentId = scheduledPaymentId,
        payeeName = payeeName,
        amountLabel = "$currency $amount",
        scheduledDateLabel = formatScheduledDate(scheduledDateTime),
        scheduledType = scheduledType,
        creditorIdentification = creditorIdentification,
        reference = reference,
    )

    companion object {
        /** Must match the [ScheduledPaymentsRoute] property name — type-safe nav uses it as the key. */
        const val ACCOUNT_ID_ARG: String = "accountId"
    }
}

private const val ABBREVIATION_LENGTH = 3

/**
 * Formats an ISO-8601 UTC instant as `Fri 31 Jul 2026`, falling back to the raw string when the
 * value the bank sent cannot be parsed — an odd-format date on screen tells the user more than a
 * blank line does.
 */
internal fun formatScheduledDate(isoDateTime: String): String {
    val dateTime = runCatching {
        Instant.parse(isoDateTime).toLocalDateTime(TimeZone.UTC)
    }.getOrNull() ?: return isoDateTime
    return dateTime.toDisplayLabel()
}

private fun LocalDateTime.toDisplayLabel(): String {
    val weekday = dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }.take(ABBREVIATION_LENGTH)
    val monthLabel = month.name.lowercase().replaceFirstChar { it.uppercase() }.take(ABBREVIATION_LENGTH)
    return "$weekday $day $monthLabel $year"
}
