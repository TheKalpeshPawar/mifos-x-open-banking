/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.standingorders.ui

import org.mifosx.openbanking.core.data.util.RemoteException
import template.core.base.network.NetworkError

/**
 * Screen state for the standing-orders list.
 *
 * @property accountId The route argument; the order list is scoped to it.
 * @property uiState What the screen currently renders.
 */
data class StandingOrdersState(
    val accountId: String,
    val uiState: StandingOrdersUiState = StandingOrdersUiState.Loading,
)

/**
 * The four rendered states.
 *
 * [Content] carries the two summary counts alongside the rows because the summary line is a
 * display-only readout of this exact payload — deriving it again in the composable would let the
 * summary and the list disagree.
 */
sealed interface StandingOrdersUiState {

    data object Loading : StandingOrdersUiState

    data class Content(
        val orders: List<StandingOrderRowUi>,
        val activeCount: Int,
        val inactiveCount: Int,
    ) : StandingOrdersUiState

    data object Empty : StandingOrdersUiState

    data class Error(val kind: StandingOrdersErrorKind) : StandingOrdersUiState
}

/**
 * One display-ready standing order card. Every field is a finished string; the view model does the
 * formatting.
 *
 * Blank fields mean the bank sent nothing for them, and the card omits those lines rather than
 * rendering a stray label with no value. [finalDateLabel] is governed by [hasFinalPayment] instead
 * of blankness, so an order the bank marked as ending keeps that line even if the date is oddly
 * formatted.
 *
 * @property isActive Drives the badge variant, the card's dimming and nothing else — the status
 *   text itself comes from [statusLabel] so an unexpected status code still reads truthfully.
 * @property currencyLabel Rendered beside [amountLabel] at a smaller size, matching the design.
 *   Separate from the amount so the two can carry different type without string surgery.
 */
data class StandingOrderRowUi(
    val standingOrderId: String,
    val payeeName: String,
    val statusLabel: String,
    val isActive: Boolean,
    val amountLabel: String,
    val currencyLabel: String,
    val frequencyLabel: String,
    val nextDateLabel: String,
    val finalDateLabel: String,
    val hasFinalPayment: Boolean,
    val sortCodeLabel: String,
    val referenceLabel: String,
)

/**
 * The five failure modes the screen distinguishes, each mapped to its own message.
 *
 * Unlike direct debits, every kind here offers Retry — including a revoked consent. The user
 * re-authorises in Consents and comes straight back to this screen, so a Retry that works the
 * moment they return is more useful than a dead end, and TC-SO-007 pins that behaviour.
 */
enum class StandingOrdersErrorKind {
    TokenExpired,
    ConsentRevoked,
    RateLimited,
    ServerError,
    NetworkError,
}

/** Actions the view model owns. Back navigation is the screen's lambda, not routed here. */
sealed interface StandingOrdersAction {
    data object RetryLoad : StandingOrdersAction
}

/**
 * Classifies a stream failure into one of the five [StandingOrdersErrorKind]s.
 *
 * Store5 surfaces the repository's `NetworkError` wrapped in a [RemoteException]. Anything the
 * transport could not categorise — a serialization failure, an unmapped status — falls through to
 * [StandingOrdersErrorKind.ServerError], the message that promises least about the cause.
 */
internal fun classifyStandingOrdersError(throwable: Throwable): StandingOrdersErrorKind =
    when ((throwable as? RemoteException)?.networkError) {
        is NetworkError.Client.Unauthorized -> StandingOrdersErrorKind.TokenExpired
        is NetworkError.Client.Forbidden -> StandingOrdersErrorKind.ConsentRevoked
        is NetworkError.Client.RateLimited -> StandingOrdersErrorKind.RateLimited
        is NetworkError.Network -> StandingOrdersErrorKind.NetworkError
        else -> StandingOrdersErrorKind.ServerError
    }
