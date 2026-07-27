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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.mifosx.openbanking.core.common.formatMoney
import org.mifosx.openbanking.core.data.banking.TransactionDetailRepository
import org.mifosx.openbanking.core.model.banking.TransactionDetail
import template.core.base.common.screen.ScreenState
import template.core.base.ui.viewmodel.BaseViewModel
import kotlin.time.Instant

/**
 * Resolves and renders a single transaction.
 *
 * OBIE exposes no single-transaction endpoint, so the view model subscribes to the account's whole
 * transaction stream and filters it by the route's `transactionId`: a match becomes [Content], a
 * successful fetch with no match becomes [TransactionDetailUiState.Empty] (a not-found, distinct from a
 * fetch failure), and a failure is classified into a recoverable-or-not [Error]. Retry re-runs the
 * fetch through the stream the view model owns.
 *
 * All display formatting — the signed amount, the dates, the running balance, the bank code — happens
 * here, so the composables receive finished strings and stay testable on the JVM. Copy-to-clipboard is
 * a UI concern, so [TransactionDetailAction.CopyReference] emits a [TransactionDetailEvent] the screen
 * performs against its clipboard manager rather than the view model touching the platform.
 */
class TransactionDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: TransactionDetailRepository,
) : BaseViewModel<TransactionDetailState, TransactionDetailEvent, TransactionDetailAction>(
    initialState = TransactionDetailState(
        transactionId = savedStateHandle.get<String>(TRANSACTION_ID_ARG).orEmpty(),
        accountId = savedStateHandle.get<String>(ACCOUNT_ID_ARG).orEmpty(),
    ),
) {

    /** The stream this screen renders, scoped to this view model. */
    private val stream = repository.transactionDetailStream(state.accountId, viewModelScope)

    init {
        val transactionId = state.transactionId
        stream.state
            .onEach { screenState -> updateState { copy(uiState = screenState.toUiState(transactionId)) } }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: TransactionDetailAction) {
        when (action) {
            TransactionDetailAction.RetryLoad -> stream.refresh()
            is TransactionDetailAction.CopyReference ->
                sendEvent(TransactionDetailEvent.CopyToClipboard(action.reference))
        }
    }

    private fun ScreenState<List<TransactionDetail>>.toUiState(
        transactionId: String,
    ): TransactionDetailUiState = when (this) {
        is ScreenState.Content -> resolve(data, transactionId)
        is ScreenState.Error -> TransactionDetailUiState.Error(classifyTransactionDetailError(error))
        is ScreenState.NoNetwork -> TransactionDetailUiState.Error(TransactionDetailErrorKind.Network)
        ScreenState.Unauthenticated -> TransactionDetailUiState.Error(TransactionDetailErrorKind.TokenExpired)
        ScreenState.Empty -> TransactionDetailUiState.Empty
        ScreenState.Loading -> TransactionDetailUiState.Loading
    }

    /** A `transactionId` matching a record is Content; a successful fetch with no match is Empty. */
    private fun resolve(
        transactions: List<TransactionDetail>,
        transactionId: String,
    ): TransactionDetailUiState =
        transactions.firstOrNull { it.transactionId == transactionId }
            ?.let { TransactionDetailUiState.Content(it.toUiModel()) }
            ?: TransactionDetailUiState.Empty

    private fun TransactionDetail.toUiModel(): TransactionDetailUiModel = TransactionDetailUiModel(
        amountLabel = signedAmount(amount, currency, isCredit),
        isCredit = isCredit,
        currencyLabel = currency,
        merchantLabel = merchantName ?: transactionInformation,
        status = status,
        isBooked = status.equals(BOOKED_STATUS, ignoreCase = true),
        bookingDateLabel = formatTimestamp(bookingDateTime),
        valueDateLabel = formatTimestamp(valueDateTime),
        category = category,
        merchantCategoryCode = merchantCategoryCode,
        balanceAfterLabel = balanceAmountLabel(balanceAmount, balanceCurrency),
        referenceLabel = transactionInformation,
        bankCodeLabel = bankCode(proprietaryCode, proprietaryIssuer),
    )

    companion object {
        /** Must match the [TransactionDetailRoute] `accountId` property name — nav uses it as the key. */
        const val ACCOUNT_ID_ARG: String = "accountId"

        /** Must match the [TransactionDetailRoute] `transactionId` property name — nav's key. */
        const val TRANSACTION_ID_ARG: String = "transactionId"
    }
}

private const val BOOKED_STATUS = "Booked"
private const val BANK_CODE_SEPARATOR = " · "
private const val TIME_FIELD_WIDTH = 2
private const val MONTH_ABBREVIATION_LENGTH = 3

/** `-£42.17` for a debit, `+£2,400.00` for a credit — the amount is always positive minor units. */
private fun signedAmount(amount: String, currency: String, isCredit: Boolean): String {
    val sign = if (isCredit) "+" else "-"
    return "$sign${formatMoney(amount, currency)}"
}

/**
 * `£21,530.92` — the running balance as a magnitude, deliberately unsigned.
 *
 * The payload's `Balance.CreditDebitIndicator` cannot be used to sign this. HSBC sets it per
 * transaction to the *transaction's* direction, not the balance's, so every debit on an account in
 * credit arrives as `Debit`: the sandbox returns `Balance {Debit, ITBD, 21530.92}` while that same
 * account's `/balances` reports `{Credit, ITBD, 21530.92}` — identical figure and type, opposite
 * indicator. Signing on it rendered an account holding £21,530.92 as `-£21,530.92`.
 *
 * A truthful sign would need the account-level balance, which this screen does not load, so the
 * magnitude is the most this data supports. Showing no sign is incomplete; showing the wrong one
 * tells the PSU they are overdrawn when they are not.
 */
private fun balanceAmountLabel(amount: String, currency: String): String = formatMoney(amount, currency)

/** `DR · HSBC`, or just the code when the issuer is absent. */
private fun bankCode(code: String, issuer: String): String =
    if (issuer.isBlank()) code else "$code$BANK_CODE_SEPARATOR$issuer"

/** `26 Jun 2026, 11:22` from an ISO-8601 UTC instant, falling back to the raw string when unparseable. */
private fun formatTimestamp(isoDateTime: String): String {
    val dateTime = runCatching {
        Instant.parse(isoDateTime).toLocalDateTime(TimeZone.UTC)
    }.getOrNull() ?: return isoDateTime
    return dateTime.toDisplayLabel()
}

private fun LocalDateTime.toDisplayLabel(): String {
    val month = month.name.lowercase().replaceFirstChar { it.uppercase() }.take(MONTH_ABBREVIATION_LENGTH)
    val hh = hour.toString().padStart(TIME_FIELD_WIDTH, '0')
    val mm = minute.toString().padStart(TIME_FIELD_WIDTH, '0')
    return "$day $month $year, $hh:$mm"
}
