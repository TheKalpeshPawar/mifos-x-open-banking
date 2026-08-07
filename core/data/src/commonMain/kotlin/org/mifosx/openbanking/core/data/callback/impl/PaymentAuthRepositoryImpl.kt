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

import org.mifosx.openbanking.core.data.banking.mapper.statusOrEmpty
import org.mifosx.openbanking.core.data.callback.PaymentAuthRepository
import org.mifosx.openbanking.core.data.callback.PaymentAuthSession
import org.mifosx.openbanking.core.data.callback.PaymentAuthValidation
import org.mifosx.openbanking.core.network.api.ConsentCreationScope
import org.mifosx.openbanking.core.network.api.OAuth
import org.mifosx.openbanking.core.network.api.Pisp
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import kotlin.time.Clock

private const val ERROR_ACCESS_DENIED = "access_denied"

internal class PaymentAuthRepositoryImpl(
    private val oauth: OAuth,
    private val pisp: Pisp,
    private val paymentAuthSession: PaymentAuthSession,
    private val redirectUri: String,
) : PaymentAuthRepository {

    override fun isPaymentRedirect(redirectUrl: String): Boolean {
        val pending = paymentAuthSession.pendingConsentId()
        if (pending.isNullOrBlank()) return false
        return paymentAuthSession.matchesPendingState(parseCallbackUrl(redirectUrl).state)
    }

    /**
     * Order matters, and mirrors the sign-in leg:
     *  - `state` is checked first, against the value held for the authorisation in flight. No
     *    pending authorisation means nothing authentic to check against — a hard SecurityError.
     *  - Errors are classified BEFORE the nonce check, because HSBC returns no `id_token` when the
     *    PSU declines; checking the nonce first would report a plain refusal as a SecurityError.
     */
    @Suppress("ReturnCount")
    override fun validateCallback(redirectUrl: String): PaymentAuthValidation {
        val params = parseCallbackUrl(redirectUrl)

        val consentId = paymentAuthSession.pendingConsentId()
        if (consentId.isNullOrBlank() || !paymentAuthSession.matchesPendingState(params.state)) {
            return PaymentAuthValidation.SecurityError
        }

        if (params.error != null) {
            return if (params.error == ERROR_ACCESS_DENIED) {
                PaymentAuthValidation.AccessDenied
            } else {
                PaymentAuthValidation.Error(
                    params.errorDescription ?: "HSBC reported an error: ${params.error}",
                )
            }
        }

        if (idTokenNonceMismatch(params.idToken, paymentAuthSession.pendingNonce().orEmpty())) {
            return PaymentAuthValidation.SecurityError
        }

        val code = params.code ?: return PaymentAuthValidation.MissingCode
        return PaymentAuthValidation.Valid(code = code, consentId = consentId)
    }

    override suspend fun exchangeCode(code: String): NetworkResult<Unit, NetworkError> =
        when (val result = oauth.exchangeAuthorizationCode(code, redirectUri)) {
            is NetworkResult.Success -> {
                paymentAuthSession.savePaymentToken(result.data)
                NetworkResult.Success(Unit)
            }

            is NetworkResult.Error -> result
        }

    /**
     * Read on a **client-credentials** token, never the PSU one.
     *
     * A consent resource is TPP-authenticated: the same credential that created it reads it back.
     * The PSU token from the authorisation leg authorises the *payment* — funds confirmation and
     * submission — and presenting it here returns `401`, which is what this did before, right after
     * an authorisation that had actually succeeded.
     */
    @Suppress("ReturnCount")
    override suspend fun consentStatus(consentId: String): NetworkResult<String, NetworkError> {
        val token = when (val result = oauth.clientCredentialsToken(ConsentCreationScope.PAYMENTS)) {
            is NetworkResult.Success -> result.data.accessToken
            is NetworkResult.Error -> return result
        }

        return when (val result = pisp.getDomesticPaymentConsent(token, consentId)) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.statusOrEmpty())
            is NetworkResult.Error -> result
        }
    }

    override fun recordApproved() {
        paymentAuthSession.saveApprovedAt(Clock.System.now().toString())
    }

    override fun discardAuthorisation() = paymentAuthSession.clear()
}
