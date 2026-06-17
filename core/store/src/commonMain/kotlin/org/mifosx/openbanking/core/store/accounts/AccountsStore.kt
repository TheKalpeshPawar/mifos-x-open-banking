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

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.mifosx.openbanking.core.database.cache.ObpCacheDao
import org.mifosx.openbanking.core.model.obp.Account
import org.mifosx.openbanking.core.model.obp.ObpException
import org.mifosx.openbanking.core.network.api.AccountsApi
import org.mifosx.openbanking.core.store.infra.jsonCachedStore
import org.mobilenativefoundation.store.store5.Store
import template.core.base.network.NetworkResult

/**
 * Durable, offline-first Store5 store for the user's accounts. Backed by the Room
 * JSON cache (survives process death) via [jsonCachedStore]; refreshes from
 * [AccountsApi] and surfaces errors as Store5 error responses.
 *
 * The `/my/accounts` list omits balance, number and routings, so each account is
 * enriched in parallel via the v7 core-detail endpoint before being cached.
 */
fun provideAccountsStore(
    api: AccountsApi,
    dao: ObpCacheDao,
    json: Json,
): Store<Unit, List<Account>> = jsonCachedStore(
    cacheKey = "accounts",
    dao = dao,
    json = json,
    serializer = ListSerializer(Account.serializer()),
) {
    val accounts = when (val result = api.myAccounts()) {
        is NetworkResult.Success -> result.data.accounts
        is NetworkResult.Error -> throw ObpException(reason = result.error.name)
    }
    coroutineScope {
        accounts
            .map { account -> async { enrichWithDetail(api, account) } }
            .awaitAll()
    }
}

/**
 * Fetches the v7 core detail (balance/number/routings) for [account] and merges it onto
 * the list entry. Falls back to the un-enriched list entry if the detail call fails, so a
 * single bad account never blanks the whole list.
 */
private suspend fun enrichWithDetail(api: AccountsApi, account: Account): Account {
    val bankId = account.bankId
    val accountId = account.accountIdOrId
    if (bankId.isBlank() || accountId.isBlank()) return account
    return when (val detail = api.myAccountDetail(bankId, accountId)) {
        is NetworkResult.Success -> account.copy(
            number = detail.data.number.ifBlank { account.number },
            balance = detail.data.balance,
            accountRoutings = detail.data.accountRoutings.ifEmpty { account.accountRoutings },
            accountType = account.typeOrProduct.ifBlank { detail.data.productCode },
        )
        is NetworkResult.Error -> account
    }
}
