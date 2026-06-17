/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.store.kyc

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.mifosx.openbanking.core.database.cache.ObpCacheDao
import org.mifosx.openbanking.core.model.obp.KycDocument
import org.mifosx.openbanking.core.model.obp.ObpException
import org.mifosx.openbanking.core.network.api.KycApi
import org.mifosx.openbanking.core.network.obp.ObpConfig
import org.mifosx.openbanking.core.store.infra.keyedJsonCachedStore
import org.mobilenativefoundation.store.store5.Store
import template.core.base.network.NetworkResult

/**
 * Durable, offline-first Store5 store for a customer's KYC documents, keyed by
 * customerId. Backed by the Room JSON cache.
 */
fun provideKycDocumentsStore(
    api: KycApi,
    config: ObpConfig,
    dao: ObpCacheDao,
    json: Json,
): Store<String, List<KycDocument>> = keyedJsonCachedStore(
    dao = dao,
    json = json,
    serializer = ListSerializer(KycDocument.serializer()),
    cacheKey = { customerId -> "kyc:$customerId" },
) { customerId ->
    when (val result = api.listDocuments(config.bankId, customerId)) {
        is NetworkResult.Success -> result.data.documents
        is NetworkResult.Error -> throw ObpException(reason = result.error.name)
    }
}
