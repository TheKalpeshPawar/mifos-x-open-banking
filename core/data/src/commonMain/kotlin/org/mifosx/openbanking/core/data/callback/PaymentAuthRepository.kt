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

import template.core.base.network.NetworkError
import template.core.base.network.NetworkResult

/** The outcome of checking a redirect that came back from a payment authorisation. */
sealed interface PaymentAuthValidation {
    /** Authentic. [code] is the authorization code to exchange, against consent [consentId]. */
    data class Valid(val code: String, val consentId: String) : PaymentAuthValidation

    /** `state` or `nonce` did not match what was issued — treat as hostile, never as a retry. */
    data object SecurityError : PaymentAuthValidation

    /** The PSU declined at the bank. Not an error; the payment simply does not proceed. */
    data object AccessDenied : PaymentAuthValidation

    data object MissingCode : PaymentAuthValidation

    data class Error(val message: String) : PaymentAuthValidation
}

/**
 * The payment authorisation leg — the app-to-app hop that turns a staged consent into one the PSU
 * has approved.
 *
 * Structurally the sign-in leg's twin, and deliberately not the same object: this exchanges on
 * `scope=payments` and stores the result in [PaymentAuthSession], leaving the AIS session alone.
 * Routing a payment authorisation through [ConsentCallbackRepository] would overwrite the account
 * bearer with a payments-scoped one and break every read in the app.
 */
interface PaymentAuthRepository {

    /**
     * Whether [redirectUrl] belongs to a payment authorisation rather than a sign-in.
     *
     * Both legs come back through the same `ConsentRedirectBus` on the same registered redirect URI,
     * so something has to tell them apart before either is processed — a payment return handled as a
     * sign-in would overwrite the account bearer with a payments-scoped token.
     *
     * Answered by matching the redirect's `state` against the one issued for the payment
     * authorisation in flight. That is a value only this leg could have minted, so a sign-in return
     * can never satisfy it. Cheap and side-effect free: it consumes nothing, leaving the real
     * validation to [validateCallback].
     */
    fun isPaymentRedirect(redirectUrl: String): Boolean

    /**
     * Whether [redirectUrl] is the authentic return leg of the authorisation in flight.
     *
     * Checked against the `state` and `nonce` held in [PaymentAuthSession]; the caller never has to
     * parse the URL or reach into storage itself.
     */
    fun validateCallback(redirectUrl: String): PaymentAuthValidation

    /**
     * Exchanges [code] for the payments-scoped PSU token and stores it in [PaymentAuthSession].
     *
     * A successful exchange is what makes funds confirmation and submission possible.
     */
    suspend fun exchangeCode(code: String): NetworkResult<Unit, NetworkError>

    /**
     * Reads the consent's status back after authorisation.
     *
     * Load-bearing rather than cosmetic: submitting against a consent that is not `Authorised`
     * returns `400 U009`, so this is how the flow knows it may proceed.
     */
    suspend fun consentStatus(consentId: String): NetworkResult<String, NetworkError>
}
