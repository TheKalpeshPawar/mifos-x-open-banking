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
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.PUT
import org.mifosx.openbanking.core.model.obp.ProfileUpdateRequest
import org.mifosx.openbanking.core.model.obp.UserProfile
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

/** OBP current-user profile endpoints. */
interface ProfileApi {

    @GET("v4.0.0/users/current")
    suspend fun getCurrentUser(): NetworkResult<UserProfile, NetworkError>

    @PUT("v4.0.0/users/current")
    suspend fun updateCurrentUser(
        @Body request: ProfileUpdateRequest,
    ): NetworkResult<UserProfile, NetworkError>
}
