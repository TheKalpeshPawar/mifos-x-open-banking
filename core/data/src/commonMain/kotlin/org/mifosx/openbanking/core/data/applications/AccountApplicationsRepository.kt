/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.applications

import org.mifosx.openbanking.core.data.obp.toResult
import org.mifosx.openbanking.core.model.obp.AccountApplication
import org.mifosx.openbanking.core.model.obp.AccountApplicationStatusRequest
import org.mifosx.openbanking.core.network.api.AccountApplicationsApi
import org.mifosx.openbanking.core.network.obp.ObpConfig

/** Read + status-update OBP account applications (field-officer review). */
interface AccountApplicationsRepository {
    suspend fun list(): Result<List<AccountApplication>>
    suspend fun get(applicationId: String): Result<AccountApplication>
    suspend fun updateStatus(applicationId: String, status: String): Result<AccountApplication>
}

class AccountApplicationsRepositoryImpl(
    private val api: AccountApplicationsApi,
    private val config: ObpConfig,
) : AccountApplicationsRepository {

    override suspend fun list(): Result<List<AccountApplication>> =
        api.listApplications(config.bankId).toResult().map { it.accountApplications }

    override suspend fun get(applicationId: String): Result<AccountApplication> =
        api.getApplication(config.bankId, applicationId).toResult()

    override suspend fun updateStatus(applicationId: String, status: String): Result<AccountApplication> =
        api.updateStatus(config.bankId, applicationId, AccountApplicationStatusRequest(status)).toResult()
}
