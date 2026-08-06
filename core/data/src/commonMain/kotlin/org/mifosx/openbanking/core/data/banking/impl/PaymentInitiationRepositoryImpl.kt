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

import org.mifosx.openbanking.core.data.banking.AccountCapabilityRegistry
import org.mifosx.openbanking.core.data.banking.PaymentInitiationRepository
import org.mifosx.openbanking.core.data.banking.mapper.consentIdOrNull
import org.mifosx.openbanking.core.data.banking.mapper.statusOrEmpty
import org.mifosx.openbanking.core.data.banking.mapper.toConsentRequest
import org.mifosx.openbanking.core.data.banking.mapper.toPaymentReceipt
import org.mifosx.openbanking.core.data.banking.mapper.toPaymentRequest
import org.mifosx.openbanking.core.data.callback.PaymentAuthSession
import org.mifosx.openbanking.core.data.util.isDebtorAccountRefusal
import org.mifosx.openbanking.core.data.util.toThrowable
import org.mifosx.openbanking.core.model.banking.payment.PaymentDraft
import org.mifosx.openbanking.core.model.banking.payment.PaymentReceipt
import org.mifosx.openbanking.core.model.banking.payment.StagedConsent
import org.mifosx.openbanking.core.model.hsbcProduct.AccountEndpoint
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
    private val capabilityRegistry: AccountCapabilityRegistry,
    private val signingKeyPem: String,
    private val clientId: String,
    private val kid: String,
    private val bankHost: String,
    private val authorizeHost: String,
    private val redirectUri: String,
) : PaymentInitiationRepository {

    @Suppress("ReturnCount")
    override suspend fun stagePayment(draft: PaymentDraft): NetworkResult<StagedConsent, NetworkError> {
        // Every payment starts from nothing: a fresh client-credentials token below, a fresh consent
        // from the bank, a fresh state/nonce for the authorisation, and a fresh PSU token when that
        // authorisation returns. Clearing first is what guarantees the last of those — without it a
        // payments token left behind by an abandoned attempt would still be in storage, and the next
        // payment could submit on a credential its own authorisation never issued.
        paymentAuthSession.clear()

        val tokenResult = oauth.clientCredentialsToken(ConsentCreationScope.PAYMENTS)
        val paymentsToken = when (tokenResult) {
            is NetworkResult.Success -> tokenResult.data.accessToken
            is NetworkResult.Error -> return tokenResult
        }

        val consentResult = pisp.createDomesticPaymentConsent(
            paymentsScopeToken = paymentsToken,
            request = draft.toConsentRequest(),
            idempotencyKey = draft.consentIdempotencyKey,
        )
        val consent = when (consentResult) {
            is NetworkResult.Success -> consentResult.data
            is NetworkResult.Error -> return consentResult.alsoRecordRefusedPayer(draft)
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
        paymentAuthSession.saveDraft(draft)

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

    /**
     * Remembers that the bank refused this payer, so the picker can stop offering it.
     *
     * The product matrix predicts what it cheaply can — a credit card is visible in the account
     * subtype — but it cannot predict everything: a Global Money wallet reports `AccountTypeCode:
     * CACC` and is indistinguishable from a current account by the time the app sees it, yet HSBC
     * refuses it as a debtor. Rather than enumerate products the app cannot identify, this learns
     * from the refusal itself, mirroring how AIS reads already correct their own predictions on a
     * `U000`.
     *
     * Keyed on the error PATH, not the code: `U021` and `U002` were both observed on
     * `Data.Initiation.DebtorAccount.Identification`, and a third code on the same path means the
     * same thing.
     */
    private fun NetworkResult.Error<NetworkError>.alsoRecordRefusedPayer(
        draft: PaymentDraft,
    ): NetworkResult.Error<NetworkError> = also {
        if (error.toThrowable().isDebtorAccountRefusal()) {
            capabilityRegistry.markUnsupported(
                accountId = draft.debtorAccount.accountId,
                endpoint = AccountEndpoint.PaymentDebtor,
            )
        }
    }

    override fun stagedDraft(): PaymentDraft? = paymentAuthSession.draft()

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
                idempotencyKey = draft.paymentIdempotencyKey,
            )
        ) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.toPaymentReceipt())
            is NetworkResult.Error -> result
        }
    }

    /**
     * Reads the status back on a **client-credentials** token.
     *
     * Deliberately not the PSU token: that one is minted for a single payment and expires with it,
     * whereas a submitted payment stays readable indefinitely on the TPP's own credential. Using the
     * client-credentials token is what lets someone come back tomorrow and still see the outcome —
     * and it avoids the `401` that presenting a PSU token to a TPP-authenticated read earns.
     */
    @Suppress("ReturnCount")
    override suspend fun paymentStatus(domesticPaymentId: String): NetworkResult<PaymentReceipt, NetworkError> {
        val token = when (val result = oauth.clientCredentialsToken(ConsentCreationScope.PAYMENTS)) {
            is NetworkResult.Success -> result.data.accessToken
            is NetworkResult.Error -> return result
        }

        return when (val result = pisp.getDomesticPayment(token, domesticPaymentId)) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.toPaymentReceipt())
            is NetworkResult.Error -> result
        }
    }
}
