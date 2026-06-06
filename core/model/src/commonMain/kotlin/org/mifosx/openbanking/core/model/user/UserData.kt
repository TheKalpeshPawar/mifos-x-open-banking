/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
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
    val showOnboarding: Boolean,
    val firstTimeUser: Boolean,
    val isAuthenticated: Boolean,
    val isUnlocked: Boolean,
    val passcode: String,
    val enableScreenCapture: Boolean,
    val isPasscodeEnabled: Boolean,
    val isBiometricsEnabled: Boolean,
    // Notification preferences (surfaced on the Settings screen). Defaulted so older
    // persisted payloads that predate these fields deserialize without data loss.
    val isPushNotificationsEnabled: Boolean = true,
    val isTransactionAlertsEnabled: Boolean = true,
    val isMarketingEnabled: Boolean = false,
    val authToken: String? = null,
    // The user's chosen default account (set from the Home hero card). Empty = no choice yet;
    // consumers fall back to their checking-first heuristics. Defaulted for older payloads.
    val defaultAccountId: String = "",
) {
    companion object {
        val DEFAULT = UserData(
            activeUserId = "",
            passcode = "1234",
            themeBrand = ThemeBrand.DEFAULT,
            darkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
            useDynamicColor = false,
            appLanguage = LanguageConfig.DEFAULT,
            isAuthenticated = false,
            isUnlocked = false,
            isPasscodeEnabled = false,
            isBiometricsEnabled = false,
            showOnboarding = false,
            firstTimeUser = false,
            enableScreenCapture = false,
            isPushNotificationsEnabled = true,
            isTransactionAlertsEnabled = true,
            isMarketingEnabled = false,
        )
    }
}
