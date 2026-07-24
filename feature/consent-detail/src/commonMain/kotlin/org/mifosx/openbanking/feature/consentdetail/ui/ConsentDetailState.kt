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

import org.mifosx.openbanking.core.data.util.RemoteException
import org.mifosx.openbanking.core.model.callback.ConsentStatus
import template.core.base.network.NetworkError

/**
 * Screen state for consent detail.
 *
 * @property consentId The route argument; the consent shown is scoped to it.
 * @property uiState What the screen currently renders.
 */
data class ConsentDetailState(
    val consentId: String,
    val uiState: ConsentDetailUiState = ConsentDetailUiState.Loading,
)

/**
 * The six rendered states.
 *
 * [RevokeConfirm] and [Revoking] both carry the loaded consent, because the design keeps the content
 * visible behind the confirmation dialog and needs it back intact if the user cancels. Revoking is a
 * state rather than a flag so the screen can lock itself down while the call is in flight — the back
 * button and the actions are all inert there, which is what stops a double-tap issuing two DELETEs.
 *
 * [Empty] covers a `200` carrying no consent record. The idea-layer spec lists the state and ships a
 * rendered mockup for it but omits it from the state model and routes nothing to it; it is
 * implemented here because the screen can genuinely reach it and the alternative is rendering a
 * blank content state.
 */
sealed interface ConsentDetailUiState {

    data object Loading : ConsentDetailUiState

    data class Content(val consent: ConsentDetailUi) : ConsentDetailUiState

    data class RevokeConfirm(val consent: ConsentDetailUi) : ConsentDetailUiState

    data class Revoking(val consent: ConsentDetailUi) : ConsentDetailUiState

    data object Empty : ConsentDetailUiState

    data class Error(val kind: ConsentDetailErrorKind) : ConsentDetailUiState
}

/**
 * One display-ready consent.
 *
 * Dates arrive formatted; the words around them stay string resources the composable resolves.
 *
 * @property consentId OBIE `ConsentId`, rendered monospaced under the status chip.
 * @property status The bank's current status, driving the chip label and tone.
 * @property permissions The granted permission codes, in the order the bank returned them.
 * @property connectedDate Formatted `CreationDateTime`, e.g. `28 Jun 2026`.
 * @property expiresDate Formatted `ExpirationDateTime`.
 * @property transactionFromDate Formatted start of the covered transaction window.
 * @property transactionToDate Formatted end of that window.
 * @property expiryWarningDays Days remaining, non-null only inside the 7-day warning window. Null
 *   means no banner — the screen renders it purely from this being present.
 */
data class ConsentDetailUi(
    val consentId: String,
    val status: ConsentStatus,
    val permissions: List<String>,
    val connectedDate: String,
    val expiresDate: String,
    val transactionFromDate: String,
    val transactionToDate: String,
    val expiryWarningDays: Int?,
)

/** The failure modes the screen distinguishes, each mapped to its own message. */
enum class ConsentDetailErrorKind {
    ConsentNotFound,
    ConsentRevoked,
    TokenExpired,
    NetworkError,
    RevokeServerError,
}

/** Actions the view model owns. Back and the reconfirm route are the screen's lambdas. */
sealed interface ConsentDetailAction {

    data object RetryLoad : ConsentDetailAction

    /** Opens the confirmation gate. Makes no API call — that is the point of the two-step flow. */
    data object ConfirmRevoke : ConsentDetailAction

    data object DismissRevokeConfirm : ConsentDetailAction

    data object ExecuteRevoke : ConsentDetailAction
}

/**
 * Classifies a load failure into one of the [ConsentDetailErrorKind]s.
 *
 * A `404` here means the consent is genuinely unknown to the bank, which is a distinct message from
 * a transport fault. On the *revoke* path the same code means the opposite — already gone, therefore
 * success — and that is handled in the repository so this classifier never sees it.
 */
internal fun classifyConsentDetailError(throwable: Throwable): ConsentDetailErrorKind =
    when ((throwable as? RemoteException)?.networkError) {
        is NetworkError.Client.NotFound -> ConsentDetailErrorKind.ConsentNotFound
        is NetworkError.Client.Forbidden -> ConsentDetailErrorKind.ConsentRevoked
        is NetworkError.Client.Unauthorized -> ConsentDetailErrorKind.TokenExpired
        is NetworkError.Server -> ConsentDetailErrorKind.RevokeServerError
        else -> ConsentDetailErrorKind.NetworkError
    }
