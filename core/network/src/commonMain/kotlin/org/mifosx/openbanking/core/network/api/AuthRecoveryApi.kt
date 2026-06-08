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
import org.mifosx.openbanking.core.model.obp.MessageResponse
import org.mifosx.openbanking.core.model.obp.PasswordResetConfirmRequest
import org.mifosx.openbanking.core.model.obp.PasswordResetRequest
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

/**
 * OBP password-recovery endpoints. OBP has no authenticated self-service change-password; the only
 * password mutation is the email-based reset: request a reset URL (anonymous), then complete it with
 * the emailed token. The reset is completed via the emailed web link, so [confirmReset] is currently
 * unused by the app.
 */
interface AuthRecoveryApi {

    @POST("v6.0.0/users/password-reset-url")
    suspend fun initiateReset(
        @Body request: PasswordResetRequest,
    ): NetworkResult<MessageResponse, NetworkError>

    @POST("v6.0.0/users/password")
    suspend fun confirmReset(
        @Body request: PasswordResetConfirmRequest,
    ): NetworkResult<MessageResponse, NetworkError>
}
