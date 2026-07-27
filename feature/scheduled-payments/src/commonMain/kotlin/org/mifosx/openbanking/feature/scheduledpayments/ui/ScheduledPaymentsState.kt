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

import org.mifosx.openbanking.core.data.util.RemoteException
import org.mifosx.openbanking.core.model.banking.ScheduledPaymentType
import template.core.base.network.NetworkError

/**
 * Screen state for the scheduled-payments list.
 *
 * @property accountId The route argument; the payment list is scoped to it.
 * @property uiState What the screen currently renders.
 */
data class ScheduledPaymentsState(
    val accountId: String,
    val uiState: ScheduledPaymentsUiState = ScheduledPaymentsUiState.Loading,
)

/**
 * The four rendered states.
 *
 * [Empty] is distinct from [Error]: the fetch succeeded and the bank reported no future-dated
 * payments for the account — a real answer, not a failure — so it uses the neutral empty state.
 */
sealed interface ScheduledPaymentsUiState {

    data object Loading : ScheduledPaymentsUiState

    data class Content(val payments: List<ScheduledPaymentUiModel>) : ScheduledPaymentsUiState

    data object Empty : ScheduledPaymentsUiState

    data class Error(val kind: ScheduledPaymentsError) : ScheduledPaymentsUiState
}

/**
 * One display-ready scheduled payment. Every text field is a finished string the view model
 * computed, except [scheduledType], which the card resolves to a label, icon and accessibility
 * phrasing so the enum-to-presentation mapping lives with the composable that renders it.
 *
 * @property scheduledPaymentId OBIE `ScheduledPaymentId`; the stable key a card renders under.
 * @property payeeName Beneficiary display name, e.g. `HMRC Self Assessment`.
 * @property amountLabel The currency and amount, e.g. `GBP 842.00`.
 * @property scheduledDateLabel The scheduled date as `Fri 31 Jul 2026`, or the raw ISO string when
 *   it could not be parsed.
 * @property scheduledType Whether the date is an execution or an arrival date; drives the type chip.
 * @property creditorIdentification The destination sort code and account number, e.g.
 *   `08-32-00 12001039`.
 * @property reference The free-text payment reference, e.g. `HMRC-SA-2526`.
 */
data class ScheduledPaymentUiModel(
    val scheduledPaymentId: String,
    val payeeName: String,
    val amountLabel: String,
    val scheduledDateLabel: String,
    val scheduledType: ScheduledPaymentType,
    val creditorIdentification: String,
    val reference: String,
)

/**
 * The four failure modes the screen distinguishes, each mapped to its own message.
 *
 * All are retriable: the design shows a Retry button on every failure. A revoked consent will fail
 * again until the user re-authorises, but the screen still offers Retry rather than dead-ending, and
 * routes the user back through the failure they can see.
 */
enum class ScheduledPaymentsError {
    TokenExpired,
    ConsentRevoked,
    RateLimited,
    NetworkError,
}

/** Actions the view model owns. Back navigation is the screen's lambda, not routed here. */
sealed interface ScheduledPaymentsAction {
    data object RetryLoad : ScheduledPaymentsAction
}

/**
 * Classifies a stream failure into one of the four [ScheduledPaymentsError]s.
 *
 * Store5 surfaces the repository's `NetworkError` wrapped in a [RemoteException]. Anything the
 * transport could not categorise falls through to [ScheduledPaymentsError.NetworkError], which is
 * retriable — an uncategorised fault is more often transient than permanent.
 */
internal fun classifyScheduledPaymentsError(throwable: Throwable): ScheduledPaymentsError =
    when ((throwable as? RemoteException)?.networkError) {
        is NetworkError.Client.Unauthorized -> ScheduledPaymentsError.TokenExpired
        is NetworkError.Client.Forbidden -> ScheduledPaymentsError.ConsentRevoked
        is NetworkError.Client.RateLimited -> ScheduledPaymentsError.RateLimited
        is NetworkError.Network -> ScheduledPaymentsError.NetworkError
        else -> ScheduledPaymentsError.NetworkError
    }
