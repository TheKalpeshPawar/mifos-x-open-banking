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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mifosx.openbanking.core.data.accounts.AccountsRepository
import org.mifosx.openbanking.core.data.payments.PaymentsRepository
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.core.model.obp.Counterparty
import template.core.base.store.screen.DataFreshness
import template.core.base.store.screen.ScreenState

enum class PaymentType { SEPA, DOMESTIC, INTERNATIONAL }

/** Immutable payment draft handed to the confirm screen. */
@Immutable
data class PaymentDraft(
    val fromBankId: String,
    val fromAccountId: String,
    val fromLabel: String,
    val amount: String,
    val currency: String,
    val counterpartyId: String,
    val beneficiaryName: String,
    val beneficiaryBank: String,
    val iban: String,
    val reference: String,
)

/**
 * Send Money form ViewModel. Loads the user's accounts + the primary account's
 * beneficiaries, holds the form state, and validates on Continue (amount, beneficiary,
 * then a live funds-available check) before producing a [PaymentDraft] for the confirm
 * screen. SEPA requires the payment currency to equal the from-account currency, so the
 * currency tracks the selected account.
 */
class SendMoneyViewModel(
    private val accountsRepository: AccountsRepository,
    private val paymentsRepository: PaymentsRepository,
) : ViewModel() {

    private val rawState = MutableStateFlow<RawState>(RawState.Loading)
    private val form = MutableStateFlow(FormState())

    val uiState: StateFlow<ScreenState<SendMoneyContent>> =
        combine(rawState, form) { raw, f -> project(raw, f) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ScreenState.Loading,
            )

    private var accounts: List<Account> = emptyList()
    private var requestedAccountId: String = ""

    init {
        load()
    }

    /**
     * Set the from-account (passed from the hub). Loads THAT account's beneficiaries and tracks its
     * currency. Safe to call before [load] finishes — the request is remembered and applied once the
     * account list arrives.
     */
    fun setAccount(accountId: String) {
        if (accountId.isBlank() || accountId == form.value.accountId) return
        requestedAccountId = accountId
        val account = accounts.firstOrNull { it.accountIdOrId == accountId } ?: return
        rawState.value = RawState.Loading
        viewModelScope.launch { loadBeneficiaries(account) }
    }

    fun onAmountChanged(amount: String) = form.update {
        it.copy(amount = amount.filter { c -> c.isDigit() || c == '.' }, amountError = null)
    }

    fun onBeneficiaryQueryChanged(query: String) = form.update { it.copy(query = query) }

    fun onBeneficiarySelected(id: String) = form.update { it.copy(beneficiaryId = id, beneficiaryError = null) }

    fun onReferenceChanged(reference: String) = form.update { it.copy(reference = reference.take(MAX_REFERENCE)) }

    fun onPaymentTypeChanged(type: PaymentType) = form.update { it.copy(paymentType = type) }

    fun onRetry() = load()

    /** Validate + live funds-check; on success emit the draft via [onReady]. */
    fun onContinue(onReady: (PaymentDraft) -> Unit) {
        val loaded = rawState.value as? RawState.Loaded ?: return
        val f = form.value
        // A COUNTERPARTY transfer is only valid from the account that owns the payee (OBP-30017
        // otherwise), so the payment sends from the account selected on the hub.
        val account = loaded.accounts.firstOrNull { it.accountIdOrId == f.accountId }
            ?: loaded.accounts.firstOrNull()
        val beneficiary = loaded.beneficiaries.firstOrNull { it.counterpartyId == f.beneficiaryId }
        val amountValue = f.amount.toDoubleOrNull() ?: 0.0

        val amountError = if (amountValue <= 0.0) "Please enter a valid amount greater than 0" else null
        val beneficiaryError = if (beneficiary == null) "Please select a valid beneficiary" else null
        if (account == null || beneficiary == null || amountError != null) {
            form.update { it.copy(amountError = amountError, beneficiaryError = beneficiaryError) }
            return
        }

        // Live funds gate: block sends from an account without sufficient available funds
        // (e.g. an overdrawn account) before reaching the confirm screen.
        form.update { it.copy(submitting = true, formError = null) }
        viewModelScope.launch {
            val hasFunds = paymentsRepository
                .fundsAvailable(account.bankId, account.accountIdOrId, f.amount, f.currency)
                .getOrDefault(false)
            form.update { it.copy(submitting = false) }
            if (!hasFunds) {
                form.update { it.copy(formError = "Insufficient funds in your account.") }
                return@launch
            }
            onReady(
                PaymentDraft(
                    fromBankId = account.bankId,
                    fromAccountId = account.accountIdOrId,
                    fromLabel = account.label.ifBlank { account.accountIdOrId },
                    amount = f.amount,
                    currency = f.currency,
                    counterpartyId = beneficiary.counterpartyId,
                    beneficiaryName = beneficiary.name,
                    beneficiaryBank = beneficiary.otherBankRoutingAddress,
                    iban = resolveIban(beneficiary),
                    reference = f.reference,
                ),
            )
        }
    }

    private fun load() {
        rawState.value = RawState.Loading
        viewModelScope.launch {
            accounts = accountsRepository.myAccounts().getOrElse {
                rawState.value = RawState.Failed(it)
                return@launch
            }
            if (accounts.isEmpty()) {
                rawState.value = RawState.Loaded(emptyList(), emptyList())
                return@launch
            }
            // Honour an account preselected from the hub; otherwise default to the first account.
            val target = accounts.firstOrNull { it.accountIdOrId == requestedAccountId } ?: accounts.first()
            loadBeneficiaries(target)
        }
    }

    private suspend fun loadBeneficiaries(account: Account) {
        val beneficiaries = paymentsRepository.listBeneficiaries(account.bankId, account.accountIdOrId)
            .getOrElse { emptyList() }
            .filter { it.isBeneficiary }
        // /my/accounts omits balance+currency, so account.balance.currency is blank here. The
        // funds-available check and the payment both require the from-account's real currency
        // (currency mismatch -> OBP-40003 / a false "insufficient funds"), so resolve it from the
        // full account detail; fall back to any currency already known, else EUR.
        val resolvedCurrency = accountsRepository.accountDetail(account.bankId, account.accountIdOrId)
            .getOrNull()?.balance?.currency?.takeIf { it.isNotBlank() }
            ?: account.balance.currency.ifBlank { "EUR" }
        form.update {
            // Keep a preselected payee if it still belongs to this account (the two screen-side
            // LaunchedEffects — setAccount + onBeneficiarySelected — can fire in either order).
            val keep = beneficiaries.any { b -> b.counterpartyId == it.beneficiaryId }
            it.copy(
                accountId = account.accountIdOrId,
                currency = resolvedCurrency,
                beneficiaryId = if (keep) it.beneficiaryId else "",
                beneficiaryError = if (keep) it.beneficiaryError else null,
            )
        }
        rawState.value = RawState.Loaded(accounts, beneficiaries)
    }

    private fun project(raw: RawState, f: FormState): ScreenState<SendMoneyContent> = when (raw) {
        is RawState.Loading -> ScreenState.Loading
        is RawState.Failed -> ScreenState.Error(raw.error)
        is RawState.Loaded -> {
            if (raw.accounts.isEmpty()) {
                ScreenState.Empty
            } else {
                // From-account = the account selected on the hub (owns the shown payees).
                val selectedAccount = raw.accounts.firstOrNull { it.accountIdOrId == f.accountId }
                    ?: raw.accounts.first()
                val q = f.query.trim()
                val filtered = if (q.isEmpty()) {
                    raw.beneficiaries
                } else {
                    raw.beneficiaries.filter {
                        it.name.contains(q, true) || it.otherAccountRoutingAddress.contains(q, true)
                    }
                }
                ScreenState.Content(
                    data = SendMoneyContent(
                        accounts = raw.accounts,
                        selectedAccount = selectedAccount,
                        beneficiaries = filtered,
                        recentBeneficiaries = raw.beneficiaries.take(RECENT_LIMIT),
                        selectedBeneficiary = raw.beneficiaries.firstOrNull { it.counterpartyId == f.beneficiaryId },
                        amount = f.amount,
                        currency = f.currency,
                        reference = f.reference,
                        query = f.query,
                        paymentType = f.paymentType,
                        amountError = f.amountError,
                        beneficiaryError = f.beneficiaryError,
                        formError = f.formError,
                        submitting = f.submitting,
                    ),
                    freshness = DataFreshness.FRESH,
                )
            }
        }
    }

    private fun resolveIban(cp: Counterparty): String = when {
        cp.otherAccountRoutingScheme.equals("IBAN", true) -> cp.otherAccountRoutingAddress
        cp.otherAccountSecondaryRoutingScheme.equals("IBAN", true) -> cp.otherAccountSecondaryRoutingAddress
        else -> cp.otherAccountRoutingAddress
    }

    private data class FormState(
        val accountId: String = "",
        val amount: String = "",
        val currency: String = "EUR",
        val beneficiaryId: String = "",
        val reference: String = "",
        val query: String = "",
        val paymentType: PaymentType = PaymentType.SEPA,
        val amountError: String? = null,
        val beneficiaryError: String? = null,
        val formError: String? = null,
        val submitting: Boolean = false,
    )

    private sealed interface RawState {
        data object Loading : RawState
        data class Failed(val error: Throwable) : RawState
        data class Loaded(val accounts: List<Account>, val beneficiaries: List<Counterparty>) : RawState
    }

    private companion object {
        const val MAX_REFERENCE = 35
        const val RECENT_LIMIT = 5
    }
}

/** Loaded content for the Send Money form. */
@Immutable
data class SendMoneyContent(
    val accounts: List<Account>,
    val selectedAccount: Account,
    val beneficiaries: List<Counterparty>,
    val recentBeneficiaries: List<Counterparty>,
    val selectedBeneficiary: Counterparty?,
    val amount: String,
    val currency: String,
    val reference: String,
    val query: String,
    val paymentType: PaymentType,
    val amountError: String?,
    val beneficiaryError: String?,
    val formError: String?,
    val submitting: Boolean,
) {
    val continueEnabled: Boolean
        get() = !submitting && amount.toDoubleOrNull()?.let { it > 0.0 } == true && selectedBeneficiary != null
}
