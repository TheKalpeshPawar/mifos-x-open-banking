/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.model.user

import kotlinx.serialization.Serializable

@Serializable
data class UserData(
    val activeUserId: String,
    val themeBrand: ThemeBrand,
    val darkThemeConfig: DarkThemeConfig,
    val useDynamicColor: Boolean,
    val appLanguage: LanguageConfig,
    val isAuthenticated: Boolean,
    val isUnlocked: Boolean,
    val passcode: String,
    val enableScreenCapture: Boolean,
    val isPasscodeEnabled: Boolean,
    val isBiometricsEnabled: Boolean,
    val selectedAccountId: String,
) {
    companion object {
        /**
         * A fresh install has seen nothing and authorised nothing.
         *
         * These previously shipped as `isAuthenticated = true` and a hardcoded
         * `passcode = "1234"` — template values that were never adjusted for this app. The
         * effect was that every launch fell through the navigator's gates straight to Home, so
         * onboarding, login and the consent callback were all unreachable.
         *
         * `isAuthenticated` is retained only for consumers that still read it; the navigator no
         * longer trusts it. Whether the PSU is signed in is derived from an authorised consent — see
         * `ConsentSession`.
         */
        val DEFAULT = UserData(
            activeUserId = "",
            passcode = "",
            themeBrand = ThemeBrand.DEFAULT,
            darkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
            useDynamicColor = false,
            appLanguage = LanguageConfig.DEFAULT,
            isAuthenticated = false,
            isUnlocked = true,
            isPasscodeEnabled = false,
            isBiometricsEnabled = false,
            enableScreenCapture = false,
            selectedAccountId = "",
        )
    }
}
