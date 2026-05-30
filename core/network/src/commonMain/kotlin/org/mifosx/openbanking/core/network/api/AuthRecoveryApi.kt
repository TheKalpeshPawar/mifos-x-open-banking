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
import org.mifosx.openbanking.core.model.obp.ChangePasswordRequest
import org.mifosx.openbanking.core.model.obp.MessageResponse
import org.mifosx.openbanking.core.model.obp.PasswordResetConfirmRequest
import org.mifosx.openbanking.core.model.obp.PasswordResetRequest
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

/** OBP password recovery + change endpoints. */
interface AuthRecoveryApi {

    @POST("v4.0.0/auth/password/reset/initiate")
    suspend fun initiateReset(
        @Body request: PasswordResetRequest,
    ): NetworkResult<MessageResponse, NetworkError>

    @POST("v4.0.0/auth/password/reset/confirm")
    suspend fun confirmReset(
        @Body request: PasswordResetConfirmRequest,
    ): NetworkResult<MessageResponse, NetworkError>

    @POST("v4.0.0/users/current/password")
    suspend fun changePassword(
        @Body request: ChangePasswordRequest,
    ): NetworkResult<MessageResponse, NetworkError>
}
