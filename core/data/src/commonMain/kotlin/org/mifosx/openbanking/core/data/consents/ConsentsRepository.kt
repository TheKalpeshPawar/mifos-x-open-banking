/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.consents

import org.mifosx.openbanking.core.data.obp.toResult
import org.mifosx.openbanking.core.model.obp.Consent
import org.mifosx.openbanking.core.network.api.ConsentsApi

/** Read + revoke OBP consents (connected apps). */
interface ConsentsRepository {
    suspend fun list(consumerId: String): Result<List<Consent>>
    suspend fun revoke(consumerId: String, consentId: String): Result<Consent>
}

class ConsentsRepositoryImpl(
    private val api: ConsentsApi,
) : ConsentsRepository {

    override suspend fun list(consumerId: String): Result<List<Consent>> =
        api.listConsents(consumerId).toResult().map { it.consents }

    override suspend fun revoke(consumerId: String, consentId: String): Result<Consent> =
        api.revokeConsent(consumerId, consentId).toResult()
}
