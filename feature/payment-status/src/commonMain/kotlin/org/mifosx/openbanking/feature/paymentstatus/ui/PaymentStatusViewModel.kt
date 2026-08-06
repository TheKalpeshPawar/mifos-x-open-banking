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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import org.mifosx.openbanking.core.common.formatDateTime
import org.mifosx.openbanking.core.common.formatSortCode
import org.mifosx.openbanking.core.common.formatTimeOfDay
import org.mifosx.openbanking.core.data.banking.PaymentInitiationRepository
import org.mifosx.openbanking.core.data.util.toThrowable
import org.mifosx.openbanking.core.model.banking.payment.PaymentReceipt
import template.core.base.network.NetworkResult
import template.core.base.ui.viewmodel.BaseViewModel
import kotlin.time.Clock

private const val SORT_CODE_DIGITS = 6

/**
 * Reads one submitted payment's settlement status.
 *
 * Refresh-on-demand rather than polled: a terminal status will not change again, and polling one is
 * wasted work. The read is cheap enough to survive the PSU token expiring — it falls back to a
 * client-credentials payments token — so a customer can come back tomorrow and still see the
 * outcome.
 */
class PaymentStatusViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: PaymentInitiationRepository,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : BaseViewModel<PaymentStatusState, Nothing, PaymentStatusAction>(
    initialState = PaymentStatusState(
        paymentId = savedStateHandle.get<String>(PAYMENT_ID_ARG).orEmpty(),
    ),
) {

    init {
        load(refreshing = false)
    }

    override fun handleAction(action: PaymentStatusAction) {
        when (action) {
            PaymentStatusAction.RefreshStatus -> load(refreshing = true)
        }
    }

    /**
     * A manual refresh keeps the current status on screen rather than collapsing to a skeleton —
     * replacing a known answer with a placeholder reads as losing it.
     */
    private fun load(refreshing: Boolean) {
        val current = state.uiState
        if (refreshing && current is PaymentStatusUiState.Content) {
            updateState { copy(uiState = current.copy(refreshing = true)) }
        }

        viewModelScope.launch {
            when (val result = repository.paymentStatus(state.paymentId)) {
                is NetworkResult.Success ->
                    updateState { copy(uiState = result.data.toContent()) }

                is NetworkResult.Error -> updateState {
                    copy(
                        uiState = PaymentStatusUiState.Error(
                            classifyPaymentStatusError(result.error.toThrowable()),
                        ),
                    )
                }
            }
        }
    }

    /**
     * The three timestamps are separate facts and are shown separately.
     *
     * [PaymentReceipt.creationDateTime] is when the payment was made — the only one that means
     * "submitted". [PaymentReceipt.statusUpdateDateTime] is when the status last moved, which HSBC
     * returns unchanged even on a settled payment. Conflating them, as this did, put the wrong
     * label on the wrong value and left the right one unread.
     */
    private fun PaymentReceipt.toContent(): PaymentStatusUiState.Content =
        PaymentStatusUiState.Content(
            paymentId = domesticPaymentId,
            status = status,
            disposition = status.disposition,
            amountLabel = amountLabel,
            creditorName = creditorName,
            reference = reference,
            debtorLabel = debtorIdentification.toAccountLabel(),
            submittedAt = formatDateTime(creationDateTime, timeZone),
            settledAt = settlementDateTime
                .takeIf { it.isNotBlank() }
                ?.let { formatDateTime(it, timeZone) }
                .orEmpty(),
            statusChangedAt = formatDateTime(statusUpdateDateTime, timeZone),
            charges = charges,
            lastCheckedAt = formatTimeOfDay(clock.now(), timeZone),
        )

    companion object {
        /** Must match the [org.mifosx.openbanking.feature.paymentstatus.PaymentStatusRoute] property. */
        const val PAYMENT_ID_ARG: String = "paymentId"
    }
}

/**
 * Renders the OBIE identification the way it is written down — `40-05-15 12345678` — rather than the
 * unpunctuated fourteen digits the wire carries.
 */
private fun String.toAccountLabel(): String {
    val digits = filter(Char::isDigit)
    if (digits.length <= SORT_CODE_DIGITS) return this
    return "${formatSortCode(digits.take(SORT_CODE_DIGITS))} ${digits.drop(SORT_CODE_DIGITS)}"
}
