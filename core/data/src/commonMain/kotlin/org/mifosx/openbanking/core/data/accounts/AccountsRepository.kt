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

import kotlinx.coroutines.CoroutineScope
import org.mifosx.openbanking.core.model.obp.Account
import template.core.base.store.screen.ScreenDataStream

/** Read access to the authenticated user's OBP accounts. */
interface AccountsRepository {
    /**
     * Store5-backed, network-aware stream of the account list (cache-then-network,
     * auto-refresh on reconnect). The canonical offline-first read path for the
     * accounts/home screens.
     */
    fun accountsStream(scope: CoroutineScope): ScreenDataStream<List<Account>>

    suspend fun listAccounts(): Result<List<Account>>
    suspend fun myAccounts(): Result<List<Account>>
    suspend fun accountDetail(accountId: String): Result<Account>
}
