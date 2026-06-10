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

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.mifosx.openbanking.core.data.auth.AuthRecoveryRepository
import org.mifosx.openbanking.core.data.auth.ObpAuthRepository
import org.mifosx.openbanking.core.data.profile.ProfileRepository
import org.mifosx.openbanking.core.data.user.UserDataRepository
import org.mifosx.openbanking.core.model.user.DarkThemeConfig
import org.mifosx.openbanking.core.model.user.LanguageConfig
import org.mifosx.openbanking.core.model.user.UserData
import org.mifosx.openbanking.feature.settings.isBiometricAvailableOnDevice
import template.core.base.store.screen.DataFreshness
import template.core.base.store.screen.ScreenState

/**
 * Settings ViewModel — surfaces the profile header + user-configurable preferences (Appearance,
 * Security, About) as a single [ScreenState], styled to the rendered design preview
 * (`idea-layer/screens/settings/preview`).
 *
 * Preference state is the single source of truth in [UserDataRepository.userData]; the UI state is
 * derived reactively, so toggles persist via the repository and the new value flows straight back
 * into [uiState]. The profile header (name/email/initials) is loaded once from
 * [ProfileRepository.current] and merged in. Screen navigation (Profile, Change Password, …) is
 * driven by the screen via NavController callbacks.
 *
 * @param biometricAvailable coarse platform-capability flag gating the Biometric Login toggle.
 * @param appVersion build version string shown in the footer.
 */
class SettingsViewModel(
    private val userDataRepository: UserDataRepository,
    private val profileRepository: ProfileRepository,
    private val authRecoveryRepository: AuthRecoveryRepository,
    private val obpAuthRepository: ObpAuthRepository,
    private val biometricAvailable: Boolean = isBiometricAvailableOnDevice(),
    private val appVersion: String = "v1.0.0",
) : ViewModel() {

    private val profileFlow = MutableStateFlow(ProfileHeader())

    private val _uiState = MutableStateFlow<ScreenState<SettingsUiState>>(ScreenState.Loading)
    val uiState: StateFlow<ScreenState<SettingsUiState>> = _uiState.asStateFlow()

    // OBP has no authenticated change-password; the only path is an email-based reset, which needs
    // both the username and email of the signed-in user (captured from the loaded profile).
    private var resetUsername: String = ""
    private var resetEmail: String = ""

    private val resetMessageState = MutableStateFlow<String?>(null)
    val resetMessage: StateFlow<String?> = resetMessageState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(userDataRepository.userData, profileFlow) { data, profile ->
                ScreenState.Content(
                    data.toUiState(biometricAvailable, appVersion, profile),
                    DataFreshness.FRESH,
                )
            }.collect { _uiState.value = it }
        }
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            profileRepository.current()
                .onSuccess { profile ->
                    val name = profile.username.ifBlank { profile.email.substringBefore('@') }
                        .ifBlank { "Mifos User" }
                    resetUsername = profile.username
                    resetEmail = profile.email
                    profileFlow.value = ProfileHeader(
                        name = name,
                        email = profile.email,
                        initials = initialsOf(name),
                    )
                }
                .onFailure {
                    // Keep a graceful fallback header; Settings preferences still work offline.
                    profileFlow.value = ProfileHeader(name = "Mifos User", email = "", initials = "M")
                }
        }
    }

    /** Persists the chosen theme mode; the new value flows back into [uiState] reactively. */
    fun onThemeConfigSelected(config: DarkThemeConfig) {
        viewModelScope.launch { userDataRepository.setDarkThemeConfig(config) }
    }

    fun onLanguageSelected(language: String) {
        val config = LanguageConfig.entries.firstOrNull { it.localeName == language }
            ?: LanguageConfig.DEFAULT
        viewModelScope.launch { userDataRepository.setLanguage(config) }
    }

    fun onBiometricToggled() {
        val enabled = userDataRepository.userData.value.isBiometricsEnabled
        viewModelScope.launch { userDataRepository.setIsBiometricsEnabled(!enabled) }
    }

    /**
     * Signs the user out and wipes the local session: clears the held OBP token (DirectLogin or the
     * OIDC `Bearer` access token) from the token provider, then resets all persisted user data to
     * defaults. `RootNavViewModel` observes [UserDataRepository.userData] and routes back to auth.
     */
    fun onSignOut() {
        obpAuthRepository.logout()
        viewModelScope.launch { userDataRepository.clearUserData() }
    }

    /**
     * Requests a password-reset email for the signed-in user. OBP completes the reset via the emailed
     * link, so this only kicks off the email; the result is surfaced through [resetMessage].
     */
    fun onResetPassword() {
        val username = resetUsername
        val email = resetEmail
        if (username.isBlank() || email.isBlank()) {
            resetMessageState.value = "We couldn't read your profile. Please try again."
            return
        }
        viewModelScope.launch {
            authRecoveryRepository.initiateReset(username, email)
                .onSuccess {
                    resetMessageState.value =
                        "If your account is valid, a password reset link has been emailed to $email."
                }
                .onFailure {
                    resetMessageState.value = "Couldn't start a password reset. Please try again."
                }
        }
    }

    fun onResetMessageConsumed() {
        resetMessageState.value = null
    }
}

/** Profile header summary shown at the top of Settings. */
@Immutable
data class ProfileHeader(
    val name: String = "",
    val email: String = "",
    val initials: String = "",
)

/** Immutable settings screen state. */
@Immutable
data class SettingsUiState(
    val profileName: String = "",
    val profileEmail: String = "",
    val profileInitials: String = "",
    val themeConfig: DarkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
    val selectedLanguage: String = "en",
    val isBiometricLoginEnabled: Boolean = false,
    val isBiometricAvailableOnDevice: Boolean = false,
    val appVersion: String = "v1.0.0",
    val isLoading: Boolean = false,
)

internal fun UserData.toUiState(
    biometricAvailable: Boolean,
    appVersion: String,
    profile: ProfileHeader,
): SettingsUiState = SettingsUiState(
    profileName = profile.name,
    profileEmail = profile.email,
    profileInitials = profile.initials,
    themeConfig = darkThemeConfig,
    selectedLanguage = appLanguage.localeName ?: "en",
    isBiometricLoginEnabled = isBiometricsEnabled,
    isBiometricAvailableOnDevice = biometricAvailable,
    appVersion = appVersion,
    isLoading = false,
)

/** Up to two uppercase initials from a display name; "M" fallback when no name is available. */
internal fun initialsOf(name: String): String =
    name.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")
        .ifBlank { "M" }
