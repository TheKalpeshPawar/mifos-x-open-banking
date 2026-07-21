/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accountdetail.ui

import org.mifosx.openbanking.core.data.util.RemoteException
import template.core.base.network.NetworkError

/**
 * Screen state for the account-detail hub.
 *
 * @property accountId The route argument; also handed to every Explore chip as it navigates on.
 * @property uiState What the screen currently renders.
 */
data class AccountDetailState(
    val accountId: String,
    val uiState: AccountDetailUiState = AccountDetailUiState.Loading,
)

/**
 * The four rendered states.
 *
 * [Empty] deliberately carries the header: an account the bank reports no balances for still shows
 * its identity and its navigation chips, so it is a content-bearing state rather than an absence
 * of data.
 */
sealed interface AccountDetailUiState {

    data object Loading : AccountDetailUiState

    data class Content(
        val header: AccountHeaderUi,
        val balances: List<BalanceRowUi>,
    ) : AccountDetailUiState

    data class Empty(val header: AccountHeaderUi) : AccountDetailUiState

    data class Error(
        val kind: AccountDetailErrorKind,
        val recoverable: Boolean,
    ) : AccountDetailUiState
}

/** Display-ready account header. Every field is a finished string; the view model does the work. */
data class AccountHeaderUi(
    val nickname: String,
    val accountSubType: String,
    val identificationLabel: String,
    val currency: String,
    val servicerIdentification: String,
    val lastUpdatedLabel: String,
)

/** One typed balance row, e.g. `InterimAvailable` / `2,847.63 GBP`. */
data class BalanceRowUi(
    val type: String,
    val amountLabel: String,
)

/**
 * The five failure modes the screen distinguishes, each mapped to its own message.
 *
 * [recoverable] drives Retry visibility: a withdrawn consent or an account outside the authorised
 * set will not resolve by retrying, so those two offer back-navigation instead.
 */
enum class AccountDetailErrorKind(val recoverable: Boolean) {
    TokenExpired(recoverable = true),
    ConsentWithdrawn(recoverable = false),
    AccountNotFound(recoverable = false),
    Network(recoverable = true),
    Unexpected(recoverable = true),
}

/** Actions the view model owns. Navigation is handled by the screen's lambdas, not routed here. */
sealed interface AccountDetailAction {
    data object RetryLoad : AccountDetailAction
}

/**
 * Classifies a stream failure into one of the five [AccountDetailErrorKind]s.
 *
 * Store5 surfaces the repository's `NetworkError` wrapped in a [RemoteException]; anything else
 * (a mapper failure, an unexpected runtime error) falls through to [AccountDetailErrorKind.Unexpected].
 */
internal fun classifyAccountDetailError(throwable: Throwable): AccountDetailErrorKind =
    when ((throwable as? RemoteException)?.networkError) {
        is NetworkError.Client.Unauthorized -> AccountDetailErrorKind.TokenExpired
        is NetworkError.Client.Forbidden -> AccountDetailErrorKind.ConsentWithdrawn
        is NetworkError.Client.NotFound -> AccountDetailErrorKind.AccountNotFound
        is NetworkError.Network -> AccountDetailErrorKind.Network
        else -> AccountDetailErrorKind.Unexpected
    }
