/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.store.applications

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.mifosx.openbanking.core.database.cache.ObpCacheDao
import org.mifosx.openbanking.core.model.obp.AccountApplication
import org.mifosx.openbanking.core.model.obp.ObpException
import org.mifosx.openbanking.core.network.api.AccountApplicationsApi
import org.mifosx.openbanking.core.network.obp.ObpConfig
import org.mifosx.openbanking.core.store.infra.jsonCachedStore
import org.mobilenativefoundation.store.store5.Store
import template.core.base.network.NetworkResult

/**
 * Durable, offline-first Store5 store for the bank's account applications
 * (field-officer review queue), keyed by [Unit]. Backed by the Room JSON cache.
 */
fun provideAccountApplicationsStore(
    api: AccountApplicationsApi,
    config: ObpConfig,
    dao: ObpCacheDao,
    json: Json,
): Store<Unit, List<AccountApplication>> = jsonCachedStore(
    cacheKey = "account-applications",
    dao = dao,
    json = json,
    serializer = ListSerializer(AccountApplication.serializer()),
) {
    when (val result = api.listApplications(config.bankId)) {
        is NetworkResult.Success -> result.data.accountApplications
        is NetworkResult.Error -> throw ObpException(reason = result.error.name)
    }
}
