/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accountdetail

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.mifosx.openbanking.core.data.banking.AccountDetailRepository
import org.mifosx.openbanking.core.model.banking.AccountBalanceLine
import org.mifosx.openbanking.core.model.banking.AccountDetail
import org.mifosx.openbanking.core.model.banking.AccountDetailWithBalances
import template.core.base.common.screen.ScreenState
import template.core.base.store.screen.ScreenDataStream

/**
 * In-memory [AccountDetailRepository] for view-model tests.
 *
 * The view model merges two streams, so [emit] fans a combined [ScreenState] out to both: a
 * `Content` is split into its detail and balance halves, and every other state is pushed to both
 * unchanged. [observedAccountId] and [refreshCount] record what the view model asked for.
 */
class FakeAccountDetailRepository : AccountDetailRepository {

    private val detailStates = MutableStateFlow<ScreenState<AccountDetail>>(ScreenState.Loading)
    private val balanceStates =
        MutableStateFlow<ScreenState<List<AccountBalanceLine>>>(ScreenState.Loading)

    private val detailRefreshes = MutableSharedFlow<Unit>(replay = REFRESH_REPLAY)
    private val balanceRefreshes = MutableSharedFlow<Unit>(replay = REFRESH_REPLAY)

    /** The account id the view model passed in — the route argument, verbatim. */
    var observedAccountId: String? = null
        private set

    /** How many streams the view model opened; two per subscription. */
    var stateCallCount: Int = 0
        private set

    /** Refreshes seen on the detail stream — the Retry action's observable effect. */
    val refreshCount: Int get() = detailRefreshes.replayCache.size

    /** Pushes a combined state, fanning it out to both underlying streams. */
    fun emit(state: ScreenState<AccountDetailWithBalances>) {
        when (state) {
            is ScreenState.Content -> {
                detailStates.value = ScreenState.Content(
                    data = state.data.detail,
                    freshness = state.freshness,
                    fetchedAt = state.fetchedAt,
                )
                balanceStates.value = ScreenState.Content(
                    data = state.data.balances,
                    freshness = state.freshness,
                    fetchedAt = state.fetchedAt,
                )
            }

            else -> {
                @Suppress("UNCHECKED_CAST")
                detailStates.value = state as ScreenState<AccountDetail>

                @Suppress("UNCHECKED_CAST")
                balanceStates.value = state as ScreenState<List<AccountBalanceLine>>
            }
        }
    }

    override fun detailStream(accountId: String, scope: CoroutineScope): ScreenDataStream<AccountDetail> {
        observedAccountId = accountId
        stateCallCount++
        return ScreenDataStream(state = detailStates, refreshTrigger = detailRefreshes)
    }

    override fun balanceLinesStream(
        accountId: String,
        scope: CoroutineScope,
    ): ScreenDataStream<List<AccountBalanceLine>> {
        observedAccountId = accountId
        return ScreenDataStream(state = balanceStates, refreshTrigger = balanceRefreshes)
    }

    private companion object {
        const val REFRESH_REPLAY = 16
    }
}
