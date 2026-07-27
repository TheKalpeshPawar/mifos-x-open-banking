/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.settings

import org.mifosx.openbanking.core.model.user.DarkThemeConfig
import org.mifosx.openbanking.feature.settings.ui.SettingsErrorKind
import org.mifosx.openbanking.feature.settings.ui.SettingsState
import org.mifosx.openbanking.feature.settings.ui.SettingsUiState
import org.mifosx.openbanking.feature.settings.ui.labelResource

/**
 * The settings states the suites render, carrying the values the design was drawn against:
 * the System theme and the shipped build identity.
 *
 * Shared by the view-model and Compose suites so both assert against one screen rather than two
 * that happen to look alike.
 */
object SettingsFixtures {

    const val APP_VERSION: String = "0.1.0 (build 1)"

    /** How many rows the screen offers in total: theme, one account row, three about rows. */
    const val EXPECTED_ROW_COUNT: Int = 5

    /** Appearance, Account, About & Legal — Security and Notifications are not offered. */
    const val EXPECTED_SECTION_COUNT: Int = 3

    fun contentState(
        themeConfig: DarkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
        isThemeMenuExpanded: Boolean = false,
    ): SettingsState = SettingsState(
        uiState = SettingsUiState.Content(
            themeConfig = themeConfig,
            themeLabel = themeConfig.labelResource(),
            appVersionLabel = APP_VERSION,
            isThemeMenuExpanded = isThemeMenuExpanded,
        ),
    )

    fun errorState(
        kind: SettingsErrorKind = SettingsErrorKind.PreferencesUnavailable,
    ): SettingsState = SettingsState(uiState = SettingsUiState.Error(kind))

    fun emptyState(): SettingsState = SettingsState(uiState = SettingsUiState.Empty)
}
