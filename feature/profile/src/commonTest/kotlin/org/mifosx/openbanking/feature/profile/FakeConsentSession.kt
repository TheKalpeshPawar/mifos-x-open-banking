/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.profile

import org.mifosx.openbanking.core.data.callback.ConsentSession
import org.mifosx.openbanking.core.network.model.oauth.PsuTokenResponse
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * In-memory [ConsentSession] whose activity and expiry the test sets directly.
 *
 * Re-declared here rather than imported: test source sets do not cross Gradle module boundaries, so
 * `core:data`'s own fakes are invisible from a feature module. [clearCount] records the one
 * observable effect of signing out.
 */
@OptIn(ExperimentalTime::class)
class FakeConsentSession(
    private var active: Boolean = true,
    private var expiration: Instant? = null,
    private var consentId: String? = "urn-hsbc-consent-1029",
) : ConsentSession {

    /** How many times the credentials were dropped, whether by `clear` or by `forgetAll`. */
    var clearCount: Int = 0
        private set

    /** How many times sign-out reached the session, which is the `forgetAll` path. */
    var forgetAllCount: Int = 0
        private set

    private var savedTokens: PsuTokenResponse? = null

    fun setActive(value: Boolean) {
        active = value
    }

    fun setExpiration(value: Instant?) {
        expiration = value
    }

    override fun isActive(): Boolean = active

    override fun tokens(): PsuTokenResponse? = savedTokens

    override fun save(tokens: PsuTokenResponse) {
        savedTokens = tokens
        active = true
    }

    override fun saveConsentMeta(consentId: String, expirationDateTime: String) {
        this.consentId = consentId
        expiration = runCatching { Instant.parse(expirationDateTime) }.getOrNull()
    }

    override fun consentId(): String? = consentId

    override fun consentExpiration(): Instant? = expiration

    override fun clear() {
        clearCount++
        savedTokens = null
        consentId = null
        expiration = null
        active = false
    }

    override fun forgetAll() {
        forgetAllCount++
        clear()
    }
}
