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
import de.jensklingenberg.ktorfit.http.Path
import org.mifosx.openbanking.core.model.obp.DirectLoginResponse
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

/** OBP authentication endpoints. */
interface AuthApi {

    /**
     * Exchange DirectLogin credentials for a session token. Credentials travel in the
     * `Authorization` header (see [org.mifosx.openbanking.core.network.obp.ObpAuth.loginHeader]),
     * not the body.
     */
    @POST("v4.0.0/banks/{bankId}/direct_login")
    suspend fun directLogin(
        @Path("bankId") bankId: String,
        @Header("Authorization") authorization: String,
    ): NetworkResult<DirectLoginResponse, NetworkError>
}
