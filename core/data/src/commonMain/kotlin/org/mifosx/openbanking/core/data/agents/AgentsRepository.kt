/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.agents

import org.mifosx.openbanking.core.data.obp.toResult
import org.mifosx.openbanking.core.model.obp.Agent
import org.mifosx.openbanking.core.model.obp.AgentRequest
import org.mifosx.openbanking.core.network.api.AgentsApi
import org.mifosx.openbanking.core.network.obp.ObpConfig

/** Register the field officer as an OBP bank agent. */
interface AgentsRepository {
    suspend fun register(request: AgentRequest): Result<Agent>
}

class AgentsRepositoryImpl(
    private val api: AgentsApi,
    private val config: ObpConfig,
) : AgentsRepository {

    override suspend fun register(request: AgentRequest): Result<Agent> =
        api.registerAgent(config.bankId, request).toResult()
}
