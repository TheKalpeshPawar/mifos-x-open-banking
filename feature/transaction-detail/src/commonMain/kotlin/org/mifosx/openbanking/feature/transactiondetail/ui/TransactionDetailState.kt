/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.transactiondetail.ui

import org.mifosx.openbanking.core.data.util.RemoteException
import org.mifosx.openbanking.core.model.banking.TransactionCategory
import template.core.base.network.NetworkError

/**
 * Screen state for the transaction-detail view.
 *
 * @property transactionId The route argument used to resolve the single record from the account list.
 * @property accountId The route argument; the transaction list is scoped to it.
 * @property uiState What the screen currently renders.
 */
data class TransactionDetailState(
    val transactionId: String,
    val accountId: String,
    val uiState: TransactionDetailUiState = TransactionDetailUiState.Loading,
)

/**
 * The four rendered states.
 *
 * [Empty] is distinct from [Error]: the fetch succeeded with a valid response, but the route's
 * `transactionId` matched no record (the transaction dropped out of HSBC's window, or consent narrowed
 * since the list was fetched). That is a not-found, not a failure, so it uses the neutral empty state.
 */
sealed interface TransactionDetailUiState {

    data object Loading : TransactionDetailUiState

    data class Content(val transaction: TransactionDetailUiModel) : TransactionDetailUiState

    data class Error(val kind: TransactionDetailErrorKind) : TransactionDetailUiState

    data object Empty : TransactionDetailUiState
}

/**
 * One display-ready transaction. Every field is a finished string the view model computed, except
 * [category] (resolved to a localised label in the composable) and the two booleans that drive colour.
 *
 * @property amountLabel The signed amount, e.g. `-£42.17` (debit) or `+£2,400.00` (credit).
 * @property isCredit Drives the amount colour: primary for credit, error for debit.
 * @property currencyLabel The ISO-4217 currency code shown in muted meta, e.g. `GBP`.
 * @property merchantLabel The merchant name, or the transaction narrative when the payload has none.
 * @property status The OBIE status, e.g. `Booked` / `Pending`.
 * @property isBooked Drives the status chip colour: primary-container when booked, else secondary.
 * @property bookingDateLabel The booking timestamp as `26 Jun 2026, 11:22`.
 * @property valueDateLabel The value timestamp as `26 Jun 2026, 11:22`.
 * @property category The client-derived category; the composable resolves its label.
 * @property merchantCategoryCode The ISO-18245 MCC, or null — the MCC row hides when null.
 * @property balanceAfterLabel The running balance after the transaction, e.g. `£447.63`.
 * @property referenceLabel The free-text reference (also the clipboard payload).
 * @property bankCodeLabel The proprietary code and issuer, e.g. `DR · HSBC`.
 */
data class TransactionDetailUiModel(
    val amountLabel: String,
    val isCredit: Boolean,
    val currencyLabel: String,
    val merchantLabel: String,
    val status: String,
    val isBooked: Boolean,
    val bookingDateLabel: String,
    val valueDateLabel: String,
    val category: TransactionCategory,
    val merchantCategoryCode: String?,
    val balanceAfterLabel: String,
    val referenceLabel: String,
    val bankCodeLabel: String,
)

/**
 * The four failure modes the screen distinguishes.
 *
 * [recoverable] drives which button the error state shows: a recoverable failure (an expired token, a
 * dropped connection) offers Retry; a withdrawn consent will fail identically on every attempt, so it
 * offers Go Back instead. A `transactionId` absent from a successful response is not an error at all —
 * it routes to the Empty state.
 */
enum class TransactionDetailErrorKind(val recoverable: Boolean) {
    TokenExpired(recoverable = true),
    ConsentWithdrawn(recoverable = false),
    Network(recoverable = true),
    Unexpected(recoverable = true),
}

/** Actions the view model owns. Back navigation is the screen's lambda, not routed here. */
sealed interface TransactionDetailAction {

    data object RetryLoad : TransactionDetailAction

    /** Requests the reference string be copied to the system clipboard. */
    data class CopyReference(val reference: String) : TransactionDetailAction
}

/**
 * One-shot events. Clipboard access is a UI concern (it needs the composition's clipboard manager), so
 * the view model emits the text to copy and the screen performs the write and shows the confirmation.
 */
sealed interface TransactionDetailEvent {

    /** Copy [text] to the system clipboard and confirm with a snackbar. */
    data class CopyToClipboard(val text: String) : TransactionDetailEvent
}

/**
 * Classifies a stream failure into one of the four [TransactionDetailErrorKind]s.
 *
 * Store5 surfaces the repository's `NetworkError` wrapped in a [RemoteException]; anything it cannot
 * categorise (a mapper failure, an unexpected runtime error) falls through to
 * [TransactionDetailErrorKind.Unexpected], which is recoverable — an uncategorised fault is more often
 * transient than permanent.
 */
internal fun classifyTransactionDetailError(throwable: Throwable): TransactionDetailErrorKind =
    when ((throwable as? RemoteException)?.networkError) {
        is NetworkError.Client.Unauthorized -> TransactionDetailErrorKind.TokenExpired
        is NetworkError.Client.Forbidden -> TransactionDetailErrorKind.ConsentWithdrawn
        is NetworkError.Network -> TransactionDetailErrorKind.Network
        else -> TransactionDetailErrorKind.Unexpected
    }
