/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.standingorders.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mifosx.openbanking.core.data.standingorders.StandingOrdersRepository
import org.mifosx.openbanking.core.model.obp.StandingOrder
import org.mifosx.openbanking.core.model.obp.StandingOrderDetail
import org.mifosx.openbanking.core.model.obp.StandingOrderExecution
import template.core.base.store.screen.DataFreshness
import template.core.base.store.screen.ScreenState

/**
 * One standing order, fully expanded. Data comes from the derived/created order set
 * (OBP exposes no standing-order read endpoint) and its execution history is the booked
 * `TXN_TYPE=SO` transactions of the series — so pause/resume/cancel cannot hit a real
 * endpoint and the screen surfaces them as deferred actions.
 */
class StandingOrderDetailViewModel(
    private val standingOrdersRepository: StandingOrdersRepository,
    private val bankId: String,
    private val accountId: String,
    private val standingOrderId: String,
) : ViewModel() {

    private val state = MutableStateFlow<ScreenState<StandingOrderDetailContent>>(ScreenState.Loading)
    val uiState: StateFlow<ScreenState<StandingOrderDetailContent>> = state.asStateFlow()

    init {
        load()
    }

    fun onRetry() = load()

    private fun load() {
        state.value = ScreenState.Loading
        viewModelScope.launch {
            standingOrdersRepository.detail(bankId, accountId, standingOrderId)
                .onSuccess { state.value = ScreenState.Content(it.toContent(), DataFreshness.FRESH) }
                .onFailure { state.value = ScreenState.Error(it) }
        }
    }

    companion object {
        const val MAX_EXECUTIONS = 5
    }
}

/** Display-ready projection of one standing order for the detail screen. */
@Immutable
data class StandingOrderDetailContent(
    val name: String,
    val statusLabel: String,
    val isActive: Boolean,
    val isCreated: Boolean,
    val recipientName: String,
    val recipientAccount: String,
    val amount: String,
    val currency: String,
    val frequencyLabel: String,
    val startedOn: String,
    val lastPayment: String,
    val nextPayment: String,
    val finalDate: String,
    val executions: List<ExecutionRow>,
)

/** One row of the Recent Executions card; [transactionId] drills into transaction detail. */
@Immutable
data class ExecutionRow(
    val transactionId: String,
    val dateLabel: String,
    val amountLabel: String,
)

private fun StandingOrderDetail.toContent() = StandingOrderDetailContent(
    name = order.name.ifBlank { "Standing Order" },
    statusLabel = statusLabel(order.status),
    isActive = order.isActive,
    isCreated = order.created,
    recipientName = order.counterpartyName.ifBlank { order.name }.ifBlank { "Recipient" },
    recipientAccount = maskAccount(order.counterpartyAccount),
    amount = formatMoney(order.amountValue, order.amountCurrency),
    currency = order.amountCurrency,
    frequencyLabel = frequencyDisplay(order.frequency),
    startedOn = executions.lastOrNull()?.date?.let(::formatDate).orEmpty(),
    lastPayment = order.lastPaymentDate.takeIf { it.isNotBlank() }?.let(::formatDate).orEmpty(),
    nextPayment = order.nextPaymentDate.takeIf { it.isNotBlank() }?.let(::formatDate).orEmpty(),
    finalDate = "Ongoing",
    executions = executions.take(StandingOrderDetailViewModel.MAX_EXECUTIONS).map { it.toRow() },
)

private fun StandingOrderExecution.toRow() = ExecutionRow(
    transactionId = transactionId,
    dateLabel = formatDate(date),
    amountLabel = formatMoney(amount, currency),
)

private fun statusLabel(status: String): String = when (status) {
    StandingOrder.STATUS_ACTIVE -> "Active"
    StandingOrder.STATUS_PAUSED -> "Paused"
    StandingOrder.STATUS_CANCELLED -> "Cancelled"
    else -> status.lowercase().replaceFirstChar { it.uppercase() }
}

private fun frequencyDisplay(frequency: String): String = when (frequency.uppercase()) {
    "DAILY" -> "Daily"
    "WEEKLY" -> "Weekly"
    "BI-WEEKLY" -> "Every 2 weeks"
    "YEARLY" -> "Yearly"
    "MONTHLY" -> "Monthly"
    else -> frequency.ifBlank { "Monthly" }
}

/** "ac.savings.001" → "····s001"; blank-safe. */
private fun maskAccount(id: String): String {
    val tail = id.filter { it.isLetterOrDigit() }.takeLast(4)
    return if (tail.isBlank()) "" else "····$tail"
}
