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
import org.mifosx.openbanking.core.model.banking.payment.PaymentCharge
import org.mifosx.openbanking.core.model.banking.payment.PaymentDisposition
import org.mifosx.openbanking.core.model.banking.payment.PaymentStatus
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
     * @property status The bank's own status, rendered as its OBIE meaning beneath the chip. The
     *   chip says the plain-English thing; this is the evidence behind it, and the difference
     *   matters when someone is asking whether a refresh actually did anything.
     * @property inProgress Whether the payment is still in flight, which is what decides the note
     *   and the chip's colour. In-flight is not a fault, so it must not render as one.
     * @property submittedAt When the payment was made, formatted. From the bank's
     *   `CreationDateTime` — not its `StatusUpdateDateTime`, which means something else.
     * @property settledAt When the funds are expected to settle, formatted, or empty when the bank
     *   did not say. Empty means the row is not drawn at all rather than drawn blank.
     * @property statusChangedAt When the status last moved, formatted. HSBC returns this equal to
     *   [submittedAt] even on a settled payment; that is its answer, shown as given.
     * @property charges What the bank actually charged. Empty renders no fee row — silence is not
     *   the same claim as "£0.00", and only the bank can tell us which is true.
     * @property lastCheckedAt Local clock time of the most recent successful read. Exists so an
     *   unchanged status reads as "checked, no change yet" instead of a dead button.
     * @property refreshing Whether a manual re-read is running. Distinct from [Loading]: the current
     *   status stays on screen while it refreshes rather than collapsing back to a skeleton.
     */
    data class Content(
        val paymentId: String,
        val status: PaymentStatus,
        val disposition: PaymentDisposition,
        val amountLabel: String,
        val creditorName: String,
        val reference: String,
        val debtorLabel: String,
        val submittedAt: String,
        val settledAt: String = "",
        val statusChangedAt: String = "",
        val charges: List<PaymentCharge> = emptyList(),
        val lastCheckedAt: String = "",
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
