/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.kyc

import org.mifosx.openbanking.core.data.obp.toResult
import org.mifosx.openbanking.core.model.obp.KycCheckRequest
import org.mifosx.openbanking.core.model.obp.KycDocument
import org.mifosx.openbanking.core.network.api.KycApi
import org.mifosx.openbanking.core.network.obp.ObpConfig

/** Read KYC documents and submit KYC check results for a customer. */
interface KycRepository {
    suspend fun listDocuments(customerId: String): Result<List<KycDocument>>
    suspend fun submitCheck(customerId: String, request: KycCheckRequest): Result<KycCheckRequest>
}

class KycRepositoryImpl(
    private val api: KycApi,
    private val config: ObpConfig,
) : KycRepository {

    override suspend fun listDocuments(customerId: String): Result<List<KycDocument>> =
        api.listDocuments(config.bankId, customerId).toResult().map { it.documents }

    override suspend fun submitCheck(customerId: String, request: KycCheckRequest): Result<KycCheckRequest> =
        api.submitCheck(config.bankId, customerId, request).toResult()
}
