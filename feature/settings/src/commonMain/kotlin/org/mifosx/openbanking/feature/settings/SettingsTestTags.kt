/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifosx.openbanking.feature.settings

/**
 * Stable UI test tags for the Settings screen. Values mirror the component ids in
 * `idea-layer/screens/settings/ui.yaml` so design ↔ test traceability is one-to-one.
 */
object SettingsTestTags {
    const val ROOT = "settings_root"
    const val PROFILE_HEADER = "settings_profile_header"

    // Appearance
    const val THEME_MODE_SYSTEM = "settings_theme_mode_system"
    const val THEME_MODE_LIGHT = "settings_theme_mode_light"
    const val THEME_MODE_DARK = "settings_theme_mode_dark"
    const val LANGUAGE_SELECT = "settings_language_select"

    // Security
    const val BIOMETRIC_TOGGLE = "settings_biometric_toggle"
    const val CHANGE_PASSWORD_ROW = "settings_change_password_row"

    // About
    const val ABOUT_LINK = "settings_about_link"
    const val TERMS_LINK = "settings_terms_link"
    const val PRIVACY_LINK = "settings_privacy_link"
    const val LICENSES_LINK = "settings_licenses_link"
    const val APP_VERSION_VALUE = "settings_app_version_value"

    const val SIGN_OUT = "settings_sign_out"
}
