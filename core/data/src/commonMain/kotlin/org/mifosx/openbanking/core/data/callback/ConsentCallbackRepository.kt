/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.data.callback

import org.mifosx.openbanking.core.model.callback.ConsentStatus
import org.mifosx.openbanking.core.model.oauth.PsuTokenResponse
import template.core.base.common.screen.ScreenState

data class CallbackParams(
    val code: String?,
    val idToken: String?,
    val state: String?,
    val error: String?,
    val errorDescription: String?,
    val expectedState: String,
    val expectedNonce: String,
)

sealed interface ValidationResult {
    data object Valid : ValidationResult
    data object SecurityError : ValidationResult
    data object AccessDenied : ValidationResult
    data object MissingCode : ValidationResult
    data class Error(val message: String) : ValidationResult
}

interface ConsentCallbackRepository {
    fun validateCallback(params: CallbackParams): ValidationResult

    suspend fun exchangeCode(
        code: String,
        redirectUri: String,
    ): ScreenState<PsuTokenResponse>

    suspend fun pollConsentStatus(
        consentId: String,
    ): ScreenState<ConsentStatus>
}
