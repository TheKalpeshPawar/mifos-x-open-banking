/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.network.api

import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Path
import org.mifosx.openbanking.core.model.obp.Agent
import org.mifosx.openbanking.core.model.obp.AgentRequest
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

/** OBP agent endpoints (field-officer agent-registration). */
interface AgentsApi {

    @POST("v5.1.0/banks/{bankId}/agents")
    suspend fun registerAgent(
        @Path("bankId") bankId: String,
        @Body request: AgentRequest,
    ): NetworkResult<Agent, NetworkError>
}
