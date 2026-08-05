/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.paymentstatus.ui

import org.mifosx.openbanking.core.data.util.RemoteException
import org.mifosx.openbanking.core.model.banking.payment.PaymentDisposition
import template.core.base.network.NetworkError

/**
 * Why a status read failed.
 *
 * There is no `Empty`: a payment id either resolves or it does not, and one that cannot be found is
 * [PaymentNotFound] — a failure to explain, not an absence to shrug at.
 */
enum class PaymentStatusErrorKind {
    PaymentNotFound,
    TokenExpired,
    ConsentRevoked,
    NetworkError,
}

sealed interface PaymentStatusUiState {

    data object Loading : PaymentStatusUiState

    /**
     * @property inProgress Whether the payment is still in flight, which is what decides the note
     *   and the chip's colour. In-flight is not a fault, so it must not render as one.
     * @property refreshing Whether a manual re-read is running. Distinct from [Loading]: the current
     *   status stays on screen while it refreshes rather than collapsing back to a skeleton.
     */
    data class Content(
        val paymentId: String,
        val statusLabel: String,
        val disposition: PaymentDisposition,
        val amountLabel: String,
        val creditorName: String,
        val reference: String,
        val debtorLabel: String,
        val submittedAt: String,
        val refreshing: Boolean = false,
    ) : PaymentStatusUiState {

        val inProgress: Boolean
            get() = disposition == PaymentDisposition.InProgress
    }

    data class Error(val kind: PaymentStatusErrorKind) : PaymentStatusUiState
}

data class PaymentStatusState(
    val paymentId: String,
    val uiState: PaymentStatusUiState = PaymentStatusUiState.Loading,
)

sealed interface PaymentStatusAction {
    /** Re-reads on demand. Never polled: a terminal status will not change again. */
    data object RefreshStatus : PaymentStatusAction
}

internal fun classifyPaymentStatusError(throwable: Throwable): PaymentStatusErrorKind =
    when ((throwable as? RemoteException)?.networkError) {
        is NetworkError.Client.NotFound -> PaymentStatusErrorKind.PaymentNotFound
        is NetworkError.Client.Unauthorized -> PaymentStatusErrorKind.TokenExpired
        is NetworkError.Client.Forbidden -> PaymentStatusErrorKind.ConsentRevoked
        else -> PaymentStatusErrorKind.NetworkError
    }
