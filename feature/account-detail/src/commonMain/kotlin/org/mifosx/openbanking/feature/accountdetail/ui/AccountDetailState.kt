/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accountdetail.ui

import org.mifosx.openbanking.core.data.util.RemoteException
import org.mifosx.openbanking.core.model.hsbcProduct.AccountEndpoint
import org.mifosx.openbanking.core.model.hsbcProduct.HsbcProductCapability
import org.mifosx.openbanking.core.model.hsbcProduct.HsbcProductType
import org.mifosx.openbanking.feature.accountdetail.AccountDetailChip
import template.core.base.network.NetworkError

/**
 * Screen state for the account-detail hub.
 *
 * @property accountId The route argument; also handed to every Explore chip as it navigates on.
 * @property uiState What the screen currently renders.
 * @property availableChips Which Explore chips this account can actually reach. Sits alongside
 *   [accountId] rather than inside [uiState] because both `Content` and `Empty` render the chip row.
 *
 *   The default is the **full** set, and that is load-bearing in two ways: an account whose product
 *   we cannot classify keeps every feature rather than losing them to a guess, and the UI suites
 *   that assert all nine chips render in declaration order keep passing without being rewritten.
 *   Do not "tidy" this to `emptySet()`.
 */
data class AccountDetailState(
    val accountId: String,
    val uiState: AccountDetailUiState = AccountDetailUiState.Loading,
    val availableChips: Set<AccountDetailChip> = AccountDetailChip.entries.toSet(),
)

/**
 * The four rendered states.
 *
 * [Empty] deliberately carries the header: an account the bank reports no balances for still shows
 * its identity and its navigation chips, so it is a content-bearing state rather than an absence
 * of data.
 */
sealed interface AccountDetailUiState {

    data object Loading : AccountDetailUiState

    data class Content(
        val header: AccountHeaderUi,
        val balances: List<BalanceRowUi>,
    ) : AccountDetailUiState

    data class Empty(val header: AccountHeaderUi) : AccountDetailUiState

    data class Error(
        val kind: AccountDetailErrorKind,
        val recoverable: Boolean,
    ) : AccountDetailUiState
}

/**
 * Display-ready account header. Every field is a finished string; the view model does the work.
 *
 * [accountNumber] is carried raw so the header and top bar can fall back to a "type ·· last 4" label
 * via `accountDisplayName` when the bank supplied no [nickname].
 */
data class AccountHeaderUi(
    val nickname: String,
    val accountSubType: String,
    val identificationLabel: String,
    val currency: String,
    val servicerIdentification: String,
    val lastUpdatedLabel: String,
    val accountNumber: String = "",
    /**
     * OBIE `Description` verbatim, e.g. `GLOBAL MONEY ACCOUNT`. Often the only place the bank says what
     * kind of product this is — `AccountTypeCode` reports `CACC` for a Global Money wallet just as it
     * does for an ordinary current account. Blank when the payload carried none, in which case the
     * description card is not drawn.
     */
    val description: String = "",
)

/** One typed balance row, e.g. `InterimAvailable` / `2,847.63 GBP`. */
data class BalanceRowUi(
    val type: String,
    val amountLabel: String,
)

/**
 * The five failure modes the screen distinguishes, each mapped to its own message.
 *
 * [recoverable] drives Retry visibility: a withdrawn consent or an account outside the authorised
 * set will not resolve by retrying, so those two offer back-navigation instead.
 */
enum class AccountDetailErrorKind(val recoverable: Boolean) {
    TokenExpired(recoverable = true),
    ConsentWithdrawn(recoverable = false),
    AccountNotFound(recoverable = false),
    Network(recoverable = true),
    Unexpected(recoverable = true),
}

/** Actions the view model owns. Navigation is handled by the screen's lambdas, not routed here. */
sealed interface AccountDetailAction {
    data object RetryLoad : AccountDetailAction
}

/**
 * Classifies a stream failure into one of the five [AccountDetailErrorKind]s.
 *
 * Store5 surfaces the repository's `NetworkError` wrapped in a [RemoteException]; anything else
 * (a mapper failure, an unexpected runtime error) falls through to [AccountDetailErrorKind.Unexpected].
 */
internal fun classifyAccountDetailError(throwable: Throwable): AccountDetailErrorKind =
    when ((throwable as? RemoteException)?.networkError) {
        is NetworkError.Client.Unauthorized -> AccountDetailErrorKind.TokenExpired
        is NetworkError.Client.Forbidden -> AccountDetailErrorKind.ConsentWithdrawn
        is NetworkError.Client.NotFound -> AccountDetailErrorKind.AccountNotFound
        is NetworkError.Network -> AccountDetailErrorKind.Network
        else -> AccountDetailErrorKind.Unexpected
    }

/**
 * The chips this account can reach, from HSBC's documented product matrix minus anything the bank
 * has since refused at runtime.
 *
 * Only Standing Orders, Direct Debits, Statements, Scheduled Payments and Beneficiaries are gated, and
 * that is stated explicitly rather than derived, so a chip added to [AccountDetailChip] later cannot
 * silently become hideable — a new destination has to opt in by adding its [AccountEndpoint] here.
 * Statements is credit-card-only per the matrix, so every non-credit-card product hides it; Scheduled
 * Payments and Beneficiaries are the mirror image — every product except a credit card can serve them,
 * so a credit card hides both.
 */
internal fun availableChipsFor(
    productType: HsbcProductType,
    unsupported: Set<AccountEndpoint>,
): Set<AccountDetailChip> {
    val gated = mapOf(
        AccountDetailChip.StandingOrders to AccountEndpoint.StandingOrders,
        AccountDetailChip.DirectDebits to AccountEndpoint.DirectDebits,
        AccountDetailChip.Statements to AccountEndpoint.Statements,
        AccountDetailChip.ScheduledPayments to AccountEndpoint.ScheduledPayments,
        AccountDetailChip.Beneficiaries to AccountEndpoint.Beneficiaries,
    )
    return AccountDetailChip.entries.filterTo(mutableSetOf()) { chip ->
        val endpoint = gated[chip] ?: return@filterTo true
        endpoint !in unsupported && HsbcProductCapability.supports(endpoint, productType)
    }
}
