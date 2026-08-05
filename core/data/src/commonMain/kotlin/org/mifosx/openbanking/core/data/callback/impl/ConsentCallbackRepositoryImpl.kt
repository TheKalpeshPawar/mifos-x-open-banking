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

import org.mifosx.openbanking.core.data.callback.ConsentCallbackRepository
import org.mifosx.openbanking.core.data.callback.ConsentStatusResult
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

    override suspend fun exchangeCode(
        code: String,
        redirectUri: String,
    ): ScreenState<PsuTokenResponse> =
        oauth.exchangeAuthorizationCode(code, redirectUri).toScreenState()

    override suspend fun pollConsentStatus(
        consentId: String,
    ): ScreenState<ConsentStatusResult> {
        val tokenResult = oauth.clientCredentialsToken(ConsentCreationScope.ACCOUNTS)
        val accessToken = when (tokenResult) {
            is NetworkResult.Success -> tokenResult.data.accessToken
            is NetworkResult.Error -> return tokenResult.toScreenState()
        }

        val screen = aisp.getConsent(accessToken, consentId).toScreenState()

        return when (screen) {
            is ScreenState.Content -> ScreenState.Content(
                ConsentStatusResult(
                    status = ConsentStatus.fromString(screen.data.data.status),
                    expirationDateTime = screen.data.data.expirationDateTime,
                ),
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
    }
}
