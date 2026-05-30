/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.core.data.auth.impl

import org.mifosx.openbanking.core.data.auth.ObpAuthRepository
import org.mifosx.openbanking.core.data.obp.toResult
import org.mifosx.openbanking.core.network.api.AuthApi
import org.mifosx.openbanking.core.network.obp.ObpAuth
import org.mifosx.openbanking.core.network.obp.ObpConfig
import org.mifosx.openbanking.core.network.obp.ObpTokenProvider

/**
 * DirectLogin auth repository. Posts credentials in the `Authorization` header, stores
 * the returned token in the [ObpTokenProvider] (in-memory in Phase 3; secure storage in
 * Phase 7), after which the client plugin authenticates every OBP call.
 */
class ObpAuthRepositoryImpl(
    private val authApi: AuthApi,
    private val config: ObpConfig,
    private val tokenProvider: ObpTokenProvider,
) : ObpAuthRepository {

    override suspend fun login(username: String, password: String): Result<Unit> {
        val header = ObpAuth.loginHeader(username, password, config.consumerKey)
        return authApi.directLogin(config.bankId, header)
            .toResult()
            .map { response -> tokenProvider.setToken(response.token) }
    }

    override fun logout() = tokenProvider.clear()

    override fun isLoggedIn(): Boolean = tokenProvider.token() != null
}
