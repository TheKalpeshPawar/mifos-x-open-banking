/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.store.accounts

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.mifosx.openbanking.core.database.cache.ObpCacheDao
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.core.model.obp.ObpException
import org.mifosx.openbanking.core.network.api.AccountsApi
import org.mifosx.openbanking.core.network.obp.ObpConfig
import org.mifosx.openbanking.core.store.infra.jsonCachedStore
import org.mobilenativefoundation.store.store5.Store
import template.core.base.network.NetworkResult

/**
 * Durable, offline-first Store5 store for the user's accounts. Backed by the Room
 * JSON cache (survives process death) via [jsonCachedStore]; refreshes from
 * [AccountsApi] and surfaces errors as Store5 error responses.
 */
fun provideAccountsStore(
    api: AccountsApi,
    config: ObpConfig,
    dao: ObpCacheDao,
    json: Json,
): Store<Unit, List<Account>> = jsonCachedStore(
    cacheKey = "accounts",
    dao = dao,
    json = json,
    serializer = ListSerializer(Account.serializer()),
) {
    when (val result = api.listAccounts(config.bankId)) {
        is NetworkResult.Success -> result.data.accounts
        is NetworkResult.Error -> throw ObpException(reason = result.error.name)
    }
}
