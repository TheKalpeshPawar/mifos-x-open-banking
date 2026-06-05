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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.mifosx.openbanking.core.data.accounts.AccountsRepository
import org.mifosx.openbanking.core.data.customers.CustomersRepository
import org.mifosx.openbanking.core.data.payments.PaymentsRepository
import org.mifosx.openbanking.core.data.profile.ProfileRepository
import org.mifosx.openbanking.core.data.standingorders.StandingOrdersRepository
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.core.model.obp.AmountOfMoney
import org.mifosx.openbanking.core.model.obp.Counterparty
import org.mifosx.openbanking.core.model.obp.CreateStandingOrderRequest
import org.mifosx.openbanking.core.model.obp.StandingOrder
import org.mifosx.openbanking.core.model.obp.StandingOrderSchedule
import template.core.base.store.screen.DataFreshness
import template.core.base.store.screen.ScreenState
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** OBP standing-order frequencies offered by the create form. */
val STANDING_ORDER_FREQUENCIES = listOf("WEEKLY", "MONTHLY", "YEARLY")

/** Status filter chips above the list. */
enum class StandingOrderFilter { All, Active, Paused, Cancelled }

/**
 * Standing Orders ViewModel. Resolves the user's primary account (checking-first, like
 * Beneficiaries) and loads its recurring payments — DERIVED from transaction history,
 * because OBP exposes no read endpoint for standing orders. The create sheet POSTs the
 * real create endpoint, resolving the caller's user/customer ids on demand.
 */
class StandingOrdersViewModel(
    private val standingOrdersRepository: StandingOrdersRepository,
    private val accountsRepository: AccountsRepository,
    private val paymentsRepository: PaymentsRepository,
    private val profileRepository: ProfileRepository,
    private val customersRepository: CustomersRepository,
) : ViewModel() {

    private val rawState = MutableStateFlow<RawState>(RawState.Loading)
    private val createState = MutableStateFlow(CreateSheetState())
    private val filterFlow = MutableStateFlow(StandingOrderFilter.All)

    val createSheet: StateFlow<CreateSheetState> = createState.asStateFlow()

    val uiState: StateFlow<ScreenState<StandingOrdersContent>> =
        combine(rawState, filterFlow) { raw, filter ->
            when (raw) {
                is RawState.Loading -> ScreenState.Loading
                is RawState.Failed -> ScreenState.Error(raw.error)
                is RawState.Loaded ->
                    if (raw.orders.isEmpty()) {
                        ScreenState.Empty
                    } else {
                        ScreenState.Content(
                            data = projectContent(raw.orders, filter),
                            freshness = DataFreshness.FRESH,
                        )
                    }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ScreenState.Loading,
        )

    private var account: Account? = null

    init {
        load()
    }

    fun onRetry() = load()

    fun onRefresh() = load()

    fun onFilterChanged(filter: StandingOrderFilter) = filterFlow.update { filter }

    private fun projectContent(orders: List<StandingOrder>, filter: StandingOrderFilter): StandingOrdersContent {
        val visible = when (filter) {
            StandingOrderFilter.All -> orders
            StandingOrderFilter.Active -> orders.filter { it.isActive }
            StandingOrderFilter.Paused -> orders.filter { it.isPaused }
            StandingOrderFilter.Cancelled -> orders.filter { it.isCancelled }
        }
        val active = orders.filter { it.isActive }
        val currency = orders.firstOrNull()?.amountCurrency.orEmpty()
        return StandingOrdersContent(
            orders = visible,
            activeCount = active.size,
            pausedCount = orders.count { it.isPaused },
            monthlyTotal = formatMoney(monthlyTotalOf(active).toString(), currency),
            filter = filter,
        )
    }

    /** Opens the create sheet, loading the account's payees as counterparty choices. */
    fun onCreateClicked() {
        val acct = account ?: return
        createState.update { it.copy(visible = true, loadingPayees = true, error = null) }
        viewModelScope.launch {
            val payees = paymentsRepository
                .listBeneficiaries(acct.bankId, acct.accountIdOrId)
                .getOrDefault(emptyList())
                .filter { it.isBeneficiary }
            createState.update { it.copy(loadingPayees = false, payees = payees) }
        }
    }

    fun onDismissCreate() = createState.update { CreateSheetState() }

    /** POSTs the standing order, then refreshes the list and closes the sheet. */
    fun onSubmitCreate(counterpartyId: String, amount: String, frequency: String) {
        val acct = account
        val payee = createState.value.payees.firstOrNull { it.counterpartyId == counterpartyId }
        if (acct == null || payee == null) return
        val normalized = amount.trim().replace(',', '.')
        if ((normalized.toDoubleOrNull() ?: 0.0) <= 0.0) {
            createState.update { it.copy(error = "Enter a valid amount") }
            return
        }
        createState.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            buildRequest(acct, counterpartyId, normalized, frequency)
                .mapCatching { request ->
                    standingOrdersRepository.create(acct.accountIdOrId, payee.name, request).getOrThrow()
                }
                .onSuccess {
                    createState.value = CreateSheetState()
                    load()
                }
                .onFailure { e ->
                    createState.update {
                        it.copy(submitting = false, error = e.message ?: "Could not create standing order")
                    }
                }
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun buildRequest(
        acct: Account,
        counterpartyId: String,
        amount: String,
        frequency: String,
    ): Result<CreateStandingOrderRequest> = runCatching {
        val userId = profileRepository.current().getOrThrow().userId
        val customerId = customersRepository.currentUserCustomers().getOrThrow()
            .firstOrNull { it.bankId == acct.bankId }
            ?.customerId
            ?: error("No customer record at ${acct.bankId}")
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        CreateStandingOrderRequest(
            customerId = customerId,
            userId = userId,
            counterpartyId = counterpartyId,
            amount = AmountOfMoney(currency = acct.balance.currency.ifBlank { "EUR" }, amount = amount),
            `when` = StandingOrderSchedule(frequency = frequency),
            dateSigned = "${today}T00:00:00Z",
            dateStarts = "${today}T00:00:00Z",
        )
    }

    private fun load() {
        rawState.value = RawState.Loading
        viewModelScope.launch {
            val accounts = accountsRepository.myAccounts().getOrElse {
                rawState.value = RawState.Failed(it)
                return@launch
            }
            val target = accounts.firstOrNull { it.typeOrProduct.contains("checking", ignoreCase = true) }
                ?: accounts.firstOrNull()
            if (target == null) {
                account = null
                rawState.value = RawState.Loaded(emptyList())
                return@launch
            }
            account = target
            standingOrdersRepository.listRecurring(target.accountIdOrId)
                .onSuccess { rawState.value = RawState.Loaded(it) }
                .onFailure { rawState.value = RawState.Failed(it) }
        }
    }

    private sealed interface RawState {
        data object Loading : RawState
        data class Failed(val error: Throwable) : RawState
        data class Loaded(val orders: List<StandingOrder>) : RawState
    }
}

/** Loaded content for the Standing Orders screen. */
@Immutable
data class StandingOrdersContent(
    /** Orders visible under the current [filter]. */
    val orders: List<StandingOrder>,
    val activeCount: Int,
    val pausedCount: Int,
    /** Formatted monthly-equivalent total across ACTIVE orders, e.g. "€665.99". */
    val monthlyTotal: String,
    val filter: StandingOrderFilter,
)

/** Sum of active orders normalized to a per-month amount. */
internal fun monthlyTotalOf(active: List<StandingOrder>): Double =
    active.sumOf { order ->
        val amount = order.amountValue.toDoubleOrNull() ?: 0.0
        when (order.frequency.uppercase()) {
            "DAILY" -> amount * 30
            "WEEKLY" -> amount * 4
            "BI-WEEKLY" -> amount * 2
            "YEARLY" -> amount / 12
            else -> amount
        }
    }

/** Create-sheet UI state: payee choices + submit progress/error. */
@Immutable
data class CreateSheetState(
    val visible: Boolean = false,
    val loadingPayees: Boolean = false,
    val payees: List<Counterparty> = emptyList(),
    val submitting: Boolean = false,
    val error: String? = null,
)

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
