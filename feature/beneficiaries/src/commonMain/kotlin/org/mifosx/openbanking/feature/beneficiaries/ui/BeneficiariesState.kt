/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.beneficiaries.ui

import org.mifosx.openbanking.core.data.util.RemoteException
import org.mifosx.openbanking.core.model.banking.BeneficiaryItem
import template.core.base.network.NetworkError

/**
 * Screen state for the beneficiaries list.
 *
 * @property accountId The route argument; the payee list is scoped to it.
 * @property uiState What the screen currently renders.
 */
data class BeneficiariesState(
    val accountId: String,
    val uiState: BeneficiariesUiState = BeneficiariesUiState.Loading,
)

/**
 * The four rendered states.
 *
 * [Empty] is distinct from [Error]: the fetch succeeded and the bank reported no saved payees for
 * the account — a real answer, not a failure. A search that matches nothing is *not* a state of its
 * own; it stays [Content] with an empty [Content.filtered], because the query is still live and
 * clearing it must bring the rows straight back.
 */
sealed interface BeneficiariesUiState {

    data object Loading : BeneficiariesUiState

    /**
     * @property all Every payee the bank returned, in its order. Never filtered — this is what the
     *   search re-runs against, so clearing the query restores the full list without a re-fetch.
     * @property filtered What the list renders: [all] when [query] is blank, otherwise the matches.
     * @property query The live search text.
     */
    data class Content(
        val all: List<BeneficiaryItem>,
        val filtered: List<BeneficiaryItem>,
        val query: String = "",
    ) : BeneficiariesUiState {

        /** True when a live query matched nothing — the in-content no-results block. */
        val isSearchWithoutMatches: Boolean get() = filtered.isEmpty() && query.isNotBlank()
    }

    data object Empty : BeneficiariesUiState

    data class Error(val kind: BeneficiariesErrorKind) : BeneficiariesUiState
}

/**
 * The five failure modes the screen distinguishes.
 *
 * [ConsentRevoked] is the one that is not retriable: retrying a withdrawn consent fails again until
 * the user re-authorises, so that case offers "View Consents" instead and every other case offers
 * Retry. The two are mutually exclusive by design — see [isRetriable].
 */
enum class BeneficiariesErrorKind {
    TokenExpired,
    ConsentRevoked,
    RateLimited,
    NetworkError,
    ServerError,
}

/**
 * Whether this failure offers Retry rather than the re-authorise route.
 *
 * Defined as "anything but a revoked consent". The idea-layer `ui.yaml` visibility condition omits
 * `ServerError` from its retriable list while `api.yaml` assigns that code a Retry CTA; the
 * inclusive reading is the one the API contract and the error copy both support, and it keeps the
 * two buttons strictly exclusive with no failure left with neither.
 */
internal val BeneficiariesErrorKind.isRetriable: Boolean
    get() = this != BeneficiariesErrorKind.ConsentRevoked

/** Actions the view model owns. Back navigation is the screen's lambda, not routed here. */
sealed interface BeneficiariesAction {

    data object RetryLoad : BeneficiariesAction

    data class Search(val query: String) : BeneficiariesAction
}

/**
 * Classifies a stream failure into one of the five [BeneficiariesErrorKind]s.
 *
 * Store5 surfaces the repository's `NetworkError` wrapped in a [RemoteException]. Anything the
 * transport could not categorise falls through to [BeneficiariesErrorKind.NetworkError], which is
 * retriable — an uncategorised fault is more often transient than permanent.
 */
internal fun classifyBeneficiariesError(throwable: Throwable): BeneficiariesErrorKind =
    when ((throwable as? RemoteException)?.networkError) {
        is NetworkError.Client.Unauthorized -> BeneficiariesErrorKind.TokenExpired
        is NetworkError.Client.Forbidden -> BeneficiariesErrorKind.ConsentRevoked
        is NetworkError.Client.RateLimited -> BeneficiariesErrorKind.RateLimited
        is NetworkError.Server -> BeneficiariesErrorKind.ServerError
        is NetworkError.Network -> BeneficiariesErrorKind.NetworkError
        else -> BeneficiariesErrorKind.NetworkError
    }
