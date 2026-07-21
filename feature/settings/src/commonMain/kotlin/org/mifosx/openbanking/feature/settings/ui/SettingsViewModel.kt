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

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.mifosx.openbanking.core.data.user.UserDataRepository
import org.mifosx.openbanking.core.model.user.DarkThemeConfig
import org.mifosx.openbanking.core.model.user.UserData
import template.core.base.ui.viewmodel.BaseViewModel

/**
 * Drives the settings hub: the theme preference and the rows that hand navigation back to the host.
 *
 * Reads and writes go through [UserDataRepository] rather than the preference store beneath it —
 * a feature module depends on `core/data`, never on `core/datastore`.
 *
 * The screen opens on [SettingsUiState.Content] carrying declared defaults, so nothing is written
 * at construction: the first write this view model makes is the one the user asked for by picking
 * a theme.
 */
class SettingsViewModel(
    private val userDataRepository: UserDataRepository,
    private val appVersion: SettingsAppVersion,
) : BaseViewModel<SettingsState, Nothing, SettingsAction>(
    initialState = SettingsState(
        uiState = SettingsUiState.Content(
            themeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
            themeLabel = DarkThemeConfig.FOLLOW_SYSTEM.labelResource(),
            appVersionLabel = appVersion.label,
        ),
    ),
) {

    /**
     * Bumped by [SettingsAction.RetryLoad] to re-subscribe. A failed read terminates its flow, so
     * retry has to build a new one rather than nudge the old one.
     */
    private val reads = MutableStateFlow(0)

    init {
        @OptIn(ExperimentalCoroutinesApi::class)
        reads
            .flatMapLatest { preferences() }
            .onEach { preference -> updateState { copy(uiState = preference.toUiState()) } }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SelectTheme -> selectTheme(action.config)
            SettingsAction.ToggleThemeMenu -> setThemeMenuExpanded(!isThemeMenuExpanded())
            SettingsAction.DismissThemeMenu -> setThemeMenuExpanded(false)
            SettingsAction.RetryLoad -> reads.value += 1
        }
    }

    /**
     * Writes unconditionally, including when the picked theme is the one already stored.
     *
     * Guarding on equality would make the write depend on this view model's own idea of the
     * current value; a picker that re-asserts what the user chose is cheap, and one that silently
     * does nothing after a state drift is a defect with no symptom.
     */
    private fun selectTheme(config: DarkThemeConfig) {
        setThemeMenuExpanded(false)
        viewModelScope.launch { userDataRepository.setDarkThemeConfig(config) }
    }

    private fun isThemeMenuExpanded(): Boolean =
        (state.uiState as? SettingsUiState.Content)?.isThemeMenuExpanded == true

    private fun setThemeMenuExpanded(expanded: Boolean) = updateState {
        val current = uiState
        if (current is SettingsUiState.Content) {
            copy(uiState = current.copy(isThemeMenuExpanded = expanded))
        } else {
            this
        }
    }

    /**
     * The stored theme alongside the selected account, as one emission.
     *
     * A read failure is caught here rather than at the collector so the error is scoped to this
     * subscription — [reads] can then replace it wholesale on retry.
     */
    private fun preferences(): Flow<StoredPreferences> =
        combine<DarkThemeConfig, UserData, StoredPreferences>(
            userDataRepository.observeDarkThemeConfig,
            userDataRepository.userData,
        ) { theme, user ->
            StoredPreferences.Loaded(
                themeConfig = theme,
                selectedAccountId = user.selectedAccountId,
            )
        }.catch { throwable -> emit(StoredPreferences.Failed(classifySettingsError(throwable))) }

    private fun StoredPreferences.toUiState(): SettingsUiState = when (this) {
        is StoredPreferences.Loaded -> SettingsUiState.Content(
            themeConfig = themeConfig,
            themeLabel = themeConfig.labelResource(),
            appVersionLabel = appVersion.label,
            isThemeMenuExpanded = isThemeMenuExpanded(),
            selectedAccountId = selectedAccountId,
        )

        is StoredPreferences.Failed -> SettingsUiState.Error(kind)
    }
}

/**
 * The build identity shown on the App Version row.
 *
 * A wrapper rather than a bare `String` so Koin can bind it unambiguously, and so tests can state
 * the version they expect instead of asserting against whatever the build stamped.
 */
data class SettingsAppVersion(val label: String)

/** One resolved read of the preference store, or the failure that ended it. */
private sealed interface StoredPreferences {

    data class Loaded(
        val themeConfig: DarkThemeConfig,
        val selectedAccountId: String,
    ) : StoredPreferences

    data class Failed(val kind: SettingsErrorKind) : StoredPreferences
}
