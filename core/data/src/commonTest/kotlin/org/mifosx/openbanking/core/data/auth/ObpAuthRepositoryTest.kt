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

import kotlinx.coroutines.test.runTest
import org.mifosx.openbanking.core.data.auth.impl.ObpAuthRepositoryImpl
import org.mifosx.openbanking.core.model.obp.DirectLoginResponse
import org.mifosx.openbanking.core.model.obp.MessageResponse
import org.mifosx.openbanking.core.model.obp.OidcToken
import org.mifosx.openbanking.core.network.api.AuthApi
import org.mifosx.openbanking.core.network.obp.InMemoryObpTokenProvider
import org.mifosx.openbanking.core.network.obp.ObpConfig
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeAuthApi(
    var result: NetworkResult<DirectLoginResponse, NetworkError> =
        NetworkResult.Success(DirectLoginResponse(token = "tok-123")),
) : AuthApi {
    var lastAuthorization: String? = null
    override suspend fun directLogin(
        authorization: String,
    ): NetworkResult<DirectLoginResponse, NetworkError> {
        lastAuthorization = authorization
        return result
    }

    override suspend fun oidcToken(
        code: String,
        redirectUri: String,
        clientId: String,
        codeVerifier: String,
        grantType: String,
    ): NetworkResult<OidcToken, NetworkError> = NetworkResult.Success(OidcToken(accessToken = "oidc-acc"))

    override suspend fun oidcRefresh(
        refreshToken: String,
        clientId: String,
        grantType: String,
    ): NetworkResult<OidcToken, NetworkError> = NetworkResult.Success(OidcToken(accessToken = "oidc-acc"))

    override suspend fun oidcRevoke(
        token: String,
        clientId: String,
    ): NetworkResult<MessageResponse, NetworkError> = NetworkResult.Success(MessageResponse("revoked"))
}

class ObpAuthRepositoryTest {

    private val config = ObpConfig(consumerKey = "consumer-xyz")

    @Test
    fun login_success_storesTokenAndSetsLoggedIn() = runTest {
        val tokenProvider = InMemoryObpTokenProvider()
        val api = FakeAuthApi(NetworkResult.Success(DirectLoginResponse(token = "tok-123")))
        val repo = ObpAuthRepositoryImpl(api, config, tokenProvider)

        val result = repo.login("alice", "secret")

        assertTrue(result.isSuccess)
        assertEquals("tok-123", tokenProvider.token())
        assertTrue(repo.isLoggedIn())
        // credentials + consumer key travel in the Authorization header, not the body
        assertTrue(api.lastAuthorization!!.contains("consumer_key=\"consumer-xyz\""))
        assertTrue(api.lastAuthorization!!.contains("username=\"alice\""))
    }

    @Test
    fun login_error_leavesNoToken() = runTest {
        val tokenProvider = InMemoryObpTokenProvider()
        val api = FakeAuthApi(NetworkResult.Error(NetworkError.BAD_REQUEST))
        val repo = ObpAuthRepositoryImpl(api, config, tokenProvider)

        val result = repo.login("alice", "wrong")

        assertTrue(result.isFailure)
        assertNull(tokenProvider.token())
        assertFalse(repo.isLoggedIn())
    }

    @Test
    fun logout_clearsToken() = runTest {
        val tokenProvider = InMemoryObpTokenProvider()
        val repo = ObpAuthRepositoryImpl(FakeAuthApi(), config, tokenProvider)
        repo.login("alice", "secret")

        repo.logout()

        assertNull(tokenProvider.token())
        assertFalse(repo.isLoggedIn())
    }
}
