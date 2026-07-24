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
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Pins the storage contract [SettingsConsentSession] backs: tokens gate the "signed in" state, and
 * the consent id + expiry round-trip and are wiped alongside the tokens on [ConsentSession.clear].
 */
@OptIn(ExperimentalTime::class)
class ConsentSessionTest {

    private val settings = MapSettings()
    private val session = SettingsConsentSession(settings)

    private val token = PsuTokenResponse(
        accesstoken = "psu-access-token",
        tokentype = "Bearer",
        expiresin = 300,
        scope = "openid accounts",
    )

    @Test
    fun savedTokensMakeTheSessionActiveAndReadBack() {
        session.save(token)

        assertTrue(session.isActive())
        assertEquals(token, session.tokens())
    }

    @Test
    fun anEmptySessionIsNotActive() {
        assertFalse(session.isActive())
        assertNull(session.tokens())
    }

    @Test
    fun saveConsentMetaRoundTripsIdAndExpiry() {
        session.saveConsentMeta("cn-1", "2026-07-01T00:00:00Z")

        assertEquals("cn-1", session.consentId())
        assertEquals(Instant.parse("2026-07-01T00:00:00Z"), session.consentExpiration())
    }

    @Test
    fun consentExpirationIsNullWhenAbsent() {
        assertNull(session.consentId())
        assertNull(session.consentExpiration())
    }

    @Test
    fun consentExpirationIsNullWhenUnparseable() {
        session.saveConsentMeta("cn-1", "not-a-timestamp")

        assertEquals("cn-1", session.consentId())
        assertNull(session.consentExpiration())
    }

    @Test
    fun clearWipesTokensAndConsentMeta() {
        session.save(token)
        session.saveConsentMeta("cn-1", "2026-07-01T00:00:00Z")

        session.clear()

        assertNull(session.tokens())
        assertNull(session.consentId())
        assertNull(session.consentExpiration())
    }

    @Test
    fun forgetAllWipesEverything() {
        session.save(token)
        session.saveConsentMeta("cn-1", "2026-07-01T00:00:00Z")

        session.forgetAll()

        assertNull(session.tokens())
        assertNull(session.consentId())
        assertNull(session.consentExpiration())
        assertTrue(settings.keys.isEmpty(), "forgetAll must leave nothing behind")
    }
}
