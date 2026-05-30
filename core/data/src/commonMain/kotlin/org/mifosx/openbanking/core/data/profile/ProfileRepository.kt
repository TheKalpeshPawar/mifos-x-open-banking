/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.profile

import org.mifosx.openbanking.core.data.obp.toResult
import org.mifosx.openbanking.core.model.obp.ProfileUpdateRequest
import org.mifosx.openbanking.core.model.obp.UserProfile
import org.mifosx.openbanking.core.network.api.ProfileApi

/** Read + update the authenticated user's OBP profile. */
interface ProfileRepository {
    suspend fun current(): Result<UserProfile>
    suspend fun update(request: ProfileUpdateRequest): Result<UserProfile>
}

class ProfileRepositoryImpl(
    private val api: ProfileApi,
) : ProfileRepository {

    override suspend fun current(): Result<UserProfile> =
        api.getCurrentUser().toResult()

    override suspend fun update(request: ProfileUpdateRequest): Result<UserProfile> =
        api.updateCurrentUser(request).toResult()
}
