/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accountholder.ui

import org.mifosx.openbanking.core.data.util.RemoteException
import org.mifosx.openbanking.core.model.banking.PartyProfile
import template.core.base.network.NetworkError

/**
 * Screen state for the account-holder screen.
 *
 * @property accountId The route argument; the party identity is scoped to it.
 * @property uiState What the screen currently renders.
 */
data class AccountHolderState(
    val accountId: String,
    val uiState: AccountHolderUiState = AccountHolderUiState.Loading,
)

/**
 * The four rendered states — identity only.
 *
 * Consent management and sign-out live in Settings → Consents, not here, so this screen has no
 * expiry banner, permissions list or sign-out overlay: just the account holder's identity, plus the
 * usual loading / empty / error surfaces.
 */
sealed interface AccountHolderUiState {

    data object Loading : AccountHolderUiState

    /**
     * A rendered identity.
     *
     * @property profile The party identity. Its fields are blank rather than null when the bank sent
     *   nothing, and each identity row hides itself on a blank value.
     */
    data class Content(
        val profile: PartyProfile,
    ) : AccountHolderUiState

    data object Empty : AccountHolderUiState

    data class Error(val kind: AccountHolderErrorKind) : AccountHolderUiState
}

/**
 * The four failure modes the screen distinguishes, each mapped to its own message.
 *
 * [isRetriable] drives Retry visibility. A consent granted without `ReadParty`, and a bank with no
 * identity record for the account, both fail identically on every attempt — the user has to
 * re-authorise — so offering the button there would be a lie.
 */
enum class AccountHolderErrorKind(val isRetriable: Boolean) {
    TokenExpired(isRetriable = true),
    ConsentMissingParty(isRetriable = false),
    ProfileNotFound(isRetriable = false),
    LoadFailed(isRetriable = true),
}

/** Actions the view model owns. Back navigation is the screen's lambda, not routed here. */
sealed interface AccountHolderAction {
    data object RetryLoad : AccountHolderAction
}

/**
 * Classifies a stream failure into one of the four [AccountHolderErrorKind]s.
 *
 * Store5 surfaces the repository's `NetworkError` wrapped in a [RemoteException]. Anything the
 * transport could not categorise — a serialization failure, an unmapped status — falls through to
 * [AccountHolderErrorKind.LoadFailed], which is retriable: an uncategorised fault is more often transient
 * than permanent, and the user is not stranded without a way to try again.
 */
internal fun classifyAccountHolderError(throwable: Throwable): AccountHolderErrorKind =
    when ((throwable as? RemoteException)?.networkError) {
        is NetworkError.Client.Unauthorized -> AccountHolderErrorKind.TokenExpired
        is NetworkError.Client.Forbidden -> AccountHolderErrorKind.ConsentMissingParty
        is NetworkError.Client.NotFound -> AccountHolderErrorKind.ProfileNotFound
        else -> AccountHolderErrorKind.LoadFailed
    }
