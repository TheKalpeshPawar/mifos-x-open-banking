/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.standingorders

import org.mifosx.openbanking.core.data.obp.toResult
import org.mifosx.openbanking.core.model.obp.StandingOrder
import org.mifosx.openbanking.core.network.api.StandingOrdersApi
import org.mifosx.openbanking.core.network.obp.ObpConfig

/** Read + cancel access to an account's OBP standing orders. */
interface StandingOrdersRepository {
    suspend fun list(accountId: String): Result<List<StandingOrder>>
    suspend fun get(accountId: String, standingOrderId: String): Result<StandingOrder>
    suspend fun cancel(accountId: String, standingOrderId: String): Result<StandingOrder>
}

class StandingOrdersRepositoryImpl(
    private val api: StandingOrdersApi,
    private val config: ObpConfig,
) : StandingOrdersRepository {

    override suspend fun list(accountId: String): Result<List<StandingOrder>> =
        api.listStandingOrders(config.bankId, accountId).toResult().map { it.standingOrders }

    override suspend fun get(accountId: String, standingOrderId: String): Result<StandingOrder> =
        api.getStandingOrder(config.bankId, accountId, standingOrderId).toResult()

    override suspend fun cancel(accountId: String, standingOrderId: String): Result<StandingOrder> =
        api.cancelStandingOrder(config.bankId, accountId, standingOrderId).toResult()
}
