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
import org.mifosx.openbanking.core.data.callback.CallbackParams
import org.mifosx.openbanking.core.data.callback.ConsentCallbackRepository
import org.mifosx.openbanking.core.data.callback.ValidationResult
import org.mifosx.openbanking.core.data.util.toScreenState
import org.mifosx.openbanking.core.model.callback.ConsentStatus
import org.mifosx.openbanking.core.model.oauth.PsuTokenResponse
import org.mifosx.openbanking.core.network.api.Aisp
import org.mifosx.openbanking.core.network.api.ConsentCreationScope
import org.mifosx.openbanking.core.network.api.OAuth
import template.core.base.common.screen.DataFreshness
import template.core.base.common.screen.ScreenState
import template.core.base.network.NetworkResult
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class ConsentCallbackRepositoryImpl(
    private val oauth: OAuth,
    private val aisp: Aisp,
) : ConsentCallbackRepository {

    @Suppress("ReturnCount")
    override fun validateCallback(params: CallbackParams): ValidationResult {
        if (params.state != params.expectedState) return ValidationResult.SecurityError
        if (params.expectedNonce.isNotEmpty() && idTokenNonceMismatch(params.idToken, params.expectedNonce)) {
            return ValidationResult.SecurityError
        }
        if (params.error != null) {
            return if (params.error == "access_denied") {
                ValidationResult.AccessDenied
            } else {
                ValidationResult.Error(params.errorDescription ?: "HSBC reported an error: ${params.error}")
            }
        }
        if (params.code == null) return ValidationResult.MissingCode
        return ValidationResult.Valid
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Suppress("ReturnCount")
    private fun idTokenNonceMismatch(idToken: String?, expectedNonce: String): Boolean {
        if (idToken == null) return true
        return try {
            val parts = idToken.split(".")
            if (parts.size < 2) return true
            val payload = Base64.UrlSafe.decode(parts[1]).decodeToString()
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
}
