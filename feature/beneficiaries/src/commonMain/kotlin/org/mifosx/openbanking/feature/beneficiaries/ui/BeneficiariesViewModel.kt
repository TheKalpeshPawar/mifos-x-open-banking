/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.beneficiaries.ui

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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import org.mifosx.openbanking.core.data.accounts.AccountsRepository
import org.mifosx.openbanking.core.data.banks.BanksRepository
import org.mifosx.openbanking.core.data.payments.PaymentsRepository
import org.mifosx.openbanking.core.data.transactions.TransactionsRepository
import org.mifosx.openbanking.core.model.obp.Counterparty
import org.mifosx.openbanking.core.model.obp.CreateCounterpartyRequest
import org.mifosx.openbanking.core.model.obp.Transaction
import template.core.base.store.screen.DataFreshness
import template.core.base.store.screen.ScreenState
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Sort order for the full beneficiaries list. */
enum class SortOrder { AlphaAscending, AlphaDescending }

/**
 * Beneficiaries ViewModel. Resolves the user's primary account, then joins three OBP
 * sources into a single screen model:
 *  - counterparties (the beneficiary list) via [PaymentsRepository]
 *  - bank display names via [BanksRepository] (OBP-scheme routing only; else the code)
 *  - last-payment amount/date + recency via [TransactionsRepository], joined on the
 *    transaction description matching the counterparty name.
 *
 * Search + sort are applied client-side over the loaded rows, so neither re-hits the
 * network. `Empty` means the account has no beneficiaries at all; a search that matches
 * nothing stays `Content` (search bar operable) with an empty `all` list.
 */
class BeneficiariesViewModel(
    private val paymentsRepository: PaymentsRepository,
    private val banksRepository: BanksRepository,
    private val transactionsRepository: TransactionsRepository,
    private val accountsRepository: AccountsRepository,
) : ViewModel() {

    private val rawState = MutableStateFlow<RawState>(RawState.Loading)
    private val queryFlow = MutableStateFlow("")
    private val sortFlow = MutableStateFlow(SortOrder.AlphaAscending)

    val searchQuery: StateFlow<String> = queryFlow.asStateFlow()
    val sortOrder: StateFlow<SortOrder> = sortFlow.asStateFlow()

    private var currentAccountId: String? = null
    private var currentCurrency: String = "GBP"

    val uiState: StateFlow<ScreenState<BeneficiariesContent>> =
        combine(rawState, queryFlow, sortFlow) { raw, query, sort ->
            when (raw) {
                is RawState.Loading -> ScreenState.Loading
                is RawState.Failed -> ScreenState.Error(raw.error)
                is RawState.Loaded -> projectContent(raw.rows, query, sort)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ScreenState.Loading,
        )

    init {
        load()
    }

    fun onSearchQueryChanged(query: String) = queryFlow.update { query }

    fun onSortChanged(order: SortOrder) = sortFlow.update { order }

    fun toggleSort() = sortFlow.update {
        if (it == SortOrder.AlphaAscending) SortOrder.AlphaDescending else SortOrder.AlphaAscending
    }

    fun onRetry() = load()

    fun onRefresh() = load()

    /** Add a beneficiary, then reload on success. [onResult] reports success/failure to the UI. */
    fun addBeneficiary(
        name: String,
        iban: String,
        bankCode: String,
        onResult: (Boolean) -> Unit,
    ) {
        val accountId = currentAccountId
        if (accountId == null || name.isBlank() || iban.isBlank()) {
            onResult(false)
            return
        }
        val request = CreateCounterpartyRequest(
            name = name.trim(),
            description = "",
            currency = currentCurrency,
            otherAccountRoutingScheme = "IBAN",
            otherAccountRoutingAddress = iban.trim().replace(" ", ""),
            otherBankRoutingScheme = if (bankCode.isBlank()) "OBP" else "BIC",
            otherBankRoutingAddress = bankCode.trim().ifBlank { "" },
            isBeneficiary = true,
        )
        viewModelScope.launch {
            paymentsRepository.createBeneficiary(accountId, request).fold(
                onSuccess = {
                    onResult(true)
                    load()
                },
                onFailure = { onResult(false) },
            )
        }
    }

    private fun load() {
        rawState.value = RawState.Loading
        viewModelScope.launch {
            val accounts = accountsRepository.myAccounts().getOrElse {
                rawState.value = RawState.Failed(it)
                return@launch
            }
            val primary = accounts.firstOrNull()
            if (primary == null) {
                currentAccountId = null
                rawState.value = RawState.Loaded(emptyList())
                return@launch
            }
            val accountId = primary.accountIdOrId
            currentAccountId = accountId
            currentCurrency = primary.balance.currency.ifBlank { "GBP" }

            val beneficiaries = paymentsRepository.listBeneficiaries(accountId).getOrElse {
                rawState.value = RawState.Failed(it)
                return@launch
            }.filter { it.isBeneficiary }
            // Last-payment + recency are an enhancement — a transactions failure must not
            // fail the whole screen, so fall back to an empty list.
            val transactions = transactionsRepository
                .listTransactions(primary.bankId, accountId, limit = null)
                .getOrElse { emptyList() }

            rawState.value = RawState.Loaded(buildRows(beneficiaries, transactions))
        }
    }

    private suspend fun buildRows(
        beneficiaries: List<Counterparty>,
        transactions: List<Transaction>,
    ): List<BeneficiaryRow> {
        val latestByName = latestTransactionByPayeeName(transactions)
        return beneficiaries.map { cp ->
            val bankName = if (cp.otherBankRoutingScheme.equals("OBP", ignoreCase = true)) {
                banksRepository.bankName(cp.otherBankRoutingAddress)
            } else {
                cp.otherBankRoutingAddress
            }
            val txn = latestByName[cp.name.lowercase()]
            BeneficiaryRow(
                id = cp.counterpartyId,
                name = cp.name,
                bankName = bankName,
                accountIdentifier = maskIdentifier(cp.otherAccountRoutingAddress),
                lastPayment = txn?.let {
                    LastPayment(
                        amount = formatMoney(it.details.value.amount, it.details.value.currency),
                        date = formatDate(it.details.posted),
                        relativeDate = formatRelative(it.details.posted),
                        recencyKey = it.details.posted,
                    )
                },
            )
        }
    }

    private fun projectContent(
        rows: List<BeneficiaryRow>,
        query: String,
        sort: SortOrder,
    ): ScreenState<BeneficiariesContent> {
        if (rows.isEmpty()) return ScreenState.Empty
        val q = query.trim()
        val filtered = if (q.isEmpty()) rows else rows.filter { it.matches(q) }
        val sorted = when (sort) {
            SortOrder.AlphaAscending -> filtered.sortedBy { it.name.lowercase() }
            SortOrder.AlphaDescending -> filtered.sortedByDescending { it.name.lowercase() }
        }
        val recentlyUsed = filtered
            .filter { it.lastPayment != null }
            .sortedByDescending { it.lastPayment?.recencyKey.orEmpty() }
            .take(RECENT_LIMIT)
        return ScreenState.Content(
            data = BeneficiariesContent(
                recentlyUsed = recentlyUsed,
                all = sorted,
                query = query,
                sortOrder = sort,
                totalCount = rows.size,
            ),
            freshness = DataFreshness.FRESH,
        )
    }

    private fun latestTransactionByPayeeName(transactions: List<Transaction>): Map<String, Transaction> {
        val map = mutableMapOf<String, Transaction>()
        for (txn in transactions) {
            val key = txn.details.description.lowercase()
            if (key.isBlank()) continue
            val existing = map[key]
            if (existing == null || txn.details.posted > existing.details.posted) {
                map[key] = txn
            }
        }
        return map
    }

    private sealed interface RawState {
        data object Loading : RawState
        data class Failed(val error: Throwable) : RawState
        data class Loaded(val rows: List<BeneficiaryRow>) : RawState
    }

    private companion object {
        const val RECENT_LIMIT = 3
    }
}

/** A presentation row for one beneficiary, with bank name + last-payment metadata resolved. */
@Immutable
data class BeneficiaryRow(
    val id: String,
    val name: String,
    val bankName: String,
    val accountIdentifier: String,
    val lastPayment: LastPayment?,
) {
    val initials: String
        get() = name.split(' ', '\t')
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercaseChar() }
            .joinToString("")
            .ifBlank { "?" }

    fun matches(query: String): Boolean =
        name.contains(query, ignoreCase = true) ||
            accountIdentifier.contains(query, ignoreCase = true) ||
            bankName.contains(query, ignoreCase = true)
}

/** Last-payment summary derived from transactions. */
@Immutable
data class LastPayment(
    val amount: String,
    val date: String,
    val relativeDate: String,
    val recencyKey: String,
)

/** Loaded content for the Beneficiaries screen. */
@Immutable
data class BeneficiariesContent(
    val recentlyUsed: List<BeneficiaryRow>,
    val all: List<BeneficiaryRow>,
    val query: String,
    val sortOrder: SortOrder,
    val totalCount: Int,
)

/** Masks a long account identifier/IBAN: "GB29NWBK60161331926819" -> "GB29 ··· 6819". */
internal fun maskIdentifier(identifier: String): String {
    val clean = identifier.trim()
    if (clean.length <= 8) return clean
    return "${clean.take(4)} ··· ${clean.takeLast(4)}"
}

/** Formats an OBP money value ("-5.40", "EUR") to a display string ("€5.40"); abs value. */
internal fun formatMoney(amount: String, currency: String): String {
    val value = amount.toDoubleOrNull() ?: return "$currency $amount".trim()
    val abs = if (value < 0) -value else value
    val cents = kotlin.math.round(abs * 100).toLong()
    val whole = cents / 100
    val frac = (cents % 100).toString().padStart(2, '0')
    val symbol = when (currency.uppercase()) {
        "GBP" -> "£"
        "EUR" -> "€"
        "USD" -> "$"
        else -> if (currency.isBlank()) "" else "$currency "
    }
    return "$symbol$whole.$frac"
}

/** Relative date for recent rows ("today" / "yesterday" / "N days ago" / "N weeks ago"); else absolute. */
@OptIn(ExperimentalTime::class)
internal fun formatRelative(iso: String): String {
    val date = runCatching { LocalDate.parse(iso.substringBefore('T')) }.getOrNull()
        ?: return formatDate(iso)
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val days = date.daysUntil(today)
    return when {
        days <= 0 -> "today"
        days == 1 -> "yesterday"
        days < 7 -> "$days days ago"
        days < 14 -> "1 week ago"
        days < 30 -> "${days / 7} weeks ago"
        else -> formatDate(iso)
    }
}

/** Formats an ISO-8601 timestamp ("2026-06-01T15:16:10Z") to "1 Jun 2026"; blank-safe. */
internal fun formatDate(iso: String): String {
    val datePart = iso.substringBefore('T').trim()
    val parts = datePart.split('-')
    val months = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    )
    val formatted = parts.takeIf { it.size == 3 }?.let { p ->
        val day = p[2].toIntOrNull()
        val name = p[1].toIntOrNull()?.let { months.getOrNull(it - 1) }
        if (day != null && name != null) "$day $name ${p[0]}" else null
    }
    return formatted ?: datePart
}
