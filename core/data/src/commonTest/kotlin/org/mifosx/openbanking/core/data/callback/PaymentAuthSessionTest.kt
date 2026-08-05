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

import com.russhwolf.settings.MapSettings
import org.mifosx.openbanking.core.network.model.oauth.PsuTokenResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val CONSENT_ID = "812774903"
private const val PAYMENT_STATE = "payment-state-1"
private const val PAYMENT_NONCE = "payment-nonce-1"

/**
 * Covers [SettingsPaymentAuthSession] — the second consent slot.
 *
 * The property under test throughout is isolation: the payment leg must be able to store and clear
 * its own credentials without the AIS session in [SettingsConsentSession] noticing, because the two
 * share the same secure store and an overwrite would sign the user out of their accounts.
 */
class PaymentAuthSessionTest {

    private fun session(settings: MapSettings = MapSettings()) =
        SettingsPaymentAuthSession(secureSettings = settings) to settings

    @Test
    fun holdsThePendingAuthorisationAcrossTheBrowserHop() {
        val (session, _) = session()

        session.savePending(consentId = CONSENT_ID, state = PAYMENT_STATE, nonce = PAYMENT_NONCE)

        assertEquals(CONSENT_ID, session.pendingConsentId())
        assertEquals(PAYMENT_NONCE, session.pendingNonce())
        assertTrue(session.matchesPendingState(PAYMENT_STATE))
    }

    @Test
    fun refusesAStateItDidNotIssue() {
        val (session, _) = session()
        session.savePending(CONSENT_ID, PAYMENT_STATE, PAYMENT_NONCE)

        assertFalse(session.matchesPendingState("someone-elses-state"))
        assertFalse(session.matchesPendingState(null))
    }

    /** With nothing in flight there is no state to match, so every redirect is someone else's. */
    @Test
    fun matchesNothingWhenNoAuthorisationIsInFlight() {
        val (session, _) = session()

        assertFalse(session.matchesPendingState(PAYMENT_STATE))
        assertNull(session.pendingConsentId())
    }

    @Test
    fun roundTripsThePaymentsScopedToken() {
        val (session, _) = session()

        session.savePaymentToken(PsuTokenResponse(accesstoken = "payments-access", scope = "payments"))

        assertEquals("payments-access", session.paymentToken()?.accesstoken)
    }

    /**
     * The isolation invariant, asserted against the AIS session's real storage keys rather than a
     * fake: writing a payment authorisation must leave `hsbc_tokens` and `hsbc_consent_id` untouched.
     */
    @Test
    fun leavesTheAccountSessionUntouched() {
        val settings = MapSettings()
        val consentSession = SettingsConsentSession(secureSettings = settings)
        consentSession.save(PsuTokenResponse(accesstoken = "ais-access"))
        consentSession.saveConsentMeta(consentId = "ais-consent", expirationDateTime = "2026-12-01T00:00:00Z")

        val paymentSession = SettingsPaymentAuthSession(secureSettings = settings)
        paymentSession.savePending(CONSENT_ID, PAYMENT_STATE, PAYMENT_NONCE)
        paymentSession.savePaymentToken(PsuTokenResponse(accesstoken = "payments-access"))

        assertEquals("ais-access", consentSession.tokens()?.accesstoken)
        assertEquals("ais-consent", consentSession.consentId())
        assertTrue(consentSession.isActive())
    }

    /** And the reverse: clearing the payment leg must not sign the user out. */
    @Test
    fun clearingThePaymentLegLeavesTheUserSignedIn() {
        val settings = MapSettings()
        val consentSession = SettingsConsentSession(secureSettings = settings)
        consentSession.save(PsuTokenResponse(accesstoken = "ais-access"))
        consentSession.saveConsentMeta("ais-consent", "2026-12-01T00:00:00Z")

        val paymentSession = SettingsPaymentAuthSession(secureSettings = settings)
        paymentSession.savePending(CONSENT_ID, PAYMENT_STATE, PAYMENT_NONCE)
        paymentSession.savePaymentToken(PsuTokenResponse(accesstoken = "payments-access"))
        paymentSession.clear()

        assertTrue(consentSession.isActive())
        assertEquals("ais-consent", consentSession.consentId())
        assertNull(paymentSession.paymentToken())
        assertNull(paymentSession.pendingConsentId())
    }
}
