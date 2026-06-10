/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.accounts.ui

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
import org.mifosx.openbanking.core.data.customers.CustomersRepository
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.core.model.obp.AccountAttribute
import template.core.base.store.screen.DataFreshness
import template.core.base.store.screen.ScreenState

/**
 * Account-detail ViewModel. Loads the full OBP account (balance, number, routings, owners,
 * product attributes — the account-list omits these) for a specific (bankId, accountId) and
 * resolves the owning bank's display name. Projects only fields OBP actually returns; no
 * transactions. [load] is driven from the screen with the route's bankId/accountId.
 */
class AccountDetailViewModel(
    private val accountsRepository: AccountsRepository,
    private val banksRepository: BanksRepository,
    private val customersRepository: CustomersRepository,
) : ViewModel() {

    private val rawState = MutableStateFlow<ScreenState<AccountDetailContent>>(ScreenState.Loading)
    val uiState: StateFlow<ScreenState<AccountDetailContent>> = rawState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ScreenState.Loading,
    )

    private var bankId: String = ""
    private var accountId: String = ""

    /** Load the account at (bankId, accountId). Idempotent per id; re-loads when the id changes. */
    fun load(bankId: String, accountId: String) {
        if (accountId.isBlank()) return
        this.bankId = bankId
        this.accountId = accountId
        rawState.value = ScreenState.Loading
        viewModelScope.launch {
            val account = accountsRepository.accountDetail(bankId, accountId).getOrElse {
                rawState.value = ScreenState.Error(it)
                return@launch
            }
            val bankName = banksRepository.bankName(account.bankId)
            val customerName = customersRepository.accountHolderName(bankId, accountId).getOrNull().orEmpty()
            val holder = customerName.ifBlank { account.owners.firstOrNull()?.displayName.orEmpty() }
            rawState.value = ScreenState.Content(
                data = project(account, bankName, holder),
                freshness = DataFreshness.FRESH,
            )
        }
    }

    fun onRetry() = load(bankId, accountId)

    private fun project(account: Account, bankName: String, holder: String): AccountDetailContent {
        val currency = account.balance.currency
        return AccountDetailContent(
            accountLabel = account.label.ifBlank { account.accountIdOrId },
            balanceDisplay = formatMoney(account.balance.amount, currency),
            currency = currency,
            holder = holder,
            accountNumber = account.number,
            ibanRaw = account.ibanOrEmpty,
            ibanDisplay = groupIban(account.ibanOrEmpty),
            bankName = bankName,
            productLabel = humanizeCode(account.typeOrProduct),
            attributes = account.accountAttributes.map { attr -> toAttributeRow(attr, currency) },
        )
    }

    private fun toAttributeRow(attr: AccountAttribute, currency: String): AttributeRow {
        val value = if (attr.type.equals("DOUBLE", ignoreCase = true) && attr.value.toDoubleOrNull() != null) {
            formatMoney(attr.value, currency)
        } else {
            attr.value
        }
        return AttributeRow(label = humanizeCode(attr.name), value = value)
    }
}

/** Loaded content for the account-detail screen — every field maps to a real OBP account field. */
@Immutable
data class AccountDetailContent(
    val accountLabel: String,
    val balanceDisplay: String,
    val currency: String,
    val holder: String,
    val accountNumber: String,
    val ibanRaw: String,
    val ibanDisplay: String,
    val bankName: String,
    val productLabel: String,
    val attributes: List<AttributeRow>,
) {
    /** Plan & fees card shows only when there is a product and/or at least one attribute. */
    val hasPlanSection: Boolean get() = productLabel.isNotBlank() || attributes.isNotEmpty()
}

@Immutable
data class AttributeRow(val label: String, val value: String)

/** Symbol for common currencies; falls back to "<code> " so the amount stays labelled. */
internal fun currencySymbol(currency: String): String = when (currency.uppercase()) {
    "GBP" -> "£"
    "EUR" -> "€"
    "USD" -> "$"
    else -> if (currency.isBlank()) "" else "$currency "
}

/** Formats an OBP money string ("-3970.29", "GBP") to "-£3,970.29"; sign-preserving, grouped. */
internal fun formatMoney(amount: String, currency: String): String {
    val value = amount.toDoubleOrNull() ?: return "${currencySymbol(currency)}$amount".trim()
    val negative = value < 0
    val cents = kotlin.math.round(kotlin.math.abs(value) * 100).toLong()
    val whole = cents / 100
    val frac = (cents % 100).toString().padStart(2, '0')
    val grouped = whole.toString().reversed().chunked(3).joinToString(",").reversed()
    return "${if (negative) "-" else ""}${currencySymbol(currency)}$grouped.$frac"
}

/** Groups an IBAN into 4-char blocks for display ("GB29MFOS…" -> "GB29 MFOS …"); blank-safe. */
internal fun groupIban(iban: String): String =
    iban.trim().replace(" ", "").chunked(4).joinToString(" ")

/** Acronym/currency tokens kept upper-case when humanising codes (FEE is a word, GBP is a code). */
private val UPPER_TOKENS = setOf(
    "GBP", "EUR", "USD", "CHF", "JPY", "CAD", "AUD", "BWP", "SGD", "INR",
    "CCY", "IBAN", "BIC", "SWIFT", "FX", "ATM", "API", "ID", "GSOC",
)

/**
 * Humanises an OBP code ("BUSINESS-CURRENT-GBP", "MONTHLY_FEE") to a readable label
 * ("Business Current GBP", "Monthly Fee"). Known acronym/currency tokens stay upper-case;
 * every other token is title-cased.
 */
internal fun humanizeCode(code: String): String =
    code.split('-', '_', ' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { token ->
            if (token.uppercase() in UPPER_TOKENS) {
                token.uppercase()
            } else {
                token.lowercase().replaceFirstChar { it.uppercaseChar() }
            }
        }
