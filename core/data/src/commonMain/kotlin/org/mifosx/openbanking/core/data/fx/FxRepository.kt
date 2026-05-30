/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.fx

import org.mifosx.openbanking.core.data.obp.toResult
import org.mifosx.openbanking.core.model.obp.FxRate
import org.mifosx.openbanking.core.network.api.FxApi
import org.mifosx.openbanking.core.network.obp.ObpConfig

/** Read access to OBP foreign-exchange rates. */
interface FxRepository {
    suspend fun getRate(from: String, to: String): Result<FxRate>
}

class FxRepositoryImpl(
    private val api: FxApi,
    private val config: ObpConfig,
) : FxRepository {

    override suspend fun getRate(from: String, to: String): Result<FxRate> =
        api.getRate(config.bankId, from, to).toResult()
}
