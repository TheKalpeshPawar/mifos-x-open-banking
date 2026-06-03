/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.settings.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.core.data.profile.ProfileRepository
import org.mifosx.openbanking.core.data.user.UserDataRepository
import org.mifosx.openbanking.core.model.obp.ProfileUpdateRequest
import org.mifosx.openbanking.core.model.obp.UserProfile
import org.mifosx.openbanking.core.model.user.DarkThemeConfig
import org.mifosx.openbanking.core.model.user.LanguageConfig
import org.mifosx.openbanking.core.model.user.ThemeBrand
import org.mifosx.openbanking.core.model.user.UserData
import template.core.base.store.screen.ScreenState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Stateful fake — mutates its backing [userData] StateFlow on each setter so the ViewModel,
 * which derives its [SettingsUiState] reactively from [userData], observes the change.
 */
private class FakeUserDataRepository(
    initial: UserData = UserData.DEFAULT,
) : UserDataRepository {
    private val _userData = MutableStateFlow(initial)
    override val userData: StateFlow<UserData> = _userData
    override val authToken: String? = null
    override val passcode: String = ""
    override val observeLanguage: Flow<LanguageConfig> = _userData.map { it.appLanguage }
    override val observeDarkThemeConfig: Flow<DarkThemeConfig> = _userData.map { it.darkThemeConfig }
    override val observeDynamicColorPreference: Flow<Boolean> = _userData.map { it.useDynamicColor }
    override val observeScreenCapturePreference: Flow<Boolean> = _userData.map { it.enableScreenCapture }
    override val observePushNotificationsEnabled: Flow<Boolean> = _userData.map { it.isPushNotificationsEnabled }
    override val observeTransactionAlertsEnabled: Flow<Boolean> = _userData.map { it.isTransactionAlertsEnabled }
    override val observeMarketingEnabled: Flow<Boolean> = _userData.map { it.isMarketingEnabled }

    override suspend fun setLanguage(language: LanguageConfig) {
        _userData.value = _userData.value.copy(appLanguage = language)
    }
    override suspend fun setThemeBrand(themeBrand: ThemeBrand) {
        _userData.value = _userData.value.copy(themeBrand = themeBrand)
    }
    override suspend fun setDarkThemeConfig(darkThemeConfig: DarkThemeConfig) {
        _userData.value = _userData.value.copy(darkThemeConfig = darkThemeConfig)
    }
    override suspend fun setDynamicColorPreference(useDynamicColor: Boolean) {
        _userData.value = _userData.value.copy(useDynamicColor = useDynamicColor)
    }
    override suspend fun setIsAuthenticated(isAuthenticated: Boolean) {
        _userData.value = _userData.value.copy(isAuthenticated = isAuthenticated)
    }
    override suspend fun setIsUnlocked(isUnlocked: Boolean) {
        _userData.value = _userData.value.copy(isUnlocked = isUnlocked)
    }
    override suspend fun setIsPasscodeEnabled(isPasscodeEnabled: Boolean) {
        _userData.value = _userData.value.copy(isPasscodeEnabled = isPasscodeEnabled)
    }
    override suspend fun setIsBiometricsEnabled(isBiometricsEnabled: Boolean) {
        _userData.value = _userData.value.copy(isBiometricsEnabled = isBiometricsEnabled)
    }
    override suspend fun setPushNotificationsEnabled(isEnabled: Boolean) {
        _userData.value = _userData.value.copy(isPushNotificationsEnabled = isEnabled)
    }
    override suspend fun setTransactionAlertsEnabled(isEnabled: Boolean) {
        _userData.value = _userData.value.copy(isTransactionAlertsEnabled = isEnabled)
    }
    override suspend fun setMarketingEnabled(isEnabled: Boolean) {
        _userData.value = _userData.value.copy(isMarketingEnabled = isEnabled)
    }
    override suspend fun setShowOnboarding(showOnboarding: Boolean) {
        _userData.value = _userData.value.copy(showOnboarding = showOnboarding)
    }
    override suspend fun setFirstTimeState(firstTimeState: Boolean) {
        _userData.value = _userData.value.copy(firstTimeUser = firstTimeState)
    }
    override suspend fun setPasscode(passcode: String) {
        _userData.value = _userData.value.copy(passcode = passcode)
    }
    override suspend fun clearUserData() {
        _userData.value = UserData.DEFAULT
    }
}

private class FakeProfileRepository(
    private val result: Result<UserProfile> = Result.success(
        UserProfile(username = "Aisha Saleh", email = "aisha.s@example.com"),
    ),
) : ProfileRepository {
    override suspend fun current(): Result<UserProfile> = result
    override suspend fun update(request: ProfileUpdateRequest): Result<UserProfile> = result
}

class SettingsViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun content(vm: SettingsViewModel): SettingsUiState =
        (vm.uiState.value as ScreenState.Content).data

    @Test
    fun initialState_hydratesDefaults_andClearsLoading() = runTest {
        val vm = SettingsViewModel(
            FakeUserDataRepository(),
            FakeProfileRepository(),
            biometricAvailable = true,
            appVersion = "v1.0.0",
        )
        val s = content(vm)
        assertFalse(s.isLoading)
        assertFalse(s.isDarkModeEnabled)
        assertEquals("en", s.selectedLanguage)
        assertTrue(s.isPushNotificationsEnabled)
        assertTrue(s.isTransactionAlertsEnabled)
        assertFalse(s.isMarketingEnabled)
        assertFalse(s.isBiometricLoginEnabled)
        assertTrue(s.isBiometricAvailableOnDevice)
        assertEquals("v1.0.0", s.appVersion)
    }

    @Test
    fun onDarkModeToggled_enablesDarkTheme() = runTest {
        val repo = FakeUserDataRepository()
        val vm = SettingsViewModel(repo, FakeProfileRepository(), biometricAvailable = true)
        vm.onDarkModeToggled()
        assertTrue(content(vm).isDarkModeEnabled)
        assertEquals(DarkThemeConfig.DARK, repo.userData.value.darkThemeConfig)
        vm.onDarkModeToggled()
        assertFalse(content(vm).isDarkModeEnabled)
        assertEquals(DarkThemeConfig.LIGHT, repo.userData.value.darkThemeConfig)
    }

    @Test
    fun onLanguageSelected_persistsMappedLocale() = runTest {
        val repo = FakeUserDataRepository()
        val vm = SettingsViewModel(repo, FakeProfileRepository(), biometricAvailable = true)
        vm.onLanguageSelected("es")
        assertEquals("es", content(vm).selectedLanguage)
        assertEquals(LanguageConfig.SPANISH, repo.userData.value.appLanguage)
    }

    @Test
    fun onPushNotificationsToggled_flipsPersistedValue() = runTest {
        val repo = FakeUserDataRepository()
        val vm = SettingsViewModel(repo, FakeProfileRepository(), biometricAvailable = true)
        vm.onPushNotificationsToggled()
        assertFalse(content(vm).isPushNotificationsEnabled)
        assertFalse(repo.userData.value.isPushNotificationsEnabled)
    }

    @Test
    fun onTransactionAlertsToggled_flipsPersistedValue() = runTest {
        val repo = FakeUserDataRepository()
        val vm = SettingsViewModel(repo, FakeProfileRepository(), biometricAvailable = true)
        vm.onTransactionAlertsToggled()
        assertFalse(content(vm).isTransactionAlertsEnabled)
        assertFalse(repo.userData.value.isTransactionAlertsEnabled)
    }

    @Test
    fun onBiometricToggled_persistsBiometricFlag() = runTest {
        val repo = FakeUserDataRepository()
        val vm = SettingsViewModel(repo, FakeProfileRepository(), biometricAvailable = true)
        vm.onBiometricToggled()
        assertTrue(content(vm).isBiometricLoginEnabled)
        assertTrue(repo.userData.value.isBiometricsEnabled)
    }

    @Test
    fun biometricUnavailable_reflectedInState() = runTest {
        val vm = SettingsViewModel(FakeUserDataRepository(), FakeProfileRepository(), biometricAvailable = false)
        assertFalse(content(vm).isBiometricAvailableOnDevice)
    }

    @Test
    fun profileHeader_populatesFromRepository() = runTest {
        val vm = SettingsViewModel(FakeUserDataRepository(), FakeProfileRepository(), biometricAvailable = true)
        val s = content(vm)
        assertEquals("Aisha Saleh", s.profileName)
        assertEquals("aisha.s@example.com", s.profileEmail)
        assertEquals("AS", s.profileInitials)
    }

    @Test
    fun onSignOut_setsUnauthenticated() = runTest {
        val repo = FakeUserDataRepository(UserData.DEFAULT.copy(isAuthenticated = true))
        val vm = SettingsViewModel(repo, FakeProfileRepository(), biometricAvailable = true)
        vm.onSignOut()
        assertFalse(repo.userData.value.isAuthenticated)
    }
}
