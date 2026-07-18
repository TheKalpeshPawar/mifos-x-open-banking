/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.consentcallback

import org.mifosx.openbanking.core.data.callback.ConsentCallbackRepository
import org.mifosx.openbanking.core.data.callback.ConsentStatusResult
import org.mifosx.openbanking.core.data.callback.ValidationResult
import org.mifosx.openbanking.core.model.callback.ConsentStatus
import org.mifosx.openbanking.core.network.model.oauth.PsuTokenResponse
import template.core.base.common.screen.DataFreshness
import template.core.base.common.screen.ScreenState

class FakeConsentCallbackRepository : ConsentCallbackRepository {

    var validateResult: ValidationResult = ValidationResult.Valid(code = "auth-code", consentId = "cn-1")

    var exchangeResult: ScreenState<PsuTokenResponse> = ScreenState.Content(
        PSU_TOKEN,
        DataFreshness.FRESH,
    )

    /** Successive poll results, so the awaiting-then-authorised journey can be driven. */
    var pollResults: MutableList<ScreenState<ConsentStatusResult>> = mutableListOf(
        ScreenState.Content(ConsentStatusResult(ConsentStatus.Authorised, null), DataFreshness.FRESH),
    )

    var validatedUrls: MutableList<String> = mutableListOf()
        private set

    var exchangedCodes: MutableList<String> = mutableListOf()
        private set

    var polledConsentIds: MutableList<String> = mutableListOf()
        private set

    override fun validateCallback(redirectUrl: String): ValidationResult {
        validatedUrls += redirectUrl
        return validateResult
    }

    override suspend fun exchangeCode(code: String, redirectUri: String): ScreenState<PsuTokenResponse> {
        exchangedCodes += code
        return exchangeResult
    }

    override suspend fun pollConsentStatus(consentId: String): ScreenState<ConsentStatusResult> {
        polledConsentIds += consentId
        return if (pollResults.size > 1) pollResults.removeFirst() else pollResults.first()
    }

    companion object {
        val PSU_TOKEN = PsuTokenResponse(
            accesstoken = "psu-access-token",
            tokentype = "Bearer",
            expiresin = 300,
            scope = "openid accounts",
        )

        /** [ScreenState.Error] carries a raw [Throwable]; its message is what the VM surfaces. */
        val NETWORK_ERROR = RuntimeException("Token exchange failed.")
    }
}
