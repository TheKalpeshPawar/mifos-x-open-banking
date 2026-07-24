/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.consentdetail.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.mifosx.openbanking.core.data.banking.ConsentDetailRepository
import org.mifosx.openbanking.core.data.user.AppLogout
import org.mifosx.openbanking.core.model.banking.ConsentSummary
import template.core.base.common.screen.ScreenState
import template.core.base.ui.viewmodel.BaseViewModel
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Drives consent detail: what one consent covers, and the two-step gate for withdrawing it.
 *
 * Revocation is deliberately three states rather than a boolean. Tapping Revoke only opens
 * [ConsentDetailUiState.RevokeConfirm] and issues nothing; confirming moves to
 * [ConsentDetailUiState.Revoking], which locks the screen while the work is in flight so a second
 * tap cannot start it twice.
 *
 * Confirming a revoke is a full logout: it withdraws the consent and clears all local data through
 * the shared [AppLogout], the same path as profile sign-out. The screen is not navigated away from
 * here — clearing the data drives the root navigator to onboarding on its own, so there is no
 * one-shot event to raise (`Event = Nothing`).
 */
@OptIn(ExperimentalTime::class)
class ConsentDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: ConsentDetailRepository,
    private val appLogout: AppLogout,
) : BaseViewModel<ConsentDetailState, Nothing, ConsentDetailAction>(
    initialState = ConsentDetailState(
        consentId = savedStateHandle.get<String>(CONSENT_ID_ARG).orEmpty(),
    ),
) {

    /** The stream this screen renders, scoped to this view model. */
    private val stream = repository.consentStream(state.consentId, viewModelScope)

    init {
        stream.state
            .onEach { screenState -> updateState { copy(uiState = screenState.toUiState()) } }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: ConsentDetailAction) {
        when (action) {
            ConsentDetailAction.RetryLoad -> stream.refresh()
            ConsentDetailAction.ConfirmRevoke -> openRevokeGate()
            ConsentDetailAction.DismissRevokeConfirm -> closeRevokeGate()
            ConsentDetailAction.ExecuteRevoke -> executeRevoke()
        }
    }

    /** Opens the gate. No API call — the whole point of the confirm step. */
    private fun openRevokeGate() {
        val current = state.uiState as? ConsentDetailUiState.Content ?: return
        updateState { copy(uiState = ConsentDetailUiState.RevokeConfirm(current.consent)) }
    }

    private fun closeRevokeGate() {
        val current = state.uiState as? ConsentDetailUiState.RevokeConfirm ?: return
        updateState { copy(uiState = ConsentDetailUiState.Content(current.consent)) }
    }

    /**
     * Locks the screen and runs the full logout: withdraw the consent, then clear all local data.
     *
     * Only reachable from the confirmation gate, so a stray dispatch from any other state is ignored
     * rather than silently logging the user out. The revoke is best-effort inside [AppLogout] — an
     * already-expired or unreachable consent still tears down the session — and the root navigator
     * takes the user to onboarding once the data is cleared, so nothing further is done here.
     */
    private fun executeRevoke() {
        val current = state.uiState as? ConsentDetailUiState.RevokeConfirm ?: return
        updateState { copy(uiState = ConsentDetailUiState.Revoking(current.consent)) }

        viewModelScope.launch { appLogout.logOut() }
    }

    private fun ScreenState<ConsentSummary>.toUiState(): ConsentDetailUiState = when (this) {
        is ScreenState.Content -> ConsentDetailUiState.Content(data.toUi(Clock.System.now()))
        is ScreenState.Error -> ConsentDetailUiState.Error(classifyConsentDetailError(error))
        is ScreenState.NoNetwork -> ConsentDetailUiState.Error(ConsentDetailErrorKind.NetworkError)
        ScreenState.Unauthenticated -> ConsentDetailUiState.Error(ConsentDetailErrorKind.TokenExpired)
        ScreenState.Empty -> ConsentDetailUiState.Empty
        ScreenState.Loading -> ConsentDetailUiState.Loading
    }

    private fun ConsentSummary.toUi(now: Instant): ConsentDetailUi {
        val days = daysUntilExpiry(expirationDateTime, now)
        return ConsentDetailUi(
            consentId = consentId,
            status = status,
            permissions = permissions,
            connectedDate = formatDetailDate(creationDateTime),
            expiresDate = formatDetailDate(expirationDateTime),
            transactionFromDate = formatDetailDate(transactionFromDateTime),
            transactionToDate = formatDetailDate(transactionToDateTime),
            expiryWarningDays = days.takeIf { it <= EXPIRY_WARNING_DAYS },
        )
    }

    companion object {
        /** Must match the [ConsentDetailRoute] property name — type-safe nav uses it as the key. */
        const val CONSENT_ID_ARG: String = "consentId"

        /**
         * The warning window, in days.
         *
         * Seven here and fourteen on the list, deliberately: the list sweeps every connection and
         * warns earlier, this screen is where the user already is and warns only when imminent.
         */
        const val EXPIRY_WARNING_DAYS = 7
    }
}

private const val MONTH_ABBREVIATION_LENGTH = 3

/**
 * Whole days from [now] until [expirationDateTime], never negative.
 *
 * An unparseable or past date yields zero, which puts the screen inside the warning window —
 * treating an unreadable expiry as urgent is the safe direction.
 */
@OptIn(ExperimentalTime::class)
internal fun daysUntilExpiry(expirationDateTime: String, now: Instant): Int {
    val expiry = runCatching { Instant.parse(expirationDateTime) }.getOrNull() ?: return 0
    return (expiry - now).inWholeDays.coerceAtLeast(0).toInt()
}

/**
 * Formats an ISO-8601 instant as `28 Jun 2026`, falling back to the raw string when the value the
 * bank sent cannot be parsed.
 */
@OptIn(ExperimentalTime::class)
internal fun formatDetailDate(isoDateTime: String): String {
    val dateTime = runCatching {
        Instant.parse(isoDateTime).toLocalDateTime(TimeZone.UTC)
    }.getOrNull() ?: return isoDateTime
    val month = dateTime.month.name.lowercase()
        .replaceFirstChar { it.uppercase() }
        .take(MONTH_ABBREVIATION_LENGTH)
    return "${dateTime.day} $month ${dateTime.year}"
}
