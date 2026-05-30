/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.login.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.core.data.auth.ObpAuthRepository
import org.mifosx.openbanking.core.data.user.UserDataRepository
import org.mifosx.openbanking.core.model.obp.ObpException
import org.mifosx.openbanking.core.model.user.DarkThemeConfig
import org.mifosx.openbanking.core.model.user.LanguageConfig
import org.mifosx.openbanking.core.model.user.ThemeBrand
import org.mifosx.openbanking.core.model.user.UserData
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeObpAuthRepository(
    var loginResult: Result<Unit> = Result.success(Unit),
    var oidcResult: Result<Unit> = Result.success(Unit),
) : ObpAuthRepository {
    var lastUsername: String? = null
    override suspend fun login(username: String, password: String): Result<Unit> {
        lastUsername = username
        return loginResult
    }

    override suspend fun loginWithOidc(
        code: String,
        redirectUri: String,
        codeVerifier: String,
    ): Result<Unit> = oidcResult

    override suspend fun refreshSession(refreshToken: String): Result<Unit> = Result.success(Unit)
    override fun logout() = Unit
    override fun isLoggedIn(): Boolean = false
}

private class FakeUserDataRepository : UserDataRepository {
    var authenticated = false
    var unlocked = false

    override val userData: StateFlow<UserData> = MutableStateFlow(UserData.DEFAULT)
    override val authToken: String? = null
    override val passcode: String = ""
    override val observeLanguage: Flow<LanguageConfig> = flowOf(LanguageConfig.DEFAULT)
    override val observeDarkThemeConfig: Flow<DarkThemeConfig> =
        flowOf(DarkThemeConfig.FOLLOW_SYSTEM)
    override val observeDynamicColorPreference: Flow<Boolean> = flowOf(false)
    override val observeScreenCapturePreference: Flow<Boolean> = flowOf(false)

    override suspend fun setLanguage(language: LanguageConfig) = Unit
    override suspend fun setThemeBrand(themeBrand: ThemeBrand) = Unit
    override suspend fun setDarkThemeConfig(darkThemeConfig: DarkThemeConfig) = Unit
    override suspend fun setDynamicColorPreference(useDynamicColor: Boolean) = Unit
    override suspend fun setIsAuthenticated(isAuthenticated: Boolean) {
        authenticated = isAuthenticated
    }

    override suspend fun setIsUnlocked(isUnlocked: Boolean) {
        unlocked = isUnlocked
    }

    override suspend fun setIsPasscodeEnabled(isPasscodeEnabled: Boolean) = Unit
    override suspend fun setIsBiometricsEnabled(isBiometricsEnabled: Boolean) = Unit
    override suspend fun setShowOnboarding(showOnboarding: Boolean) = Unit
    override suspend fun setFirstTimeState(firstTimeState: Boolean) = Unit
    override suspend fun setPasscode(passcode: String) = Unit
    override suspend fun clearUserData() = Unit
}

class LoginViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        authRepository: ObpAuthRepository = FakeObpAuthRepository(),
        userDataRepository: UserDataRepository = FakeUserDataRepository(),
    ) = LoginViewModel(authRepository, userDataRepository)

    @Test
    fun credentialEntryUpdatesState() = runTest {
        val vm = viewModel()
        vm.trySendAction(LoginAction.UsernameChanged("alice"))
        vm.trySendAction(LoginAction.PasswordChanged("secret"))

        val state = vm.stateFlow.value
        assertEquals("alice", state.username)
        assertEquals("secret", state.password)
        assertTrue(state.isFormValid)
    }

    @Test
    fun directLoginDisabledUntilBothFieldsPresent() = runTest {
        val vm = viewModel()
        assertFalse(vm.stateFlow.value.isFormValid)
        vm.trySendAction(LoginAction.UsernameChanged("alice"))
        assertFalse(vm.stateFlow.value.isFormValid)
    }

    @Test
    fun directLoginSuccessMarksSessionAuthenticated() = runTest {
        val auth = FakeObpAuthRepository(loginResult = Result.success(Unit))
        val userData = FakeUserDataRepository()
        val vm = viewModel(auth, userData)

        vm.trySendAction(LoginAction.UsernameChanged("alice"))
        vm.trySendAction(LoginAction.PasswordChanged("secret"))
        vm.trySendAction(LoginAction.DirectLoginClicked)

        assertEquals("alice", auth.lastUsername)
        assertTrue(userData.authenticated)
        assertTrue(userData.unlocked)
        assertNull(vm.stateFlow.value.errorMessage)
    }

    @Test
    fun directLoginUnauthorizedShowsInvalidCredentialsMessage() = runTest {
        val auth = FakeObpAuthRepository(
            loginResult = Result.failure(ObpException(reason = "UNAUTHORIZED")),
        )
        val userData = FakeUserDataRepository()
        val vm = viewModel(auth, userData)

        vm.trySendAction(LoginAction.UsernameChanged("alice"))
        vm.trySendAction(LoginAction.PasswordChanged("wrong"))
        vm.trySendAction(LoginAction.DirectLoginClicked)

        val state = vm.stateFlow.value
        assertEquals(
            "Invalid username or password. Please check your credentials and try again.",
            state.errorMessage,
        )
        assertFalse(state.isLoading)
        assertFalse(userData.authenticated)
    }

    @Test
    fun serverErrorShowsServerMessage() = runTest {
        val auth = FakeObpAuthRepository(
            loginResult = Result.failure(ObpException(reason = "SERVER")),
        )
        val vm = viewModel(auth, FakeUserDataRepository())

        vm.trySendAction(LoginAction.UsernameChanged("alice"))
        vm.trySendAction(LoginAction.PasswordChanged("secret"))
        vm.trySendAction(LoginAction.DirectLoginClicked)

        assertEquals(
            "Something went wrong on our end. Please try again in a moment.",
            vm.stateFlow.value.errorMessage,
        )
    }

    @Test
    fun forgotPasswordEmitsNavigationEvent() = runTest {
        val vm = viewModel()
        var navigated = false
        // Collect the next event without blocking the test dispatcher indefinitely.
        val job = CoroutineScope(dispatcher).launch {
            vm.eventFlow.collect { if (it is LoginEvent.NavigateToForgotPassword) navigated = true }
        }
        vm.trySendAction(LoginAction.ForgotPasswordClicked)
        job.cancel()
        assertTrue(navigated)
    }

    @Test
    fun passwordVisibilityToggles() = runTest {
        val vm = viewModel()
        assertFalse(vm.stateFlow.value.isPasswordVisible)
        vm.trySendAction(LoginAction.PasswordVisibilityToggled)
        assertTrue(vm.stateFlow.value.isPasswordVisible)
    }
}
