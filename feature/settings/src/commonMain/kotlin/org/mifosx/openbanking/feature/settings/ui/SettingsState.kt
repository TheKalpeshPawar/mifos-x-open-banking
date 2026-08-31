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

import org.jetbrains.compose.resources.StringResource
import org.mifosx.openbanking.core.model.user.DarkThemeConfig
import org.mifosx.openbanking.feature.settings.generated.resources.Res
import org.mifosx.openbanking.feature.settings.generated.resources.feature_settings_theme_dark
import org.mifosx.openbanking.feature.settings.generated.resources.feature_settings_theme_follow_system
import org.mifosx.openbanking.feature.settings.generated.resources.feature_settings_theme_light

/**
 * Screen state for the settings hub.
 *
 * @property themeConfig The stored theme preference, and the option the picker marks as selected.
 * @property themeLabel The label resource for [themeConfig], resolved by the composable.
 * @property isThemeMenuExpanded Whether the theme dropdown is open.
 */
data class SettingsState(
    val themeConfig: DarkThemeConfig,
    val themeLabel: StringResource = Res.string.feature_settings_theme_follow_system,
    val isThemeMenuExpanded: Boolean = false,
)

/** Actions the view model owns. Navigation is the screen's lambdas, not routed here. */
sealed interface SettingsAction {
    /** Store [config] as the theme preference. */
    data class SelectTheme(val config: DarkThemeConfig) : SettingsAction

    /** Open the theme dropdown if closed, close it if open. */
    data object ToggleThemeMenu : SettingsAction

    /** Close the theme dropdown. */
    data object DismissThemeMenu : SettingsAction
}

/** The label resource for one theme option, used by both the row's value and the picker. */
internal fun DarkThemeConfig.themeLabel(): StringResource = when (this) {
    DarkThemeConfig.FOLLOW_SYSTEM -> Res.string.feature_settings_theme_follow_system
    DarkThemeConfig.LIGHT -> Res.string.feature_settings_theme_light
    DarkThemeConfig.DARK -> Res.string.feature_settings_theme_dark
}
