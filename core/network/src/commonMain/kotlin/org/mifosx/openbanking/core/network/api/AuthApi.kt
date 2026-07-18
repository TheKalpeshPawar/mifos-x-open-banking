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

import de.jensklingenberg.ktorfit.http.Header
import de.jensklingenberg.ktorfit.http.POST
import org.mifosx.openbanking.core.model.obp.DirectLoginResponse
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

/**
 * OBP DirectLogin endpoint. OIDC/OAuth2 lives in
 * [org.mifosx.openbanking.core.network.obp.OidcApi] because it targets the OIDC provider host
 * (not the OBP API base) and must not carry the DirectLogin session token.
 */
interface AuthApi {

    /**
     * Exchange DirectLogin credentials for a session token. Credentials travel in the
     * `Authorization` header (see [org.mifosx.openbanking.core.network.obp.ObpAuth.loginHeader]),
     * not the body.
     */
    @POST("v6.0.0/my/logins/direct")
    suspend fun directLogin(
        @Header("Authorization") authorization: String,
    ): NetworkResult<DirectLoginResponse, NetworkError>
}
