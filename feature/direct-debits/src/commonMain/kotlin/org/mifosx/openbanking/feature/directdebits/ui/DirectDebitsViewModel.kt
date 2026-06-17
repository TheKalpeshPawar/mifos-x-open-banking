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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import org.mifosx.openbanking.core.data.accounts.AccountsRepository
import org.mifosx.openbanking.core.data.directdebits.DirectDebitsRepository
import org.mifosx.openbanking.core.datastore.UserPreferencesRepository
import org.mifosx.openbanking.core.model.obp.DirectDebitMandate
import template.core.base.store.screen.DataFreshness
import template.core.base.store.screen.ScreenState

/**
 * Direct Debits ViewModel. Mandates are DERIVED from transaction history (OBP exposes no
 * read endpoint — only POST create exists), so rows come from `TXN_TYPE=DD` collection
 * series. The account comes from route arguments when provided; otherwise the user's
 * persisted default account wins, then the checking-type account, then the first.
 * Cancelling records the cancellation locally (there is no server cancel endpoint) and
 * reprojects the list in place — the mandate flips to Cancelled and the active count drops.
 */
class DirectDebitsViewModel(
    private val directDebitsRepository: DirectDebitsRepository,
    private val accountsRepository: AccountsRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val bankId: String = "",
    private val accountId: String = "",
) : ViewModel() {

    private val rawState = MutableStateFlow<RawState>(RawState.Loading)
    private val cancelTarget = MutableStateFlow<DirectDebitMandate?>(null)

    val uiState: StateFlow<ScreenState<DirectDebitsContent>> =
        combine(rawState, cancelTarget) { raw, dialog -> project(raw, dialog) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ScreenState.Loading,
            )

    init {
        load()
    }

    fun onRetry() = load()

    /** Opens the confirm dialog; cancelled mandates have nothing left to cancel. */
    fun onCancelRequested(mandateId: String) {
        val loaded = rawState.value as? RawState.Loaded ?: return
        cancelTarget.value = loaded.mandates.firstOrNull { it.id == mandateId && it.isActive }
    }

    fun onCancelDismissed() {
        cancelTarget.value = null
    }

    fun onCancelConfirmed() {
        val mandate = cancelTarget.value ?: return
        val loaded = rawState.value as? RawState.Loaded ?: return
        viewModelScope.launch {
            directDebitsRepository.cancel(loaded.accountId, mandate.id)
            cancelTarget.value = null
            directDebitsRepository.listMandates(loaded.bankId, loaded.accountId)
                .onSuccess { rawState.value = loaded.copy(mandates = it) }
        }
    }

    private fun load() {
        rawState.value = RawState.Loading
        viewModelScope.launch {
            val target = resolveAccount().getOrElse {
                rawState.value = RawState.Failed(it)
                return@launch
            }
            if (target == null) {
                rawState.value = RawState.Loaded("", "", emptyList())
                return@launch
            }
            directDebitsRepository.listMandates(target.first, target.second)
                .onSuccess { rawState.value = RawState.Loaded(target.first, target.second, it) }
                .onFailure { rawState.value = RawState.Failed(it) }
        }
    }

    /** (bankId, accountId) to load — route args win; null when the user has no accounts. */
    private suspend fun resolveAccount(): Result<Pair<String, String>?> {
        if (bankId.isNotBlank() && accountId.isNotBlank()) return Result.success(bankId to accountId)
        return accountsRepository.myAccounts().map { accounts ->
            val defaultId = userPreferencesRepository.userData.value.defaultAccountId
            val target = accounts.firstOrNull { it.accountIdOrId == defaultId }
                ?: accounts.firstOrNull { it.typeOrProduct.contains("checking", ignoreCase = true) }
                ?: accounts.firstOrNull()
            target?.let { it.bankId to it.accountIdOrId }
        }
    }

    private fun project(raw: RawState, dialog: DirectDebitMandate?): ScreenState<DirectDebitsContent> =
        when (raw) {
            is RawState.Loading -> ScreenState.Loading
            is RawState.Failed -> ScreenState.Error(raw.error)
            is RawState.Loaded -> if (raw.mandates.isEmpty()) {
                ScreenState.Empty
            } else {
                ScreenState.Content(
                    data = DirectDebitsContent(
                        mandates = raw.mandates.map { it.toRow() },
                        activeCount = raw.mandates.count { it.isActive },
                        cancelDialogFor = dialog?.toRow(),
                    ),
                    freshness = DataFreshness.FRESH,
                )
            }
        }

    private sealed interface RawState {
        data object Loading : RawState
        data class Failed(val error: Throwable) : RawState
        data class Loaded(
            val bankId: String,
            val accountId: String,
            val mandates: List<DirectDebitMandate>,
        ) : RawState
    }
}

/** Loaded content for the Direct Debits screen. */
@Immutable
data class DirectDebitsContent(
    val mandates: List<DirectDebitRow>,
    val activeCount: Int,
    val cancelDialogFor: DirectDebitRow?,
)

/** One display-ready mandate row. */
@Immutable
data class DirectDebitRow(
    val id: String,
    val merchantName: String,
    val amountLabel: String,
    val nextCollectionLabel: String,
    val referenceLabel: String,
    val statusLabel: String,
    val isActive: Boolean,
    val mandateReference: String,
)

internal fun DirectDebitMandate.toRow() = DirectDebitRow(
    id = id,
    merchantName = merchantName,
    amountLabel = "${ddSymbol(amountCurrency)}$amountValue / ${ddFrequencyLabel(frequency)}",
    nextCollectionLabel = if (isActive && nextCollectionDate.isNotBlank()) {
        "Next: ${ddFormatDate(nextCollectionDate)}"
    } else {
        ""
    },
    referenceLabel = if (mandateReference.isBlank()) "" else "Ref: $mandateReference",
    statusLabel = if (isActive) "Active" else "Cancelled",
    isActive = isActive,
    mandateReference = mandateReference,
)

internal fun ddSymbol(currency: String): String = when (currency.uppercase()) {
    "GBP" -> "£"
    "EUR" -> "€"
    "USD" -> "$"
    else -> if (currency.isBlank()) "" else "$currency "
}

internal fun ddFrequencyLabel(frequency: String): String = when (frequency.uppercase()) {
    "DAILY" -> "day"
    "WEEKLY" -> "week"
    "BI-WEEKLY" -> "2 weeks"
    "YEARLY" -> "year"
    else -> "month"
}

/** "2026-06-03" → "3 Jun 2026" (unparseable input passes through). */
internal fun ddFormatDate(iso: String): String {
    val date = runCatching { LocalDate.parse(iso) }.getOrNull() ?: return iso
    val month = DD_MONTHS[date.month.number - 1]
    return "${date.day} $month ${date.year}"
}

private val DD_MONTHS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)
