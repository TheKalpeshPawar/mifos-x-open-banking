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

import de.jensklingenberg.ktorfit.http.Field
import de.jensklingenberg.ktorfit.http.FormUrlEncoded
import de.jensklingenberg.ktorfit.http.Header
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Path
import org.mifosx.openbanking.core.model.obp.DirectLoginResponse
import org.mifosx.openbanking.core.model.obp.MessageResponse
import org.mifosx.openbanking.core.model.obp.OidcToken
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

    /** OIDC: exchange an authorization code (with PKCE verifier) for tokens. */
    @FormUrlEncoded
    @POST("v4.0.0/oauth2/token")
    suspend fun oidcToken(
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("client_id") clientId: String,
        @Field("code_verifier") codeVerifier: String,
        @Field("grant_type") grantType: String = "authorization_code",
    ): NetworkResult<OidcToken, NetworkError>

    /** OIDC: silent re-auth using a stored refresh token. */
    @FormUrlEncoded
    @POST("v4.0.0/oauth2/token")
    suspend fun oidcRefresh(
        @Field("refresh_token") refreshToken: String,
        @Field("client_id") clientId: String,
        @Field("grant_type") grantType: String = "refresh_token",
    ): NetworkResult<OidcToken, NetworkError>

    /** OIDC: revoke an access or refresh token on logout. */
    @FormUrlEncoded
    @POST("v4.0.0/oauth2/revoke")
    suspend fun oidcRevoke(
        @Field("token") token: String,
        @Field("client_id") clientId: String,
    ): NetworkResult<MessageResponse, NetworkError>
}
