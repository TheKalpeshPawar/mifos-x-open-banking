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
import org.mifosx.openbanking.core.network.model.oauth.PsuTokenResponse
import template.core.base.common.screen.ScreenState

sealed interface ValidationResult {
    /**
     * The redirect is authentic. Carries the values the caller needs next, so the caller never has
     * to re-parse the URL or reach into [PendingAuthStore] itself.
     */
    data class Valid(
        val code: String,
        val consentId: String,
    ) : ValidationResult

    data object SecurityError : ValidationResult
    data object AccessDenied : ValidationResult
    data object MissingCode : ValidationResult
    data class Error(val message: String) : ValidationResult
}

interface ConsentCallbackRepository {
    /**
     * Parses and authenticates HSBC's raw redirect URL.
     *
     * Takes the URL rather than pre-parsed parts on purpose: the `state`/`nonce` it is checked
     * against come from [PendingAuthStore], which is this layer's business. If the caller supplied
     * the expected values, the caller could pass the wrong ones — or, as the previous signature
     * allowed, an empty nonce that silently skipped replay validation entirely.
     *
     * Single-use: consumes the stored [PendingAuth], so the same redirect cannot be replayed.
     */
    fun validateCallback(redirectUrl: String): ValidationResult

    suspend fun exchangeCode(
        code: String,
        redirectUri: String,
    ): ScreenState<PsuTokenResponse>

    suspend fun pollConsentStatus(
        consentId: String,
    ): ScreenState<ConsentStatus>
}
