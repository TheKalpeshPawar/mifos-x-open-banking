/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.settings.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mifosx.openbanking.core.model.user.DarkThemeConfig
import org.mifosx.openbanking.feature.settings.FakeUserDataRepository
import org.mifosx.openbanking.feature.settings.SettingsFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Covers [SettingsViewModel]'s preference reads, theme writes and picker state.
 *
 * Test names are camelCase, not backticked: this source set also compiles for Kotlin/Native, whose
 * frontend rejects the parentheses and commas a prose-style backticked name would carry.
 */
class SettingsViewModelTest {

    private fun viewModel(
        repository: FakeUserDataRepository = FakeUserDataRepository(),
        appVersion: String = SettingsFixtures.APP_VERSION,
    ): SettingsViewModel {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        return SettingsViewModel(
            userDataRepository = repository,
            appVersion = SettingsAppVersion(appVersion),
        )
    }

    private fun content(viewModel: SettingsViewModel): SettingsUiState.Content =
        assertIs<SettingsUiState.Content>(viewModel.stateFlow.value.uiState)

    @Test
    fun initialStateIsContentRatherThanLoading() {
        val state = viewModel().stateFlow.value.uiState
        assertIs<SettingsUiState.Content>(state)
    }

    @Test
    fun contentReflectsTheStoredTheme() {
        val repository = FakeUserDataRepository(initialTheme = DarkThemeConfig.DARK)
        assertEquals(DarkThemeConfig.DARK, content(viewModel(repository)).themeConfig)
    }

    @Test
    fun selectingFollowSystemWritesIt() = runTest {
        val repository = FakeUserDataRepository(initialTheme = DarkThemeConfig.DARK)
        val vm = viewModel(repository)

        vm.trySendAction(SettingsAction.SelectTheme(DarkThemeConfig.FOLLOW_SYSTEM))

        assertEquals(listOf(DarkThemeConfig.FOLLOW_SYSTEM), repository.writtenThemes)
    }

    @Test
    fun selectingLightWritesIt() = runTest {
        val repository = FakeUserDataRepository()
        val vm = viewModel(repository)

        vm.trySendAction(SettingsAction.SelectTheme(DarkThemeConfig.LIGHT))

        assertEquals(listOf(DarkThemeConfig.LIGHT), repository.writtenThemes)
    }

    @Test
    fun selectingDarkWritesIt() = runTest {
        val repository = FakeUserDataRepository()
        val vm = viewModel(repository)

        vm.trySendAction(SettingsAction.SelectTheme(DarkThemeConfig.DARK))

        assertEquals(listOf(DarkThemeConfig.DARK), repository.writtenThemes)
    }

    /**
     * A picker that skips the write when the value is unchanged would silently do nothing after
     * any drift between stored and displayed state.
     */
    @Test
    fun selectingTheAlreadySelectedThemeStillWritesExactlyOnce() = runTest {
        val repository = FakeUserDataRepository(initialTheme = DarkThemeConfig.LIGHT)
        val vm = viewModel(repository)

        vm.trySendAction(SettingsAction.SelectTheme(DarkThemeConfig.LIGHT))

        assertEquals(listOf(DarkThemeConfig.LIGHT), repository.writtenThemes)
    }

    /** Pins the round trip: a write that never re-reads would leave the state on the old value. */
    @Test
    fun aWriteIsReflectedBackThroughTheObservedFlow() = runTest {
        val repository = FakeUserDataRepository(initialTheme = DarkThemeConfig.FOLLOW_SYSTEM)
        val vm = viewModel(repository)

        vm.trySendAction(SettingsAction.SelectTheme(DarkThemeConfig.DARK))

        assertEquals(DarkThemeConfig.DARK, content(vm).themeConfig)
    }

    @Test
    fun anExternalWriteIsReflectedBackThroughTheObservedFlow() = runTest {
        val repository = FakeUserDataRepository()
        val vm = viewModel(repository)

        repository.emitTheme(DarkThemeConfig.LIGHT)

        assertEquals(DarkThemeConfig.LIGHT, content(vm).themeConfig)
    }

    @Test
    fun threeSuccessiveWritesArriveInOrder() = runTest {
        val repository = FakeUserDataRepository()
        val vm = viewModel(repository)

        vm.trySendAction(SettingsAction.SelectTheme(DarkThemeConfig.DARK))
        vm.trySendAction(SettingsAction.SelectTheme(DarkThemeConfig.LIGHT))
        vm.trySendAction(SettingsAction.SelectTheme(DarkThemeConfig.FOLLOW_SYSTEM))

        assertEquals(
            listOf(DarkThemeConfig.DARK, DarkThemeConfig.LIGHT, DarkThemeConfig.FOLLOW_SYSTEM),
            repository.writtenThemes,
        )
    }

    /**
     * Construction must not clobber the stored preference. A view model that wrote its own default
     * on the way up would reset every user's theme on the first visit to this screen.
     */
    @Test
    fun nothingIsWrittenBeforeAnyActionIsDispatched() = runTest {
        val repository = FakeUserDataRepository(initialTheme = DarkThemeConfig.DARK)
        viewModel(repository)

        assertEquals(emptyList(), repository.writtenThemes)
    }

    @Test
    fun everyDarkThemeConfigEntryIsOfferedByThePicker() {
        assertEquals(3, DarkThemeConfig.entries.size)
    }

    @Test
    fun themeMenuStartsCollapsed() {
        assertFalse(content(viewModel()).isThemeMenuExpanded)
    }

    @Test
    fun toggleExpandsTheThemeMenu() = runTest {
        val vm = viewModel()

        vm.trySendAction(SettingsAction.ToggleThemeMenu)

        assertTrue(content(vm).isThemeMenuExpanded)
    }

    @Test
    fun dismissCollapsesTheThemeMenu() = runTest {
        val vm = viewModel()

        vm.trySendAction(SettingsAction.ToggleThemeMenu)
        vm.trySendAction(SettingsAction.DismissThemeMenu)

        assertFalse(content(vm).isThemeMenuExpanded)
    }

    @Test
    fun selectingAThemeCollapsesTheMenu() = runTest {
        val vm = viewModel()

        vm.trySendAction(SettingsAction.ToggleThemeMenu)
        vm.trySendAction(SettingsAction.SelectTheme(DarkThemeConfig.DARK))

        assertFalse(content(vm).isThemeMenuExpanded)
    }

    @Test
    fun aReadFailureRendersTheErrorState() = runTest {
        val repository = FakeUserDataRepository()
        repository.failNextRead()

        val state = viewModel(repository).stateFlow.value.uiState

        val error = assertIs<SettingsUiState.Error>(state)
        assertEquals(SettingsErrorKind.PreferencesUnavailable, error.kind)
    }

    @Test
    fun retryReReadsThePreferenceStoreAndRecovers() = runTest {
        val repository = FakeUserDataRepository(initialTheme = DarkThemeConfig.LIGHT)
        repository.failNextRead()
        val vm = viewModel(repository)
        assertIs<SettingsUiState.Error>(vm.stateFlow.value.uiState)

        vm.trySendAction(SettingsAction.RetryLoad)

        assertEquals(2, repository.readCount)
        assertEquals(DarkThemeConfig.LIGHT, content(vm).themeConfig)
    }

    @Test
    fun appVersionComesFromTheInjectedValueRatherThanALiteral() {
        val vm = viewModel(appVersion = "9.9.9 (build 42)")
        assertEquals("9.9.9 (build 42)", content(vm).appVersionLabel)
    }

    @Test
    fun selectedAccountIdIsCarriedFromStoredUserData() {
        val repository = FakeUserDataRepository(initialAccountId = SettingsFixtures.ACCOUNT_ID)
        assertEquals(SettingsFixtures.ACCOUNT_ID, content(viewModel(repository)).selectedAccountId)
    }

    @Test
    fun theThemeLabelTracksTheStoredTheme() {
        val repository = FakeUserDataRepository(initialTheme = DarkThemeConfig.DARK)
        assertEquals(DarkThemeConfig.DARK.labelResource(), content(viewModel(repository)).themeLabel)
    }
}
