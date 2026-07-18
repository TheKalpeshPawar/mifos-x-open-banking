/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.accounts

import org.mifosx.openbanking.core.data.callback.ConsentSession
import org.mifosx.openbanking.core.network.model.oauth.PsuTokenResponse
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** A [ConsentSession] seeded with a fixed consent expiry, for exercising the expiry banner logic. */
@OptIn(ExperimentalTime::class)
class FakeConsentSession(
    private val expiration: Instant? = null,
    private val storedConsentId: String? = null,
) : ConsentSession {
    override fun isActive(): Boolean = true
    override fun tokens(): PsuTokenResponse? = null
    override fun save(tokens: PsuTokenResponse) = Unit
    override fun saveConsentMeta(consentId: String, expirationDateTime: String) = Unit
    override fun consentId(): String? = storedConsentId
    override fun consentExpiration(): Instant? = expiration
    override fun clear() = Unit
}
