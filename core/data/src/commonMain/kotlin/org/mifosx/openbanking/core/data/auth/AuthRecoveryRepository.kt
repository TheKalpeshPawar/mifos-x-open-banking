/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.auth

import org.mifosx.openbanking.core.data.obp.toResult
import org.mifosx.openbanking.core.model.obp.ChangePasswordRequest
import org.mifosx.openbanking.core.model.obp.PasswordResetConfirmRequest
import org.mifosx.openbanking.core.model.obp.PasswordResetRequest
import org.mifosx.openbanking.core.network.api.AuthRecoveryApi

/** Password recovery (forgot-password) + authenticated password change. */
interface AuthRecoveryRepository {
    suspend fun initiateReset(email: String): Result<String>
    suspend fun confirmReset(token: String, newPassword: String): Result<String>
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<String>
}

class AuthRecoveryRepositoryImpl(
    private val api: AuthRecoveryApi,
) : AuthRecoveryRepository {

    override suspend fun initiateReset(email: String): Result<String> =
        api.initiateReset(PasswordResetRequest(email)).toResult().map { it.message }

    override suspend fun confirmReset(token: String, newPassword: String): Result<String> =
        api.confirmReset(PasswordResetConfirmRequest(token, newPassword)).toResult().map { it.message }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<String> =
        api.changePassword(ChangePasswordRequest(currentPassword, newPassword)).toResult().map { it.message }
}
