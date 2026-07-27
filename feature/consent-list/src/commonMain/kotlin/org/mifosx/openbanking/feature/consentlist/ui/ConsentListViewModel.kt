/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.consentlist.ui

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.mifosx.openbanking.core.data.banking.ConsentDetailRepository
import org.mifosx.openbanking.core.data.callback.ConsentSession
import org.mifosx.openbanking.core.model.banking.ConsentSummary
import template.core.base.common.screen.ScreenState
import template.core.base.ui.viewmodel.BaseViewModel
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Drives the consent list: the PSU's current bank connection.
 *
 * The id comes from [ConsentSession] — OBIE has no "list my consents" call — and is the single
 * consent this device is currently connected under. Status is always re-fetched, so a consent
 * revoked from the bank's own app appears as revoked here rather than lingering as authorised. No
 * device-side history is kept; withdrawing a consent signs the PSU out.
 *
 * Navigation is not modelled as an action — the screen owns the card tap, the connect route and the
 * re-auth route through its lambdas — so this stays a pure state machine with no one-shot events
 * (`Event = Nothing`). All formatting happens here; the card receives finished strings.
 */
@OptIn(ExperimentalTime::class)
class ConsentListViewModel(
    private val repository: ConsentDetailRepository,
    private val session: ConsentSession,
) : BaseViewModel<ConsentListState, Nothing, ConsentListAction>(
    initialState = ConsentListState(),
) {

    /** The stream this screen renders, scoped to this view model; null when nothing is connected. */
    private val stream = session.consentId()?.let { repository.consentStream(it, viewModelScope) }

    init {
        val activeStream = stream
        if (activeStream == null) {
            // Nothing is connected on this device, so there is no consent to ask the bank about.
            updateState { copy(uiState = ConsentListUiState.Empty) }
        } else {
            activeStream.state
                .onEach { screenState -> updateState { copy(uiState = screenState.toUiState()) } }
                .launchIn(viewModelScope)
        }
    }

    override fun handleAction(action: ConsentListAction) {
        when (action) {
            ConsentListAction.RetryLoad -> stream?.refresh()
        }
    }

    private fun ScreenState<ConsentSummary>.toUiState(): ConsentListUiState = when (this) {
        is ScreenState.Content -> data.toContent()
        is ScreenState.Error -> classifyConsentListError(error)
            ?.let { ConsentListUiState.Error(it) }
            ?: ConsentListUiState.ErrorAuth

        is ScreenState.NoNetwork -> ConsentListUiState.Error(ConsentListErrorKind.NetworkError)
        ScreenState.Unauthenticated -> ConsentListUiState.ErrorAuth
        ScreenState.Empty -> ConsentListUiState.Empty
        ScreenState.Loading -> ConsentListUiState.Loading
    }

    private fun ConsentSummary.toContent(): ConsentListUiState.Content {
        val card = toCard(Clock.System.now())
        return ConsentListUiState.Content(
            active = listOf(card),
            showReconfirmBanner = card.isNearExpiry,
        )
    }

    private fun ConsentSummary.toCard(now: Instant): ConsentCardUi {
        val days = daysUntilExpiry(expirationDateTime, now)
        return ConsentCardUi(
            consentId = consentId,
            status = status,
            permissionCount = permissions.size,
            daysUntilExpiry = days,
            expiredOnDate = null,
            connectedDate = formatConsentDate(creationDateTime),
            isNearExpiry = days <= RECONFIRM_WINDOW_DAYS,
        )
    }

    private companion object {
        /**
         * The reconfirmation window, in days.
         *
         * Fourteen here and seven on the detail screen, deliberately: the list is the sweep of the
         * connection and warns earlier, the detail screen is where the user already is and warns
         * only when it is imminent. Do not unify them.
         */
        const val RECONFIRM_WINDOW_DAYS = 14
    }
}

private const val MONTH_ABBREVIATION_LENGTH = 3

/**
 * Whole days from [now] until [expirationDateTime], never negative.
 *
 * A past expiry clamps to zero rather than going negative, so a consent that lapsed between the
 * fetch and the render reads as "expires in 0 days" rather than a nonsense countdown. An unparseable
 * date also yields zero — treating an unreadable expiry as urgent is the safe direction.
 */
@OptIn(ExperimentalTime::class)
internal fun daysUntilExpiry(expirationDateTime: String, now: Instant): Int {
    val expiry = runCatching { Instant.parse(expirationDateTime) }.getOrNull() ?: return 0
    return (expiry - now).inWholeDays.coerceAtLeast(0).toInt()
}

/**
 * Formats an ISO-8601 instant as `28 Jun 2026`, falling back to the raw string when the value the
 * bank sent cannot be parsed — an odd-format date on screen tells the user more than a blank does.
 */
@OptIn(ExperimentalTime::class)
internal fun formatConsentDate(isoDateTime: String): String {
    val dateTime = runCatching {
        Instant.parse(isoDateTime).toLocalDateTime(TimeZone.UTC)
    }.getOrNull() ?: return isoDateTime
    val month = dateTime.month.name.lowercase()
        .replaceFirstChar { it.uppercase() }
        .take(MONTH_ABBREVIATION_LENGTH)
    return "${dateTime.day} $month ${dateTime.year}"
}
