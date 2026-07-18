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

import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json
import org.mifosx.openbanking.core.network.model.oauth.PsuTokenResponse
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Whether the PSU has a usable HSBC session, and the tokens behind it.
 *
 * In an AISP app "signed in" is not a flag someone sets — it is whether an authorised consent
 * produced PSU tokens. Deriving it from the tokens keeps the two from drifting: a stored boolean can
 * claim the user is authenticated when no consent exists, which is exactly what shipped
 * (`UserData.DEFAULT.isAuthenticated = true`, only ever written `false` again by `clearUserData()`).
 *
 * Also owns the storage key, so the callback and the navigator cannot disagree about where the
 * session lives.
 */
interface ConsentSession {
    /** True when a completed consent has left PSU tokens behind. */
    fun isActive(): Boolean

    fun tokens(): PsuTokenResponse?

    fun save(tokens: PsuTokenResponse)

    /** Persists the consent's id and its ISO-8601 expiry alongside the tokens. */
    fun saveConsentMeta(consentId: String, expirationDateTime: String)

    /** The stored consent id, or null when none was saved. */
    fun consentId(): String?

    /** The stored consent expiry parsed to an [Instant], or null when absent or unparseable. */
    @OptIn(ExperimentalTime::class)
    fun consentExpiration(): Instant?

    fun clear()
}

class SettingsConsentSession(
    private val secureSettings: Settings,
) : ConsentSession {

    private val json = Json { ignoreUnknownKeys = true }

    override fun isActive(): Boolean = tokens()?.accesstoken?.isNotBlank() == true

    override fun tokens(): PsuTokenResponse? {
        val raw = secureSettings.getStringOrNull(KEY_TOKENS) ?: return null
        return runCatching { json.decodeFromString(PsuTokenResponse.serializer(), raw) }.getOrNull()
    }

    override fun save(tokens: PsuTokenResponse) {
        secureSettings.putString(KEY_TOKENS, json.encodeToString(PsuTokenResponse.serializer(), tokens))
    }

    override fun saveConsentMeta(consentId: String, expirationDateTime: String) {
        secureSettings.putString(KEY_CONSENT_ID, consentId)
        secureSettings.putString(KEY_CONSENT_EXPIRATION, expirationDateTime)
    }

    override fun consentId(): String? = secureSettings.getStringOrNull(KEY_CONSENT_ID)

    @OptIn(ExperimentalTime::class)
    override fun consentExpiration(): Instant? =
        secureSettings.getStringOrNull(KEY_CONSENT_EXPIRATION)
            ?.let { runCatching { Instant.parse(it) }.getOrNull() }

    override fun clear() {
        secureSettings.remove(KEY_TOKENS)
        secureSettings.remove(KEY_CONSENT_ID)
        secureSettings.remove(KEY_CONSENT_EXPIRATION)
    }

    private companion object {
        const val KEY_TOKENS = "hsbc_tokens"
        const val KEY_CONSENT_ID = "hsbc_consent_id"
        const val KEY_CONSENT_EXPIRATION = "hsbc_consent_expiration"
    }
}
