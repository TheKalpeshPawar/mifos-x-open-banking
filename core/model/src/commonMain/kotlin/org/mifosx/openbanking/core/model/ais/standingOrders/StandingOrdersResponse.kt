/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.model.ais.standingOrders

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StandingOrdersResponse(
    @SerialName("Data")
    val data: Data? = null,
    @SerialName("Links")
    val links: Links? = null,
    @SerialName("Meta")
    val meta: Meta? = null,
)
