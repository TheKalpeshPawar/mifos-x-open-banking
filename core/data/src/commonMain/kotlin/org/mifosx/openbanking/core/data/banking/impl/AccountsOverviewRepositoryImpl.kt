/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking.impl

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import org.mifosx.openbanking.core.data.banking.AccountsOverviewRepository
import org.mifosx.openbanking.core.data.banking.store.BankingStores
import org.mifosx.openbanking.core.data.banking.store.getOnce
import org.mifosx.openbanking.core.data.infra.NetworkMonitor
import org.mifosx.openbanking.core.model.banking.AccountBalance
import org.mifosx.openbanking.core.model.banking.AccountWithBalance
import org.mifosx.openbanking.core.model.banking.BankAccount
import org.mobilenativefoundation.store.store5.Store
import template.core.base.common.screen.ScreenState
import template.core.base.store.infra.FetchedAtRepository
import template.core.base.store.screen.ScreenDataStream
import template.core.base.store.screen.asScreenStream

/**
 * Enriches the accounts stream with per-account balances.
 *
 * The accounts stream (network + Room SoT) drives the screen state; each of its [ScreenState.Content]
 * lists is fanned out concurrently to resolve balances, then re-wrapped as [ScreenState.Content]. All
 * other states pass through untouched, and an empty account set collapses to [ScreenState.Empty].
 *
 * Reuses the two existing qualified stores rather than owning any new store: the accounts store keyed
 * by [BankingStores.ACCOUNTS_KEY] and the memory-only balances store keyed by account id.
 *
 * **Refresh-flag design:** the balances store is memory-only, so a plain cached read
 * ([getOnce] with `refresh = false`) would keep returning the first-fetched figure and a pull-to-
 * refresh would show stale balances. A single [refreshBalances] flag, set by [refresh] and consumed
 * on the next fan-out, makes exactly the next balance resolution hit the network (`refresh = true`),
 * then reverts to cache-first reads. The flag is a plain `var` — not synchronised — because the
 * stream is collected on one UI scope, matching the plain `var stream` pattern in the sibling
 * repositories.
 */
internal class AccountsOverviewRepositoryImpl(
    private val accountsStore: Store<String, List<BankAccount>>,
    private val balancesStore: Store<String, AccountBalance>,
    private val networkMonitor: NetworkMonitor,
    private val fetchedAtRepository: FetchedAtRepository,
) : AccountsOverviewRepository {

    private var accountsStream: ScreenDataStream<List<BankAccount>>? = null
    private var refreshBalances = false

    private fun accountsStream(scope: CoroutineScope): ScreenDataStream<List<BankAccount>> =
        accountsStream ?: accountsStore.asScreenStream(
            key = BankingStores.ACCOUNTS_KEY,
            networkMonitor = networkMonitor,
            fetchedAtRepository = fetchedAtRepository,
            cacheKey = CACHE_KEY,
            scope = scope,
        ).also { accountsStream = it }

    override fun overviewState(scope: CoroutineScope): Flow<ScreenState<List<AccountWithBalance>>> =
        accountsStream(scope).state.transform { state ->
            if (state is ScreenState.Content) {
                if (state.data.isEmpty()) {
                    emit(ScreenState.Empty)
                } else {
                    val refresh = refreshBalances
                    refreshBalances = false
                    emit(ScreenState.Content(enrich(state.data, refresh), state.freshness, state.fetchedAt))
                }
            } else {
                emit(state.retype())
            }
        }

    /**
     * Re-labels a non-[ScreenState.Content] state to the overview's payload type. Safe because the
     * loading/error/no-network/unauthenticated states carry no data, so there is nothing to mis-cast.
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T> ScreenState<*>.retype(): ScreenState<T> = this as ScreenState<T>

    private suspend fun enrich(accounts: List<BankAccount>, refresh: Boolean): List<AccountWithBalance> =
        coroutineScope {
            accounts.map { account ->
                async {
                    AccountWithBalance(
                        account = account,
                        balance = balanceOrNull(account.accountId, refresh),
                    )
                }
            }.awaitAll()
        }

    /**
     * This account's balance, or null when the bank refuses or the read fails. Cancellation is not a
     * failure and propagates.
     */
    private suspend fun balanceOrNull(accountId: String, refresh: Boolean): AccountBalance? = try {
        balancesStore.getOnce(accountId, refresh)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        null
    }

    override fun refresh() {
        refreshBalances = true
        accountsStream?.refresh()
    }

    private companion object {
        const val CACHE_KEY = "accounts:overview"
    }
}
