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

/** OBP DirectLogin session lifecycle. */
interface ObpAuthRepository {

    /** Exchange credentials for a session token and retain it for subsequent calls. */
    suspend fun login(username: String, password: String): Result<Unit>

    /** Drop the session token. */
    fun logout()

    /** True when a session token is currently held. */
    fun isLoggedIn(): Boolean
}
