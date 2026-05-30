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

import org.mifosx.openbanking.core.model.obp.Account

/** Read access to the authenticated user's OBP accounts. */
interface AccountsRepository {
    suspend fun listAccounts(): Result<List<Account>>
    suspend fun myAccounts(): Result<List<Account>>
    suspend fun accountDetail(accountId: String): Result<Account>
}
