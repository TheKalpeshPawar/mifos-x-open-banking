/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.model.obp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Body for initiating a password reset (forgot-password). */
@Serializable
data class PasswordResetRequest(
    val email: String,
)

/** Body for completing a password reset with the emailed token. */
@Serializable
data class PasswordResetConfirmRequest(
    val token: String,
    @SerialName("new_password") val newPassword: String,
)

/** Body for changing the authenticated user's password. */
@Serializable
data class ChangePasswordRequest(
    @SerialName("current_password") val currentPassword: String,
    @SerialName("new_password") val newPassword: String,
)

/** Generic single-message OBP response (reset/change-password acknowledgements). */
@Serializable
data class MessageResponse(
    val message: String = "",
)
