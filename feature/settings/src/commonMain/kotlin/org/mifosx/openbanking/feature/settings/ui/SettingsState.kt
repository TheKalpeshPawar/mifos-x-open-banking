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
 * @property uiState What the screen currently renders.
 */
data class SettingsState(
    val uiState: SettingsUiState,
)

/**
 * The three rendered states.
 *
 * There is no Loading member. Preferences resolve from local storage, and every field already has
 * a declared default, so the screen opens on [Content] with those defaults and swaps in the stored
 * values when the flow emits. A skeleton for a read that completes in the same frame would only
 * flicker.
 */
sealed interface SettingsUiState {

    /**
     * @property themeConfig The stored preference, and the option the picker marks as selected.
     * @property themeLabel The resource rendering [themeConfig] — resolved by the composable so the
     *   label follows the device locale rather than being frozen at the moment the state was built.
     * @property appVersionLabel Injected build identity, shown verbatim.
     * @property isThemeMenuExpanded Whether the theme dropdown is open. Held in state rather than
     *   in the composable so the picker's openness survives recomposition and is assertable.
     * @property selectedAccountId Carried so the Profile row can hand it to the host's navigation
     *   lambda; the settings screen itself never reads it.
     */
    data class Content(
        val themeConfig: DarkThemeConfig,
        val themeLabel: StringResource,
        val appVersionLabel: String,
        val isThemeMenuExpanded: Boolean = false,
        val selectedAccountId: String = "",
    ) : SettingsUiState

    data object Empty : SettingsUiState

    data class Error(val kind: SettingsErrorKind) : SettingsUiState
}

/**
 * The two failure modes the screen distinguishes.
 *
 * Both are retriable. A preference store that would not answer once is far more often busy than
 * broken, and withholding Retry would strand the user on a screen whose whole content is local.
 */
enum class SettingsErrorKind(val isRetriable: Boolean) {
    PreferencesUnavailable(isRetriable = true),
    Unknown(isRetriable = true),
}

/** Actions the view model owns. Navigation is the screen's lambdas, not routed here. */
sealed interface SettingsAction {
    data class SelectTheme(val config: DarkThemeConfig) : SettingsAction
    data object ToggleThemeMenu : SettingsAction
    data object DismissThemeMenu : SettingsAction
    data object RetryLoad : SettingsAction
}

/**
 * Classifies a preference-stream failure into one of the [SettingsErrorKind]s.
 *
 * An [Exception] reaching here came from the read itself — a store that could not be opened or
 * decoded — and is reported as such. Anything that is not an [Exception] is a fault in the host
 * process rather than in the preference store, so it is not described to the user as one.
 */
internal fun classifySettingsError(throwable: Throwable): SettingsErrorKind = when (throwable) {
    is Exception -> SettingsErrorKind.PreferencesUnavailable
    else -> SettingsErrorKind.Unknown
}

/** The label resource for one theme option, used by both the row's value and the picker. */
internal fun DarkThemeConfig.labelResource(): StringResource = when (this) {
    DarkThemeConfig.FOLLOW_SYSTEM -> Res.string.feature_settings_theme_follow_system
    DarkThemeConfig.LIGHT -> Res.string.feature_settings_theme_light
    DarkThemeConfig.DARK -> Res.string.feature_settings_theme_dark
}
