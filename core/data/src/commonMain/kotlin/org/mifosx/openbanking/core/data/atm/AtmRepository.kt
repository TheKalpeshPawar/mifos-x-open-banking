/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.atm

import org.mifosx.openbanking.core.data.obp.toResult
import org.mifosx.openbanking.core.model.obp.Atm
import org.mifosx.openbanking.core.network.api.AtmApi
import org.mifosx.openbanking.core.network.obp.ObpConfig

/** Read access to OBP ATM locations. */
interface AtmRepository {
    suspend fun listAtms(): Result<List<Atm>>
}

class AtmRepositoryImpl(
    private val api: AtmApi,
    private val config: ObpConfig,
) : AtmRepository {

    override suspend fun listAtms(): Result<List<Atm>> =
        api.listAtms(config.bankId).toResult().map { it.atms }
}
