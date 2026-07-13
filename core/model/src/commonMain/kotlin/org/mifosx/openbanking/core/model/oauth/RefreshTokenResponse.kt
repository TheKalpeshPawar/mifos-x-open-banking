/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.model.oauth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RefreshTokenResponse(
    @SerialName("access_token")
    val accesstoken: String? = null,
    @SerialName("token_type")
    val tokentype: String? = null,
    @SerialName("refresh_token")
    val refreshtoken: String? = null,
    @SerialName("expires_in")
    val expiresin: Int? = null,
    @SerialName("scope")
    val scope: String? = null,
    @SerialName("id_token")
    val idtoken: String? = null,
)
