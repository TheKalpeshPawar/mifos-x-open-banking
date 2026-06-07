/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.accounts

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.mifosx.openbanking.core.model.obp.Account
import kotlin.concurrent.Volatile

/**
 * Accounts with detail-level [Account.productCode] resolved. The list endpoint returns a
 * null product code, so each account needs one detail fetch; fetches run in parallel and
 * the merged result is cached for the session. A failed detail fetch degrades to the
 * list-shape account (classified PERSONAL by default) instead of failing the whole set.
 *
 * The mutex guards only cache reads/writes — network work runs outside the lock, so
 * concurrent first calls may fetch twice but can never deadlock or serialize each other.
 */
class PfmAccountsService(
    private val accountsRepository: AccountsRepository,
) {
    private val mutex = Mutex()

    @Volatile
    private var cached: List<Account>? = null

    suspend fun classifiedAccounts(): Result<List<Account>> {
        mutex.withLock { cached }?.let { return Result.success(it) }
        return accountsRepository.myAccounts().mapCatching { accounts ->
            coroutineScope {
                accounts.map { account ->
                    async { account.withDetailFields() }
                }.awaitAll()
            }
        }.onSuccess { resolved -> mutex.withLock { cached = resolved } }
    }

    /** Drops the session cache so the next call re-fetches (e.g. after adding an account). */
    fun invalidate() {
        cached = null
    }

    /**
     * Copies classification-bearing fields from the detail response onto the list-shape
     * account. The detail response may omit `bank_id`, so the list account stays the base.
     */
    private suspend fun Account.withDetailFields(): Account {
        val detail = accountsRepository.accountDetail(bankId, accountIdOrId).getOrNull() ?: return this
        return copy(
            accountType = detail.accountType.ifBlank { accountType },
            productCode = detail.productCode.ifBlank { productCode },
            label = label.ifBlank { detail.label },
            balance = if (detail.balance.currency.isNotBlank()) detail.balance else balance,
        )
    }
}
