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

/**
 * Body for requesting a password-reset email (OBP `users/password-reset-url`). OBP looks the user
 * up by both [username] and [email]; they are distinct fields and must both match a real account.
 */
@Serializable
data class PasswordResetRequest(
    val username: String,
    val email: String,
)

/** Body for completing a password reset with the emailed token (OBP `users/password`). */
@Serializable
data class PasswordResetConfirmRequest(
    val token: String,
    @SerialName("new_password") val newPassword: String,
)

/** Generic single-message OBP response (reset acknowledgements). */
@Serializable
data class MessageResponse(
    val message: String = "",
)
