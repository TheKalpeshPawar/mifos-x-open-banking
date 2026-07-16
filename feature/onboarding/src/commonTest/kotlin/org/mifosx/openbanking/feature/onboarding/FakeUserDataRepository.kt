/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.onboarding

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.mifosx.openbanking.core.data.user.UserDataRepository
import org.mifosx.openbanking.core.model.user.DarkThemeConfig
import org.mifosx.openbanking.core.model.user.LanguageConfig
import org.mifosx.openbanking.core.model.user.ThemeBrand
import org.mifosx.openbanking.core.model.user.UserData

class FakeUserDataRepository(
    firstTimeUser: Boolean = true,
    var errorOnRead: Throwable? = null,
) : UserDataRepository {

    private val _userData = MutableStateFlow(
        UserData.DEFAULT.copy(firstTimeUser = firstTimeUser),
    )
    override val userData: StateFlow<UserData>
        get() = if (errorOnRead != null) throw errorOnRead!! else _userData.asStateFlow()

    var firstTimeStateSetCount = 0
    var lastFirstTimeStateValue: Boolean? = null
        private set

    override val passcode: String get() = _userData.value.passcode
    override val observeLanguage: Flow<LanguageConfig>
        get() = MutableStateFlow(UserData.DEFAULT.appLanguage).asStateFlow()
    override val observeDarkThemeConfig: Flow<DarkThemeConfig>
        get() = MutableStateFlow(UserData.DEFAULT.darkThemeConfig).asStateFlow()
    override val observeDynamicColorPreference: Flow<Boolean>
        get() = MutableStateFlow(UserData.DEFAULT.useDynamicColor).asStateFlow()
    override val observeScreenCapturePreference: Flow<Boolean>
        get() = MutableStateFlow(false).asStateFlow()

    override suspend fun setLanguage(language: LanguageConfig) {}
    override suspend fun setThemeBrand(themeBrand: ThemeBrand) {}
    override suspend fun setDarkThemeConfig(darkThemeConfig: DarkThemeConfig) {}
    override suspend fun setDynamicColorPreference(useDynamicColor: Boolean) {}
    override suspend fun setIsAuthenticated(isAuthenticated: Boolean) {}
    override suspend fun setIsUnlocked(isUnlocked: Boolean) {}
    override suspend fun setIsPasscodeEnabled(isPasscodeEnabled: Boolean) {}
    override suspend fun setIsBiometricsEnabled(isBiometricsEnabled: Boolean) {}
    override suspend fun setShowOnboarding(showOnboarding: Boolean) {}
    override suspend fun setPasscode(passcode: String) {}
    override suspend fun clearUserData() {}

    override suspend fun setFirstTimeState(firstTimeState: Boolean) {
        firstTimeStateSetCount++
        lastFirstTimeStateValue = firstTimeState
        _userData.value = _userData.value.copy(firstTimeUser = firstTimeState)
    }
}
