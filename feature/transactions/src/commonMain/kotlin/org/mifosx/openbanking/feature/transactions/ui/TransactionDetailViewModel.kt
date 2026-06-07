/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.transactions.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mifosx.openbanking.core.data.payments.PaymentsRepository
import org.mifosx.openbanking.core.data.transactions.TransactionsRepository
import org.mifosx.openbanking.core.model.obp.Transaction
import org.mifosx.openbanking.core.model.obp.TransactionRequestSummary
import org.mifosx.openbanking.feature.transactions.categoryLabel
import org.mifosx.openbanking.feature.transactions.formatDate
import org.mifosx.openbanking.feature.transactions.formatSigned
import org.mifosx.openbanking.feature.transactions.formatUnsigned
import template.core.base.store.screen.DataFreshness
import template.core.base.store.screen.ScreenState

/**
 * One transaction, fully expanded. Booked rows load via the single-transaction OBP endpoint
 * (v3.0.0 — no attributes inline, so the category falls back to the detail type when the
 * row was opened without a TXN_TYPE). Pending rows are transaction-requests (INITIATED, no
 * booked transaction yet) — there is no per-request read endpoint, so the request is found
 * in the account's transaction-request list by [requestId].
 */
class TransactionDetailViewModel(
    private val transactionsRepository: TransactionsRepository,
    private val paymentsRepository: PaymentsRepository,
    private val bankId: String,
    private val accountId: String,
    private val transactionId: String,
    private val requestId: String = "",
) : ViewModel() {

    private val state = MutableStateFlow<ScreenState<TransactionDetailContent>>(ScreenState.Loading)
    val uiState: StateFlow<ScreenState<TransactionDetailContent>> = state.asStateFlow()

    init {
        load()
    }

    fun onRetry() = load()

    private fun load() {
        when {
            transactionId.isNotBlank() -> loadBooked()
            requestId.isNotBlank() -> loadPending()
            else -> state.value = ScreenState.Empty
        }
    }

    private fun loadBooked() {
        state.value = ScreenState.Loading
        viewModelScope.launch {
            transactionsRepository.getTransaction(bankId, accountId, transactionId)
                .onSuccess { state.value = ScreenState.Content(it.toDetailContent(), DataFreshness.FRESH) }
                .onFailure { state.value = ScreenState.Error(it) }
        }
    }

    private fun loadPending() {
        state.value = ScreenState.Loading
        viewModelScope.launch {
            paymentsRepository.listTransactionRequests(bankId, accountId)
                .mapCatching { requests ->
                    requests.first { it.id == requestId }.toPendingContent(accountId)
                }
                .onSuccess { state.value = ScreenState.Content(it, DataFreshness.FRESH) }
                .onFailure { state.value = ScreenState.Error(it) }
        }
    }
}

/** Display-ready projection of one transaction (booked or pending) for the detail screen. */
@Immutable
data class TransactionDetailContent(
    val amount: String,
    val isDebit: Boolean,
    val isPending: Boolean,
    val statusLabel: String,
    val counterpartyName: String,
    val category: String,
    val dateTime: String,
    val reference: String,
    val typeLabel: String,
    val fromAccountLabel: String,
    val fromAccountTail: String,
    val toDetail: String,
    val narrative: String,
)

private fun Transaction.toDetailContent(): TransactionDetailContent {
    val value = details.value.amount.toDoubleOrNull() ?: 0.0
    return TransactionDetailContent(
        amount = formatSigned(value, details.value.currency),
        isDebit = value < 0,
        isPending = false,
        statusLabel = "Completed",
        counterpartyName = otherAccount.holder.name
            .ifBlank { details.description }
            .ifBlank { "Payment" },
        category = categoryLabel(this),
        dateTime = formatDateTime(details.completed.ifBlank { details.posted }),
        reference = txId,
        typeLabel = typeLabel(details.type),
        fromAccountLabel = thisAccount.label.ifBlank { "Account" },
        fromAccountTail = "...${thisAccount.id.takeLast(4)}",
        toDetail = "",
        narrative = metadata.narrative,
    )
}

/**
 * A transaction-request awaiting SCA confirmation — an outgoing payment whose money has
 * NOT left the account yet, so the amount renders unsigned rather than as a debit.
 */
private fun TransactionRequestSummary.toPendingContent(accountId: String): TransactionDetailContent {
    val value = details.value.amount.trimStart('-').toDoubleOrNull() ?: 0.0
    return TransactionDetailContent(
        amount = formatUnsigned(value, details.value.currency),
        isDebit = true,
        isPending = true,
        statusLabel = "Awaiting confirmation",
        counterpartyName = details.description.ifBlank { "Payment" },
        category = "Pending",
        dateTime = formatDateTime(startDate),
        reference = id,
        typeLabel = typeLabel(type),
        fromAccountLabel = "Account",
        fromAccountTail = "...${accountId.takeLast(4)}",
        toDetail = details.toSepa?.iban?.takeIf { it.isNotBlank() }
            ?: details.toCounterparty?.counterpartyId?.takeIf { it.isNotBlank() }
            ?: details.toSandboxTan?.accountId.orEmpty(),
        narrative = "",
    )
}

/** "2026-05-25T14:32:00Z" → "25 May 2026, 14:32" (empty when the timestamp is unparseable). */
internal fun formatDateTime(iso: String): String {
    val date = parseIsoDate(iso) ?: return ""
    val time = iso.substringAfter('T', "").take(5)
    return if (time.isBlank()) formatDate(date) else "${formatDate(date)}, $time"
}

private fun typeLabel(type: String): String = when (type.uppercase()) {
    "SEPA" -> "SEPA Credit Transfer"
    "SANDBOX_TAN" -> "Transfer"
    "COUNTERPARTY" -> "Counterparty Transfer"
    "DIRECT_DEBIT", "DD" -> "Direct Debit"
    "STANDING_ORDER", "SO" -> "Standing Order"
    else -> type.ifBlank { "Payment" }
}
