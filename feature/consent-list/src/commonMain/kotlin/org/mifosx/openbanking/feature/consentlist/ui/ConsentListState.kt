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

import org.mifosx.openbanking.core.data.util.RemoteException
import org.mifosx.openbanking.core.model.callback.ConsentStatus
import template.core.base.network.NetworkError

/**
 * Screen state for the consent list.
 *
 * @property uiState What the screen currently renders. The screen takes no route argument — the
 *   consents it shows are the ones this device staged, read from the session.
 */
data class ConsentListState(
    val uiState: ConsentListUiState = ConsentListUiState.Loading,
)

/**
 * The five rendered states.
 *
 * [ErrorAuth] is deliberately separate from [Error]: an expired PSU session is not a transient
 * fault, so retrying it would fail again. It offers a sign-in route instead, and the design gives it
 * a tertiary tone rather than error red — as the mockup puts it, this needs action but is not a
 * failure.
 */
sealed interface ConsentListUiState {

    data object Loading : ConsentListUiState

    /**
     * @property active The current connection as a single-element list (empty only transiently);
     *   the list shape is kept because the content renders it through a `LazyColumn`.
     * @property showReconfirmBanner True when the current consent is inside the reconfirmation
     *   window; drives the banner at the top of the screen.
     */
    data class Content(
        val active: List<ConsentCardUi>,
        val showReconfirmBanner: Boolean,
    ) : ConsentListUiState

    data object Empty : ConsentListUiState

    data class Error(val kind: ConsentListErrorKind) : ConsentListUiState

    data object ErrorAuth : ConsentListUiState
}

/**
 * One display-ready consent card.
 *
 * Dates arrive already formatted because that is locale-independent arithmetic the view model can do
 * and the composable cannot test. The surrounding words do *not* — "Expires in %d days" is
 * translatable copy, so it stays a string resource the card resolves. Only [status] stays an enum,
 * so the card picks its own chip label, icon and tone.
 *
 * @property consentId OBIE `ConsentId`; the card key and the argument carried to the detail screen.
 * @property status The bank's current status, driving the chip.
 * @property permissionCount How many data types the PSU shared. Null on a history card, which does
 *   not report a count.
 * @property daysUntilExpiry Whole days remaining, clamped at zero. Meaningful for an active consent.
 * @property expiredOnDate Formatted expiry date, e.g. `26 Jun 2026`. Non-null only for history.
 * @property connectedDate Formatted creation date, e.g. `28 Jun 2026`.
 * @property isNearExpiry True when the consent is inside the reconfirmation window; renders the
 *   urgency chip and tints the expiry line.
 */
data class ConsentCardUi(
    val consentId: String,
    val status: ConsentStatus,
    val permissionCount: Int?,
    val daysUntilExpiry: Int,
    val expiredOnDate: String?,
    val connectedDate: String,
    val isNearExpiry: Boolean,
)

/**
 * The failure modes the generic error state distinguishes.
 *
 * A 401 is not here — it routes to [ConsentListUiState.ErrorAuth] instead, which is the whole point
 * of that state existing.
 */
enum class ConsentListErrorKind {
    NetworkError,
    ServerError,
}

/** Actions the view model owns. Navigation is the screen's lambdas, not routed here. */
sealed interface ConsentListAction {
    data object RetryLoad : ConsentListAction
}

/**
 * Classifies a stream failure.
 *
 * Returns `null` for an unauthorised failure: that is not an error kind but a different screen
 * state, and forcing it into this enum would let it render with a Retry button that cannot work.
 */
internal fun classifyConsentListError(throwable: Throwable): ConsentListErrorKind? =
    when ((throwable as? RemoteException)?.networkError) {
        is NetworkError.Client.Unauthorized -> null
        is NetworkError.Server -> ConsentListErrorKind.ServerError
        else -> ConsentListErrorKind.NetworkError
    }
