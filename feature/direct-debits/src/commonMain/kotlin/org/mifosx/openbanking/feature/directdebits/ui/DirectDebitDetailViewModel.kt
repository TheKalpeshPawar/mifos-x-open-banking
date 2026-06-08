/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.directdebits.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.mifosx.openbanking.core.data.accounts.AccountsRepository
import org.mifosx.openbanking.core.data.directdebits.DirectDebitsRepository
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.core.model.obp.DirectDebitMandate
import template.core.base.store.screen.DataFreshness
import template.core.base.store.screen.ScreenState

/**
 * Direct Debit Detail ViewModel for one derived mandate. The mandate is located in the
 * account's derived series (OBP exposes no read endpoint, so [DirectDebitsRepository]
 * reconstructs mandates from `TXN_TYPE=DD` history) by [mandateId]; the linked account's
 * display name is resolved best-effort and never blocks the screen. Cancelling records the
 * cancellation locally (there is no server cancel endpoint) and re-derives the mandate in
 * place — it flips to Cancelled and the next-payment date clears. A [mandateId] that no
 * longer resolves (already cancelled/expired before open) renders the empty state.
 */
class DirectDebitDetailViewModel(
    private val directDebitsRepository: DirectDebitsRepository,
    private val accountsRepository: AccountsRepository,
    private val bankId: String = "",
    private val accountId: String = "",
    private val mandateId: String = "",
) : ViewModel() {

    private val rawState = MutableStateFlow<RawState>(RawState.Loading)
    private val cancelDialog = MutableStateFlow(false)

    val uiState: StateFlow<ScreenState<DirectDebitDetailContent>> =
        combine(rawState, cancelDialog) { raw, dialog -> project(raw, dialog) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ScreenState.Loading,
            )

    init {
        load()
    }

    fun onRetry() = load()

    /** Opens the confirm dialog; a cancelled mandate has nothing left to cancel. */
    fun onCancelRequested() {
        if ((rawState.value as? RawState.Loaded)?.mandate?.isActive == true) cancelDialog.value = true
    }

    fun onCancelDismissed() {
        cancelDialog.value = false
    }

    fun onCancelConfirmed() {
        val loaded = rawState.value as? RawState.Loaded ?: return
        viewModelScope.launch {
            directDebitsRepository.cancel(accountId, loaded.mandate.id)
            cancelDialog.value = false
            fetch()
        }
    }

    private fun load() {
        rawState.value = RawState.Loading
        viewModelScope.launch { fetch() }
    }

    private suspend fun fetch() {
        directDebitsRepository.listMandates(bankId, accountId)
            .onSuccess { mandates ->
                val mandate = mandates.firstOrNull { it.id == mandateId }
                rawState.value = if (mandate == null) {
                    RawState.Missing
                } else {
                    RawState.Loaded(mandate, accountsRepository.accountDetail(bankId, accountId).getOrNull())
                }
            }
            .onFailure { rawState.value = RawState.Failed(it) }
    }

    private fun project(raw: RawState, dialog: Boolean): ScreenState<DirectDebitDetailContent> = when (raw) {
        is RawState.Loading -> ScreenState.Loading
        is RawState.Failed -> ScreenState.Error(raw.error)
        is RawState.Missing -> ScreenState.Empty
        is RawState.Loaded -> ScreenState.Content(
            data = detailContent(raw.mandate, raw.account, dialog),
            freshness = DataFreshness.FRESH,
        )
    }

    private fun detailContent(
        mandate: DirectDebitMandate,
        account: Account?,
        dialog: Boolean,
    ): DirectDebitDetailContent = DirectDebitDetailContent(
        merchantName = mandate.merchantName,
        statusLabel = if (mandate.isActive) "Active" else "Cancelled",
        isActive = mandate.isActive,
        amountLabel = "${ddSymbol(mandate.amountCurrency)}${mandate.amountValue}",
        frequencyLabel = ddFrequencyName(mandate.frequency),
        nextPaymentLabel = if (mandate.isActive && mandate.nextCollectionDate.isNotBlank()) {
            ddFormatDate(mandate.nextCollectionDate)
        } else {
            EM_DASH
        },
        linkedAccountName = accountDisplayName(account),
        linkedAccountMasked = maskAccount(accountId),
        mandateReference = mandate.mandateReference.ifBlank { EM_DASH },
        startDateLabel = if (mandate.firstCollectionDate.isNotBlank()) {
            ddFormatDate(mandate.firstCollectionDate)
        } else {
            EM_DASH
        },
        recentPayments = mandate.recentCollections.map {
            DirectDebitPaymentRow(
                dateLabel = ddFormatDate(it.date),
                amountLabel = "-${ddSymbol(it.amountCurrency)}${it.amountValue}",
            )
        },
        cancelDialogVisible = dialog && mandate.isActive,
    )

    private sealed interface RawState {
        data object Loading : RawState
        data object Missing : RawState
        data class Failed(val error: Throwable) : RawState
        data class Loaded(val mandate: DirectDebitMandate, val account: Account?) : RawState
    }
}

/** Display-ready content for the Direct Debit Detail screen. */
@Immutable
data class DirectDebitDetailContent(
    val merchantName: String,
    val statusLabel: String,
    val isActive: Boolean,
    val amountLabel: String,
    val frequencyLabel: String,
    val nextPaymentLabel: String,
    val linkedAccountName: String,
    val linkedAccountMasked: String,
    val mandateReference: String,
    val startDateLabel: String,
    val recentPayments: List<DirectDebitPaymentRow>,
    val cancelDialogVisible: Boolean,
)

/** One collected payment in the mandate's recent history. */
@Immutable
data class DirectDebitPaymentRow(
    val dateLabel: String,
    val amountLabel: String,
)

private const val EM_DASH = "—"

private const val MASK = "••••"

/** Account display name with the same fallback the pickers use: label, else type, else "Account". */
private fun accountDisplayName(account: Account?): String = when {
    account == null -> "Account"
    account.label.isNotBlank() -> account.label
    else -> account.typeOrProduct.ifBlank { "Account" }
}

/** "ac.checking.4521" → "••••4521" — last four alphanumerics of the account id. */
private fun maskAccount(accountId: String): String {
    val tail = accountId.filter { it.isLetterOrDigit() }.takeLast(4)
    return if (tail.isBlank()) MASK else "$MASK$tail"
}

/** Title-case frequency for the hero (the list uses the per-period "month" form). */
internal fun ddFrequencyName(frequency: String): String = when (frequency.uppercase()) {
    "DAILY" -> "Daily"
    "WEEKLY" -> "Weekly"
    "BI-WEEKLY" -> "Every 2 weeks"
    "YEARLY" -> "Yearly"
    else -> "Monthly"
}
