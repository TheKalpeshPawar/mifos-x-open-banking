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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mifosx.openbanking.core.data.accounts.AccountsRepository
import org.mifosx.openbanking.core.data.payments.PaymentsRepository
import org.mifosx.openbanking.core.data.standingorders.StandingOrdersRepository
import org.mifosx.openbanking.core.datastore.UserPreferencesRepository
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.core.model.obp.StandingOrder
import template.core.base.store.screen.DataFreshness
import template.core.base.store.screen.ScreenState

/** Status filter chips above the list. */
enum class StandingOrderFilter { All, Active, Paused, Cancelled }

/**
 * Standing Orders ViewModel. Resolves the user's primary account (checking-first, like
 * Beneficiaries) and loads its recurring payments — DERIVED from transaction history,
 * because OBP exposes no read endpoint for standing orders. Creation lives on its own
 * screen ([CreateStandingOrderViewModel]).
 */
class StandingOrdersViewModel(
    private val standingOrdersRepository: StandingOrdersRepository,
    private val accountsRepository: AccountsRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val paymentsRepository: PaymentsRepository,
) : ViewModel() {

    private val rawState = MutableStateFlow<RawState>(RawState.Loading)
    private val filterFlow = MutableStateFlow(StandingOrderFilter.All)
    private val pickerState = MutableStateFlow(AccountPickerState())
    private val createGateFlow = MutableStateFlow<CreateGate>(CreateGate.Idle)

    /** Gate for the New Order FAB: blocks navigation when the account has no payees. */
    val createGate: StateFlow<CreateGate> = createGateFlow.asStateFlow()

    /**
     * Pinned header (account picker + stats + filter chips) — derived from whatever data
     * is actually loaded and kept OUTSIDE [uiState], so it stays visible with zeroed
     * stats while the list below shows loading/empty/error.
     */
    val header: StateFlow<StandingOrdersHeader> =
        combine(rawState, filterFlow, pickerState) { raw, filter, picker ->
            val orders = (raw as? RawState.Loaded)?.orders.orEmpty()
            val active = orders.filter { it.isActive }
            val currency = orders.firstOrNull()?.amountCurrency
                ?: picker.accounts
                    .firstOrNull { it.accountIdOrId == picker.selectedAccountId }
                    ?.balance?.currency
                    .orEmpty()
            StandingOrdersHeader(
                accounts = picker.accounts,
                selectedAccountId = picker.selectedAccountId,
                activeCount = active.size,
                pausedCount = orders.count { it.isPaused },
                monthlyTotal = formatMoney(monthlyTotalOf(active).toString(), currency),
                filter = filter,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StandingOrdersHeader(),
        )

    /** List-area state only: the orders visible under the current filter. */
    val uiState: StateFlow<ScreenState<List<StandingOrder>>> =
        combine(rawState, filterFlow) { raw, filter ->
            when (raw) {
                is RawState.Loading -> ScreenState.Loading
                is RawState.Failed -> ScreenState.Error(raw.error)
                is RawState.Loaded ->
                    if (raw.orders.isEmpty()) {
                        ScreenState.Empty
                    } else {
                        ScreenState.Content(
                            data = filterOrders(raw.orders, filter),
                            freshness = DataFreshness.FRESH,
                        )
                    }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ScreenState.Loading,
        )

    private var accounts: List<Account> = emptyList()
    private var selectedAccountId: String = ""

    init {
        load()
    }

    fun onRetry() = load()

    fun onRefresh() = load()

    fun onFilterChanged(filter: StandingOrderFilter) = filterFlow.update { filter }

    /**
     * New Order tapped: standing orders pay an existing payee, so check the selected
     * account's beneficiaries first — no payees means a dialog instead of the form.
     */
    fun onCreateClicked() {
        if (createGateFlow.value is CreateGate.Checking) return
        val account = accounts.firstOrNull { it.accountIdOrId == selectedAccountId } ?: return
        createGateFlow.value = CreateGate.Checking
        viewModelScope.launch {
            val payees = paymentsRepository
                .listBeneficiaries(account.bankId, account.accountIdOrId)
                .getOrDefault(emptyList())
                .filter { it.isBeneficiary }
            createGateFlow.value = if (payees.isEmpty()) {
                CreateGate.NoPayees
            } else {
                CreateGate.Ready(account.accountIdOrId)
            }
        }
    }

    /** Reset the gate after navigating or dismissing the no-payees dialog. */
    fun onCreateGateConsumed() {
        createGateFlow.value = CreateGate.Idle
    }

    /** Switch the active account: standing orders are per-account, so reload for it. */
    fun onAccountSelected(accountId: String) {
        if (accountId == selectedAccountId) return
        val account = accounts.firstOrNull { it.accountIdOrId == accountId } ?: return
        selectedAccountId = accountId
        pickerState.value = AccountPickerState(accounts, selectedAccountId)
        rawState.value = RawState.Loading
        viewModelScope.launch { loadForAccount(account) }
    }

    private fun filterOrders(orders: List<StandingOrder>, filter: StandingOrderFilter): List<StandingOrder> =
        when (filter) {
            StandingOrderFilter.All -> orders
            StandingOrderFilter.Active -> orders.filter { it.isActive }
            StandingOrderFilter.Paused -> orders.filter { it.isPaused }
            StandingOrderFilter.Cancelled -> orders.filter { it.isCancelled }
        }

    private fun load() {
        rawState.value = RawState.Loading
        viewModelScope.launch {
            accounts = accountsRepository.myAccounts().getOrElse {
                rawState.value = RawState.Failed(it)
                return@launch
            }
            val defaultId = userPreferencesRepository.userData.value.defaultAccountId
            val target = accounts.firstOrNull { it.accountIdOrId == selectedAccountId }
                ?: accounts.firstOrNull { it.accountIdOrId == defaultId }
                ?: accounts.firstOrNull { it.typeOrProduct.contains("checking", ignoreCase = true) }
                ?: accounts.firstOrNull()
            if (target == null) {
                selectedAccountId = ""
                pickerState.value = AccountPickerState()
                rawState.value = RawState.Loaded(emptyList())
                return@launch
            }
            selectedAccountId = target.accountIdOrId
            pickerState.value = AccountPickerState(accounts, selectedAccountId)
            loadForAccount(target)
        }
    }

    private suspend fun loadForAccount(account: Account) {
        standingOrdersRepository.listRecurring(account.bankId, account.accountIdOrId)
            .onSuccess { rawState.value = RawState.Loaded(it) }
            .onFailure { rawState.value = RawState.Failed(it) }
    }

    private sealed interface RawState {
        data object Loading : RawState
        data class Failed(val error: Throwable) : RawState
        data class Loaded(val orders: List<StandingOrder>) : RawState
    }
}

/** Account picker state — independent of the list so it stays visible on error/empty. */
@Immutable
data class AccountPickerState(
    val accounts: List<Account> = emptyList(),
    val selectedAccountId: String = "",
)

/** Pinned header data: picker + stats (zeroed when nothing is loaded) + active filter. */
@Immutable
data class StandingOrdersHeader(
    val accounts: List<Account> = emptyList(),
    val selectedAccountId: String = "",
    val activeCount: Int = 0,
    val pausedCount: Int = 0,
    val monthlyTotal: String = "0.00",
    val filter: StandingOrderFilter = StandingOrderFilter.All,
) {
    val selectedAccount: Account?
        get() = accounts.firstOrNull { it.accountIdOrId == selectedAccountId }
}

/** New Order gating: payee check outcome for the selected account. */
sealed interface CreateGate {
    data object Idle : CreateGate
    data object Checking : CreateGate
    data object NoPayees : CreateGate
    data class Ready(val accountId: String) : CreateGate
}

/** Display name for an account: its label, else "{TYPE} ····{last4}". */
fun accountDisplayName(account: Account?): String = when {
    account == null -> ""
    account.label.isNotBlank() -> account.label
    else -> {
        val type = account.typeOrProduct.ifBlank { "Account" }
        val last4 = account.accountIdOrId.filter { it.isLetterOrDigit() }.takeLast(4)
        "$type ····$last4"
    }
}

/** Plain sum of the active orders' payment amounts (no frequency normalization). */
internal fun monthlyTotalOf(active: List<StandingOrder>): Double =
    active.sumOf { it.amountValue.toDoubleOrNull() ?: 0.0 }

/** Formats an OBP money value ("45.00", "EUR") to a display string ("€45.00"). */
internal fun formatMoney(amount: String, currency: String): String {
    val value = amount.toDoubleOrNull() ?: return "$currency $amount".trim()
    val abs = if (value < 0) -value else value
    val cents = kotlin.math.round(abs * 100).toLong()
    val symbol = when (currency.uppercase()) {
        "GBP" -> "£"
        "EUR" -> "€"
        "USD" -> "$"
        else -> if (currency.isBlank()) "" else "$currency "
    }
    return "$symbol${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"
}

/** Formats an ISO date ("2026-07-01") to "1 Jul 2026"; blank-safe. */
internal fun formatDate(iso: String): String {
    val parts = iso.substringBefore('T').trim().split('-')
    val months = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    )
    val formatted = parts.takeIf { it.size == 3 }?.let { p ->
        val day = p[2].toIntOrNull()
        val name = p[1].toIntOrNull()?.let { months.getOrNull(it - 1) }
        if (day != null && name != null) "$day $name ${p[0]}" else null
    }
    return formatted ?: iso
}
