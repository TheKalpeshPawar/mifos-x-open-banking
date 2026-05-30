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

/** OBP authenticated user profile (`/users/current`). */
@Serializable
data class UserProfile(
    @SerialName("user_id") val userId: String = "",
    val username: String = "",
    val email: String = "",
    val provider: String = "",
    val entitlements: Entitlements = Entitlements(),
    @SerialName("date_joined") val dateJoined: String = "",
    @SerialName("last_login_at") val lastLoginAt: String = "",
)

@Serializable
data class Entitlements(
    val list: List<Entitlement> = emptyList(),
)

@Serializable
data class Entitlement(
    @SerialName("entitlement_id") val entitlementId: String = "",
    @SerialName("role_name") val roleName: String = "",
    @SerialName("bank_id") val bankId: String = "",
)

/** Body for updating mutable profile fields. */
@Serializable
data class ProfileUpdateRequest(
    val email: String,
    @SerialName("language_preference") val languagePreference: String = "",
)
