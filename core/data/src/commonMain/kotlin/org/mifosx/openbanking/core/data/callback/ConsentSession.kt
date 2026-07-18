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

    override fun clear() {
        secureSettings.remove(KEY_TOKENS)
    }

    private companion object {
        const val KEY_TOKENS = "hsbc_tokens"
    }
}
