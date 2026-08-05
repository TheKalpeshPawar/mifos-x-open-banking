/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.banking.impl

import org.mifosx.openbanking.core.data.banking.PaymentInitiationRepository
import org.mifosx.openbanking.core.data.banking.mapper.consentIdOrNull
import org.mifosx.openbanking.core.data.banking.mapper.statusOrEmpty
import org.mifosx.openbanking.core.data.banking.mapper.toConsentRequest
import org.mifosx.openbanking.core.data.banking.mapper.toPaymentReceipt
import org.mifosx.openbanking.core.data.banking.mapper.toPaymentRequest
import org.mifosx.openbanking.core.data.callback.PaymentAuthSession
import org.mifosx.openbanking.core.model.banking.payment.PaymentDraft
import org.mifosx.openbanking.core.model.banking.payment.PaymentReceipt
import org.mifosx.openbanking.core.model.banking.payment.StagedConsent
import org.mifosx.openbanking.core.network.api.ConsentCreationScope
import org.mifosx.openbanking.core.network.api.OAuth
import org.mifosx.openbanking.core.network.api.Pisp
import org.mifosx.openbanking.core.network.authorize.generateConsentAuthorizationUrl
import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult
import kotlin.time.Clock

private const val AUTHORIZE_PATH = "/obie/open-banking/v1.1/oauth2/authorize"
private const val RESPONSE_TYPE = "code id_token"

/**
 * Talks to the PISP endpoints directly rather than through a store — see
 * [PaymentInitiationRepository] for why nothing here may be cached.
 *
 * Stateless, like every repository here: the only things it holds are its injected collaborators.
 * The in-flight authorisation lives in [PaymentAuthSession], which survives the browser hop and a
 * process death; a field here would not.
 */
internal class PaymentInitiationRepositoryImpl(
    private val pisp: Pisp,
    private val oauth: OAuth,
    private val paymentAuthSession: PaymentAuthSession,
    private val signingKeyPem: String,
    private val clientId: String,
    private val kid: String,
    private val bankHost: String,
    private val authorizeHost: String,
    private val redirectUri: String,
) : PaymentInitiationRepository {

    @Suppress("ReturnCount")
    override suspend fun stagePayment(draft: PaymentDraft): NetworkResult<StagedConsent, NetworkError> {
        val tokenResult = oauth.clientCredentialsToken(ConsentCreationScope.PAYMENTS)
        val paymentsToken = when (tokenResult) {
            is NetworkResult.Success -> tokenResult.data.accessToken
            is NetworkResult.Error -> return tokenResult
        }

        val consentResult = pisp.createDomesticPaymentConsent(
            paymentsScopeToken = paymentsToken,
            request = draft.toConsentRequest(),
            idempotencyKey = draft.idempotencyKey,
        )
        val consent = when (consentResult) {
            is NetworkResult.Success -> consentResult.data
            is NetworkResult.Error -> return consentResult
        }

        val consentId = consent.consentIdOrNull()
            ?: return NetworkResult.Error(
                NetworkError.Client.BadRequest("Consent response carried no ConsentId"),
            )

        val auth = generateConsentAuthorizationUrl(
            audience = "https://$bankHost",
            authorizeUrl = "https://$authorizeHost$AUTHORIZE_PATH",
            clientId = clientId,
            kid = kid,
            scope = ConsentCreationScope.PAYMENTS,
            responseType = RESPONSE_TYPE,
            redirectUri = redirectUri,
            consentId = consentId,
            signingKeyPem = signingKeyPem,
            nowEpochSeconds = Clock.System.now().epochSeconds,
        )

        paymentAuthSession.savePending(consentId = consentId, state = auth.state, nonce = auth.nonce)

        return NetworkResult.Success(
            StagedConsent(
                consentId = consentId,
                status = consent.statusOrEmpty(),
                authorizationUrl = auth.authorizationUrl,
                state = auth.state,
                nonce = auth.nonce,
            ),
        )
    }

    override suspend fun confirmFunds(consentId: String): NetworkResult<Boolean, NetworkError> {
        val token = paymentAuthSession.paymentToken()?.accesstoken
            ?: return NetworkResult.Error(
                NetworkError.Client.Unauthorized("No payments token — the consent is not authorised"),
            )

        return when (val result = pisp.getFundsConfirmation(token, consentId)) {
            is NetworkResult.Success ->
                NetworkResult.Success(result.data.data?.fundsAvailableResult?.fundsAvailable == true)

            is NetworkResult.Error -> result
        }
    }

    override suspend fun submitPayment(
        draft: PaymentDraft,
        consentId: String,
    ): NetworkResult<PaymentReceipt, NetworkError> {
        val token = paymentAuthSession.paymentToken()?.accesstoken
            ?: return NetworkResult.Error(
                NetworkError.Client.Unauthorized("No payments token — the consent is not authorised"),
            )

        return when (
            val result = pisp.createDomesticPayment(
                psuAccessToken = token,
                request = draft.toPaymentRequest(consentId),
                idempotencyKey = draft.idempotencyKey,
            )
        ) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.toPaymentReceipt())
            is NetworkResult.Error -> result
        }
    }

    /**
     * Reads the status back on whichever credential is still usable.
     *
     * Prefers the payments PSU token, and falls back to a fresh client-credentials token so tracking
     * survives expiry of the single-payment PSU token — the status of an already-submitted payment
     * stays readable long after the consent that authorised it has gone.
     */
    override suspend fun paymentStatus(domesticPaymentId: String): NetworkResult<PaymentReceipt, NetworkError> {
        val token = paymentAuthSession.paymentToken()?.accesstoken
            ?: when (val fallback = oauth.clientCredentialsToken(ConsentCreationScope.PAYMENTS)) {
                is NetworkResult.Success -> fallback.data.accessToken
                is NetworkResult.Error -> return fallback
            }

        return when (val result = pisp.getDomesticPayment(token, domesticPaymentId)) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.toPaymentReceipt())
            is NetworkResult.Error -> result
        }
    }
}
