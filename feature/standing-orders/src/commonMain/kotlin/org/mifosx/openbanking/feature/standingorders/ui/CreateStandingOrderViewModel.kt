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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
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
import org.mifosx.openbanking.core.model.obp.StandingOrderSchedule
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** OBP standing-order frequencies offered by the create form. */
val STANDING_ORDER_FREQUENCIES = listOf("DAILY", "WEEKLY", "BI-WEEKLY", "MONTHLY", "YEARLY")

/**
 * New Standing Order ViewModel. Mirrors the real-world banking model: payee + amount +
 * frequency + FIRST PAYMENT DATE (the recurrence anchor — weekly repeats on that weekday,
 * monthly on that day, yearly on that day+month) + optional end date. Submits the real
 * OBP create endpoint with `date_starts`/`date_expires` carrying the schedule.
 */
class CreateStandingOrderViewModel(
    private val standingOrdersRepository: StandingOrdersRepository,
    private val accountsRepository: AccountsRepository,
    private val paymentsRepository: PaymentsRepository,
    private val profileRepository: ProfileRepository,
    private val customersRepository: CustomersRepository,
    /** Account chosen on the list screen; falls back to checking-first when blank. */
    private val initialAccountId: String = "",
    @OptIn(ExperimentalTime::class)
    private val todayProvider: () -> LocalDate = { Clock.System.todayIn(TimeZone.currentSystemDefault()) },
) : ViewModel() {

    private val formState = MutableStateFlow(CreateStandingOrderForm())
    val form: StateFlow<CreateStandingOrderForm> = formState.asStateFlow()

    private var account: Account? = null

    init {
        loadAccount()
    }

    fun onPayeeSelected(counterpartyId: String) =
        formState.update { it.copy(selectedPayeeId = counterpartyId, error = null) }

    fun onAmountChanged(amount: String) = formState.update { it.copy(amount = amount, error = null) }

    fun onFrequencySelected(frequency: String) =
        formState.update { it.copy(frequency = frequency, error = null) }

    fun onStartDateSelected(date: LocalDate) = formState.update { it.copy(startDate = date, error = null) }

    fun onEndDateSelected(date: LocalDate?) = formState.update { it.copy(endDate = date, error = null) }

    fun onSubmit() {
        val current = formState.value
        val acct = account
        val payee = current.payees.firstOrNull { it.counterpartyId == current.selectedPayeeId }
        val normalized = current.amount.trim().replace(',', '.')
        val validationError = validate(acct, payee, normalized, current)
        if (validationError != null) {
            formState.update { it.copy(error = validationError) }
            return
        }
        formState.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            buildRequest(acct!!, payee!!.counterpartyId, normalized, current)
                .mapCatching { request ->
                    standingOrdersRepository
                        .create(acct.bankId, acct.accountIdOrId, payee.name, request)
                        .getOrThrow()
                }
                .onSuccess { formState.update { it.copy(submitting = false, created = true) } }
                .onFailure { e ->
                    formState.update {
                        it.copy(submitting = false, error = e.message ?: "Could not create standing order")
                    }
                }
        }
    }

    private fun validate(
        acct: Account?,
        payee: Counterparty?,
        amount: String,
        current: CreateStandingOrderForm,
    ): String? = when {
        acct == null -> "No account available"
        payee == null -> "Choose a payee"
        (amount.toDoubleOrNull() ?: 0.0) <= 0.0 -> "Enter a valid amount"
        current.startDate == null -> "Choose a start date"
        current.startDate <= todayProvider() -> "Start date must be after today"
        current.endDate != null && current.endDate <= current.startDate ->
            "End date must be after the start date"
        else -> null
    }

    private suspend fun buildRequest(
        acct: Account,
        counterpartyId: String,
        amount: String,
        current: CreateStandingOrderForm,
    ): Result<CreateStandingOrderRequest> = runCatching {
        val userId = profileRepository.current().getOrThrow().userId
        val customerId = customersRepository.currentUserCustomers().getOrThrow()
            .firstOrNull { it.bankId == acct.bankId }
            ?.customerId
            ?: error("No customer record at ${acct.bankId}")
        CreateStandingOrderRequest(
            customerId = customerId,
            userId = userId,
            counterpartyId = counterpartyId,
            amount = AmountOfMoney(currency = acct.balance.currency.ifBlank { "EUR" }, amount = amount),
            `when` = StandingOrderSchedule(frequency = current.frequency),
            dateSigned = "${todayProvider()}T00:00:00Z",
            dateStarts = "${current.startDate}T00:00:00Z",
            dateExpires = current.endDate?.let { "${it}T00:00:00Z" },
        )
    }

    private fun loadAccount() {
        viewModelScope.launch {
            val accounts = accountsRepository.myAccounts().getOrDefault(emptyList())
            val target = accounts.firstOrNull { it.accountIdOrId == initialAccountId }
                ?: accounts.firstOrNull { it.typeOrProduct.contains("checking", ignoreCase = true) }
                ?: accounts.firstOrNull()
            account = target
            if (target == null) {
                formState.update { it.copy(loadingPayees = false, error = "No account available") }
                return@launch
            }
            formState.update {
                it.copy(
                    selectedAccountId = target.accountIdOrId,
                    sourceAccountName = accountDisplayName(target),
                    currency = target.balance.currency.ifBlank { "EUR" },
                )
            }
            loadPayeesFor(target)
        }
    }

    private suspend fun loadPayeesFor(target: Account) {
        val payees = paymentsRepository
            .listBeneficiaries(target.bankId, target.accountIdOrId)
            .getOrDefault(emptyList())
            .filter { it.isBeneficiary }
        formState.update { it.copy(loadingPayees = false, payees = payees) }
    }
}

/** Form state for the New Standing Order screen. */
@Immutable
data class CreateStandingOrderForm(
    val selectedAccountId: String = "",
    /** Display name of the source account (shown in the screen subtitle). */
    val sourceAccountName: String = "",
    val loadingPayees: Boolean = true,
    val payees: List<Counterparty> = emptyList(),
    val selectedPayeeId: String = "",
    val amount: String = "",
    val currency: String = "EUR",
    val frequency: String = "MONTHLY",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val submitting: Boolean = false,
    val error: String? = null,
    /** Set after a successful POST — the screen navigates back when it flips. */
    val created: Boolean = false,
)

/** Human recurrence hint derived from the first payment date, e.g. "Repeats every Monday". */
internal fun recurrenceHint(frequency: String, startDate: LocalDate?): String {
    if (startDate == null) return ""
    return when (frequency.uppercase()) {
        "DAILY" -> "Repeats every day"
        "WEEKLY" -> "Repeats every ${weekdayName(startDate)}"
        "BI-WEEKLY" -> "Repeats every other ${weekdayName(startDate)}"
        "YEARLY" -> "Repeats every ${startDate.day} ${monthName(startDate)}"
        else -> "Repeats on the ${ordinal(startDate.day)} of each month"
    }
}

private fun weekdayName(date: LocalDate): String =
    date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }

private fun monthName(date: LocalDate): String =
    date.month.name.lowercase().replaceFirstChar { it.uppercase() }

private fun ordinal(day: Int): String {
    val suffix = when {
        day % 100 in 11..13 -> "th"
        day % 10 == 1 -> "st"
        day % 10 == 2 -> "nd"
        day % 10 == 3 -> "rd"
        else -> "th"
    }
    return "$day$suffix"
}
