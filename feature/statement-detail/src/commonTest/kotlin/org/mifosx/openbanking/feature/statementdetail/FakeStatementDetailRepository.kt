/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.statementdetail

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.mifosx.openbanking.core.data.banking.StatementDetailRepository
import org.mifosx.openbanking.core.model.banking.StatementDetail
import org.mifosx.openbanking.core.model.banking.TransactionItem
import template.core.base.common.screen.ScreenState
import template.core.base.store.screen.ScreenDataStream

/**
 * Hand-written [StatementDetailRepository] whose two streams the test drives directly.
 *
 * The view model merges a statement stream and a transactions stream, so the fake keeps a separate
 * state + refresh flow for each. [observedAccountId]/[observedStatementId] record the route arguments;
 * the two refresh counts are the Retry action's observable effect.
 */
class FakeStatementDetailRepository(
    initialStatement: ScreenState<StatementDetail> = ScreenState.Loading,
    initialTransactions: ScreenState<List<TransactionItem>> = ScreenState.Loading,
) : StatementDetailRepository {

    private val statementStates = MutableStateFlow(initialStatement)
    private val transactionStates = MutableStateFlow(initialTransactions)
    private val statementRefreshes = MutableSharedFlow<Unit>(replay = REFRESH_REPLAY)
    private val transactionRefreshes = MutableSharedFlow<Unit>(replay = REFRESH_REPLAY)

    var observedAccountId: String? = null
        private set
    var observedStatementId: String? = null
        private set

    val statementRefreshCount: Int get() = statementRefreshes.replayCache.size
    val transactionRefreshCount: Int get() = transactionRefreshes.replayCache.size

    fun emitStatement(state: ScreenState<StatementDetail>) {
        statementStates.value = state
    }

    fun emitTransactions(state: ScreenState<List<TransactionItem>>) {
        transactionStates.value = state
    }

    /** Pushes both streams to the same failure so the merged state is that error. */
    fun emitError(error: Throwable) {
        statementStates.value = ScreenState.Error(error)
        transactionStates.value = ScreenState.Error(error)
    }

    override fun statementStream(
        accountId: String,
        statementId: String,
        scope: CoroutineScope,
    ): ScreenDataStream<StatementDetail> {
        observedAccountId = accountId
        observedStatementId = statementId
        return ScreenDataStream(state = statementStates, refreshTrigger = statementRefreshes)
    }

    override fun statementTransactionsStream(
        accountId: String,
        statementId: String,
        scope: CoroutineScope,
    ): ScreenDataStream<List<TransactionItem>> {
        observedAccountId = accountId
        observedStatementId = statementId
        return ScreenDataStream(state = transactionStates, refreshTrigger = transactionRefreshes)
    }

    private companion object {
        const val REFRESH_REPLAY = 16
    }
}
