/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.directdebits.ui

import org.mifosx.openbanking.core.data.util.RemoteException
import template.core.base.network.NetworkError

/**
 * Screen state for the direct-debits list.
 *
 * @property accountId The route argument; the mandate list is scoped to it.
 * @property uiState What the screen currently renders.
 */
data class DirectDebitsState(
    val accountId: String,
    val uiState: DirectDebitsUiState = DirectDebitsUiState.Loading,
)

/**
 * The rendered states; [Content] carries the mandate rows.
 */
sealed interface DirectDebitsUiState {

    data object Loading : DirectDebitsUiState

    data class Content(
        val mandates: List<DirectDebitRowUi>,
    ) : DirectDebitsUiState

    data object Empty : DirectDebitsUiState

    /**
     * The account's HSBC product does not offer direct debits at all.
     *
     * A sibling of [Empty], not an [Error]: the bank has not failed, it has given a definitive
     * answer. Every [DirectDebitsErrorKind] offers Retry, and retrying can never change this.
     *
     * @property message The ASPSP's own explanation, shown verbatim rather than paraphrased here.
     *   Empty when the response carried no message.
     */
    data class Unsupported(val message: String) : DirectDebitsUiState

    data class Error(val kind: DirectDebitsErrorKind) : DirectDebitsUiState
}

/**
 * One display-ready mandate card. Every field is a finished string; the view model does the
 * formatting.
 *
 * [previousPaymentAmount] and [previousPaymentDateTime] are blank when the bank sent nothing for
 * them, and the card renders those rows with empty values rather than a placeholder.
 *
 * @property isActive Drives the badge style, the card's dimming and nothing else — the status text
 *   itself comes from [statusLabel] so an unexpected status code still reads truthfully.
 */
data class DirectDebitRowUi(
    val mandateId: String,
    val name: String,
    val statusLabel: String,
    val isActive: Boolean,
    val previousPaymentAmount: String,
    val previousPaymentDateTime: String,
)

/**
 * The five failure modes the screen distinguishes, each mapped to its own message.
 *
 * [isRetriable] drives Retry visibility. A revoked consent will fail identically on every attempt —
 * the user has to re-authorise in Consents — so offering the button there would be a lie.
 */
enum class DirectDebitsErrorKind(val isRetriable: Boolean) {
    TokenExpired(isRetriable = true),
    ConsentRevoked(isRetriable = false),
    RateLimited(isRetriable = true),
    ServerError(isRetriable = true),
    NetworkError(isRetriable = true),
}

/** Actions the view model owns. Back navigation is the screen's lambda, not routed here. */
sealed interface DirectDebitsAction {
    data object RetryLoad : DirectDebitsAction
}

/**
 * Classifies a stream failure into one of the five [DirectDebitsErrorKind]s.
 *
 * Store5 surfaces the repository's `NetworkError` wrapped in a [RemoteException]. Anything the
 * transport could not categorise — a serialization failure, an unmapped status — falls through to
 * [DirectDebitsErrorKind.ServerError], which is retriable: an uncategorised fault is more often
 * transient than permanent, and the user is not stranded without a way to try again.
 */
internal fun classifyDirectDebitsError(throwable: Throwable): DirectDebitsErrorKind =
    when ((throwable as? RemoteException)?.networkError) {
        is NetworkError.Client.Unauthorized -> DirectDebitsErrorKind.TokenExpired
        is NetworkError.Client.Forbidden -> DirectDebitsErrorKind.ConsentRevoked
        is NetworkError.Client.RateLimited -> DirectDebitsErrorKind.RateLimited
        is NetworkError.Network -> DirectDebitsErrorKind.NetworkError
        else -> DirectDebitsErrorKind.ServerError
    }
