/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.sendmoney.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.mifosx.openbanking.core.data.accounts.AccountsRepository
import org.mifosx.openbanking.core.data.banks.BanksRepository
import org.mifosx.openbanking.core.data.payments.PaymentsRepository
import org.mifosx.openbanking.core.datastore.UserPreferencesRepository
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.core.model.obp.Counterparty
import org.mifosx.openbanking.core.model.obp.TransactionRequestSummary
import template.core.base.store.screen.DataFreshness
import template.core.base.store.screen.ScreenState

/**
 * Send Money hub ViewModel. Loads the selected account's beneficiaries (counterparties),
 * resolves each one's bank display name, and derives last-payment recency from the account's
 * transaction-requests (which record the paid counterparty id / IBAN — OBP transactions do not
 * reliably identify the payee). Exposes a hub model: recent recipients (top-N by recency) + the
 * full beneficiary list. The amount + send happen on downstream screens.
 */
class SendMoneyHubViewModel(
    private val accountsRepository: AccountsRepository,
    private val paymentsRepository: PaymentsRepository,
    private val banksRepository: BanksRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val rawState = MutableStateFlow<ScreenState<SendMoneyHubContent>>(ScreenState.Loading)
    val uiState: StateFlow<ScreenState<SendMoneyHubContent>> = rawState.stateIn(
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

    /** Switch the from-account: reload that account's beneficiaries + recent payments. */
    fun onAccountSelected(accountId: String) {
        if (accountId == selectedAccountId) return
        val account = accounts.firstOrNull { it.accountIdOrId == accountId } ?: return
        selectedAccountId = accountId
        rawState.value = ScreenState.Loading
        viewModelScope.launch { loadForAccount(account) }
    }

    private fun load() {
        rawState.value = ScreenState.Loading
        viewModelScope.launch {
            accounts = accountsRepository.myAccounts().getOrElse {
                rawState.value = ScreenState.Error(it)
                return@launch
            }
            // Honour the persisted default account (set on the Home hero card) when present.
            val defaultId = userPreferencesRepository.userData.value.defaultAccountId
            val primary = accounts.firstOrNull { it.accountIdOrId == defaultId }
                ?: accounts.firstOrNull()
            if (primary == null) {
                rawState.value = ScreenState.Empty
                return@launch
            }
            selectedAccountId = primary.accountIdOrId
            loadForAccount(primary)
        }
    }

    private suspend fun loadForAccount(account: Account) {
        val accountId = account.accountIdOrId
        val beneficiaries = paymentsRepository.listBeneficiaries(account.bankId, accountId)
            .getOrElse {
                rawState.value = ScreenState.Error(it)
                return
            }
            .filter { it.isBeneficiary }
        val requests = paymentsRepository
            .listTransactionRequests(account.bankId, accountId)
            .getOrElse { emptyList() }
        val rows = buildRows(beneficiaries, requests)
        val recents = rows
            .filter { it.recencyKey.isNotBlank() }
            .sortedByDescending { it.recencyKey }
            .take(RECENT_LIMIT)
        // Empty payee list is NOT an Empty state — keep the account selector visible so the user
        // can switch to a funded/populated account.
        rawState.value = ScreenState.Content(
            data = SendMoneyHubContent(
                accounts = accounts,
                selectedAccountId = accountId,
                recentRecipients = recents,
                allBeneficiaries = rows,
            ),
            freshness = DataFreshness.FRESH,
        )
    }

    private suspend fun buildRows(
        beneficiaries: List<Counterparty>,
        requests: List<TransactionRequestSummary>,
    ): List<HubRecipient> {
        // Recency key per payee, keyed by counterparty id (COUNTERPARTY requests) and IBAN (SEPA).
        // OBP's start_date is date-only (no time component), so same-day payments would tie and a
        // fresh send couldn't out-rank an earlier same-day one. The list is returned in append
        // order, so we tie-break by request index: key = "<start_date>#<paddedIndex>" sorts by day
        // first, then by creation order — the most recent send ranks highest.
        val latestByCounterparty = mutableMapOf<String, String>()
        val latestByIban = mutableMapOf<String, String>()
        requests.forEachIndexed { index, req ->
            if (req.startDate.isBlank()) return@forEachIndexed
            val key = "${req.startDate}#${index.toString().padStart(RECENCY_INDEX_WIDTH, '0')}"
            req.details.toCounterparty?.counterpartyId?.takeIf { it.isNotBlank() }?.let { id ->
                if (key > latestByCounterparty[id].orEmpty()) latestByCounterparty[id] = key
            }
            req.details.toSepa?.iban?.takeIf { it.isNotBlank() }?.let { iban ->
                if (key > latestByIban[iban].orEmpty()) latestByIban[iban] = key
            }
        }
        return beneficiaries.map { cp ->
            val bankName = if (cp.otherBankRoutingScheme.equals("OBP", ignoreCase = true)) {
                banksRepository.bankName(cp.otherBankRoutingAddress)
            } else {
                cp.otherBankRoutingAddress
            }
            val recency = latestByCounterparty[cp.counterpartyId].orEmpty()
                .ifBlank { latestByIban[cp.otherAccountRoutingAddress].orEmpty() }
            HubRecipient(
                counterpartyId = cp.counterpartyId,
                name = cp.name,
                scheme = cp.otherAccountRoutingScheme.ifBlank { cp.otherBankRoutingScheme },
                bankName = bankName,
                last4 = cp.otherAccountRoutingAddress.takeLast(4),
                recencyKey = recency,
            )
        }
    }

    private companion object {
        const val RECENT_LIMIT = 4
        const val RECENCY_INDEX_WIDTH = 6
    }
}

/** A recipient row for the hub. */
@Immutable
data class HubRecipient(
    val counterpartyId: String,
    val name: String,
    val scheme: String,
    val bankName: String,
    val last4: String,
    val recencyKey: String,
) {
    val initials: String
        get() = name.split(' ', '\t')
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercaseChar() }
            .joinToString("")
            .ifBlank { "?" }

    /** "Liam W." style short caption for the recent-recipients row. */
    val shortName: String
        get() {
            val parts = name.split(' ').filter { it.isNotBlank() }
            return when {
                parts.isEmpty() -> name
                parts.size == 1 -> parts[0]
                else -> "${parts[0]} ${parts[1].first().uppercaseChar()}."
            }
        }

    /** "SEPA · Afternoon Coffee Bank ·· 8842" style subtitle. */
    val subtitle: String
        get() = listOf(scheme, bankName)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
            .let { if (last4.isNotBlank()) "$it ·· $last4" else it }
}

/** Loaded content for the Send Money hub. */
@Immutable
data class SendMoneyHubContent(
    val accounts: List<Account>,
    val selectedAccountId: String,
    val recentRecipients: List<HubRecipient>,
    val allBeneficiaries: List<HubRecipient>,
) {
    val selectedAccount: Account?
        get() = accounts.firstOrNull { it.accountIdOrId == selectedAccountId }
}
