/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.paymentconsent.ui

import org.mifosx.openbanking.core.data.util.RemoteException
import template.core.base.network.NetworkError

/**
 * Why an authorisation return failed.
 *
 * [StateMismatch] and [NoPendingAuthorisation] are both replay signals: a callback arriving with no
 * matching pending authorisation is not something to retry through, it is something that must not be
 * trusted at all.
 */
enum class PaymentConsentErrorKind {
    StateMismatch,
    NoPendingAuthorisation,
    CodeExpired,
    ConsentRejected,
    AuthorisationTimedOut,
    NetworkError,
}

/**
 * The three progress states are separate rather than one `loading` so the copy can say which stage
 * is running. An authorisation that appears to hang is otherwise indistinguishable from one that has
 * already failed.
 */
sealed interface PaymentConsentUiState {

    /** Checking the callback's `state` and `nonce` against the authorisation that was launched. */
    data object Validating : PaymentConsentUiState

    /** Trading the authorization code for the payments-scoped PSU token. */
    data object Exchanging : PaymentConsentUiState

    /**
     * Polling until the consent reports `Authorised`.
     *
     * Load-bearing, not cosmetic: submitting against a consent that has not reached `AUTH` returns
     * `400 U009`, so the hand-back to send-money is gated on this. It offers Check again rather than
     * spinning indefinitely, because a bank that is slow to authorise is a normal outcome.
     */
    data class Checking(val canCheckAgain: Boolean = false) : PaymentConsentUiState

    data object Authorised : PaymentConsentUiState

    data class Error(val kind: PaymentConsentErrorKind) : PaymentConsentUiState
}

data class PaymentConsentState(
    val uiState: PaymentConsentUiState = PaymentConsentUiState.Validating,
    val consentId: String = "",
)

sealed interface PaymentConsentAction {
    data object CheckAgain : PaymentConsentAction
    data object RetryAuthorisation : PaymentConsentAction
    data object AbandonPayment : PaymentConsentAction
}

/**
 * Handed back to send-money rather than acted on here.
 *
 * The payment is send-money's to complete — it holds the idempotency key and the staged
 * `Initiation` — so this screen reports the outcome and gets out of the way.
 */
sealed interface PaymentConsentEvent {
    data class Authorised(val consentId: String) : PaymentConsentEvent
    data object RestartAuthorisation : PaymentConsentEvent
    data object Abandoned : PaymentConsentEvent
}

internal fun classifyPaymentConsentError(throwable: Throwable): PaymentConsentErrorKind =
    when ((throwable as? RemoteException)?.networkError) {
        is NetworkError.Client.Unauthorized -> PaymentConsentErrorKind.CodeExpired
        is NetworkError.Client.Forbidden -> PaymentConsentErrorKind.ConsentRejected
        else -> PaymentConsentErrorKind.NetworkError
    }
