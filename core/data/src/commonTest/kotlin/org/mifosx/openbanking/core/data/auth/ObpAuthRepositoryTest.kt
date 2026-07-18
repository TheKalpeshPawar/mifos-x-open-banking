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

import io.ktor.client.HttpClient
import kotlinx.coroutines.test.runTest
import org.mifosx.openbanking.core.data.auth.impl.ObpAuthRepositoryImpl
import org.mifosx.openbanking.core.model.obp.DirectLoginResponse
import org.mifosx.openbanking.core.network.api.AuthApi
import org.mifosx.openbanking.core.network.obp.InMemoryObpTokenProvider
import org.mifosx.openbanking.core.network.obp.ObpConfig
import org.mifosx.openbanking.core.network.obp.OidcApi
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Test double for the OBP DirectLogin endpoint. Records the last `Authorization` header so login
 * tests can assert the [org.mifosx.openbanking.core.network.obp.ObpAuth.loginHeader] shape, and
 * returns a configurable [result] so both success and error paths are exercised.
 */
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
}

class ObpAuthRepositoryTest {

    private val config = ObpConfig(consumerKey = "consumer-xyz")

    /**
     * A real [OidcApi] wired to a bare [HttpClient]. The OIDC tests below deliberately exercise the
     * repository's pre-network guards (state validation / undiscovered provider), so no request is
     * ever dispatched through this client — it only satisfies the constructor's OIDC dependency.
     */
    private fun oidcApi(): OidcApi = OidcApi(client = HttpClient(), obpBaseUrl = config.baseUrl)

    private fun repository(
        authApi: AuthApi,
        tokenProvider: InMemoryObpTokenProvider,
    ): ObpAuthRepositoryImpl =
        ObpAuthRepositoryImpl(
            authApi = authApi,
            oidcApi = oidcApi(),
            config = config,
            tokenProvider = tokenProvider,
        )

    @Test
    fun login_success_storesTokenAndSetsLoggedIn() = runTest {
        val tokenProvider = InMemoryObpTokenProvider()
        val api = FakeAuthApi(NetworkResult.Success(DirectLoginResponse(token = "tok-123")))
        val repo = repository(api, tokenProvider)

        val result = repo.login("alice", "secret")

        assertTrue(result.isSuccess)
        assertEquals("tok-123", tokenProvider.token())
        assertTrue(repo.isLoggedIn())
        assertTrue(api.lastAuthorization!!.contains("consumer_key=\"consumer-xyz\""))
        assertTrue(api.lastAuthorization!!.contains("username=\"alice\""))
    }

    @Test
    fun login_error_leavesNoToken() = runTest {
        val tokenProvider = InMemoryObpTokenProvider()
        val api = FakeAuthApi(NetworkResult.Error(NetworkError.BAD_REQUEST))
        val repo = repository(api, tokenProvider)

        val result = repo.login("alice", "wrong")

        assertTrue(result.isFailure)
        assertNull(tokenProvider.token())
        assertFalse(repo.isLoggedIn())
    }

    @Test
    fun logout_clearsToken() = runTest {
        val tokenProvider = InMemoryObpTokenProvider()
        val repo = repository(FakeAuthApi(), tokenProvider)
        repo.login("alice", "secret")

        repo.logout()

        assertNull(tokenProvider.token())
        assertFalse(repo.isLoggedIn())
    }

    @Test
    fun completeOidc_beforePrepare_failsWithoutTouchingTokenOrNetwork() = runTest {
        val tokenProvider = InMemoryObpTokenProvider()
        val repo = repository(FakeAuthApi(), tokenProvider)

        val result = repo.completeOidc(code = "auth-code", state = "any-state")

        assertTrue(result.isFailure)
        assertNull(tokenProvider.token())
        assertFalse(repo.isLoggedIn())
    }
}
