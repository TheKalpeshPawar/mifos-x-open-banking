/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.callback.impl

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.mifosx.openbanking.core.data.callback.ConsentCallbackRepository
import org.mifosx.openbanking.core.data.callback.PendingAuthStore
import org.mifosx.openbanking.core.data.callback.ValidationResult
import org.mifosx.openbanking.core.data.util.toScreenState
import org.mifosx.openbanking.core.model.callback.ConsentStatus
import org.mifosx.openbanking.core.network.api.Aisp
import org.mifosx.openbanking.core.network.api.ConsentCreationScope
import org.mifosx.openbanking.core.network.api.OAuth
import org.mifosx.openbanking.core.network.model.oauth.PsuTokenResponse
import template.core.base.common.screen.DataFreshness
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkResult
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class ConsentCallbackRepositoryImpl(
    private val oauth: OAuth,
    private val aisp: Aisp,
    private val pendingAuthStore: PendingAuthStore,
) : ConsentCallbackRepository {

    /**
     * Order matters here:
     *  - [PendingAuthStore.consume] runs first and unconditionally, making the redirect single-use.
     *    Absent or expired means nothing authentic to check against — a hard SecurityError, never a
     *    soft pass.
     *  - Errors are classified BEFORE the nonce check, because HSBC returns no `id_token` when the
     *    PSU declines; checking the nonce first would report a plain refusal as a SecurityError.
     */
    @Suppress("ReturnCount")
    override fun validateCallback(redirectUrl: String): ValidationResult {
        val params = parseCallbackUrl(redirectUrl)

        val pending = pendingAuthStore.consume() ?: return ValidationResult.SecurityError

        if (params.state != pending.state) return ValidationResult.SecurityError

        if (params.error != null) {
            return if (params.error == ERROR_ACCESS_DENIED) {
                ValidationResult.AccessDenied
            } else {
                ValidationResult.Error(
                    params.errorDescription ?: "HSBC reported an error: ${params.error}",
                )
            }
        }

        if (idTokenNonceMismatch(params.idToken, pending.nonce)) return ValidationResult.SecurityError

        val code = params.code ?: return ValidationResult.MissingCode
        return ValidationResult.Valid(code = code, consentId = pending.consentId)
    }

    /**
     * Fails closed: anything unreadable counts as a mismatch.
     *
     * Padding is optional because a JWT's base64url segments are unpadded per RFC 7515 §2, while
     * Kotlin's [Base64.UrlSafe] demands padding by default and throws without it. Decoding a real
     * HSBC `id_token` with the strict variant therefore threw on every genuine consent and reported
     * it as a nonce mismatch.
     */
    @OptIn(ExperimentalEncodingApi::class)
    @Suppress("ReturnCount")
    private fun idTokenNonceMismatch(idToken: String?, expectedNonce: String): Boolean {
        if (idToken == null) return true
        return try {
            val parts = idToken.split(".")
            if (parts.size < 2) return true
            val payload = jwtBase64.decode(parts[1]).decodeToString()
            val nonce = Json.parseToJsonElement(payload).jsonObject["nonce"]?.jsonPrimitive?.content
            nonce != expectedNonce
        } catch (_: Exception) {
            true
        }
    }

    override suspend fun exchangeCode(
        code: String,
        redirectUri: String,
    ): ScreenState<PsuTokenResponse> =
        oauth.exchangeAuthorizationCode(code, redirectUri).toScreenState()

    override suspend fun pollConsentStatus(
        consentId: String,
    ): ScreenState<ConsentStatus> {
        val tokenResult = oauth.clientCredentialsToken(ConsentCreationScope.ACCOUNTS)
        val accessToken = when (tokenResult) {
            is NetworkResult.Success -> tokenResult.data.accessToken
            is NetworkResult.Error -> return tokenResult.toScreenState()
        }

        val screen = aisp.getConsent(accessToken, consentId).toScreenState()

        return when (screen) {
            is ScreenState.Content -> ScreenState.Content(
                ConsentStatus.fromString(screen.data.data.status),
                DataFreshness.FRESH,
            )
            is ScreenState.Error -> ScreenState.Error(screen.error)
            is ScreenState.NoNetwork -> ScreenState.NoNetwork(screen.isCaptivePortal)
            is ScreenState.Unauthenticated -> ScreenState.Unauthenticated
            is ScreenState.Loading -> ScreenState.Loading
            is ScreenState.Empty -> ScreenState.Empty
        }
    }

    private companion object {
        const val ERROR_ACCESS_DENIED = "access_denied"

        /** JWT segments are unpadded base64url (RFC 7515 §2); accept them with or without padding. */
        @OptIn(ExperimentalEncodingApi::class)
        val jwtBase64: Base64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
    }
}
