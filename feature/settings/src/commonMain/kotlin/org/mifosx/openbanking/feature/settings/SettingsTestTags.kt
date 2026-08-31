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

/**
 * `testTag` values for the settings and licences screens, shared by the desktop and Robolectric
 * suites.
 *
 * [SECTION] and [ROW] are carried by every section and every row in addition to that node's own
 * specific tag, so a suite can count them.
 */
internal object SettingsTestTags {

    const val CONTENT = "settings:content"

    const val SECTION = "settings:section"
    const val SECTION_TITLE = "settings:sectionTitle"
    const val SECTION_APPEARANCE = "settings:section:appearance"
    const val SECTION_ACCOUNT = "settings:section:account"
    const val SECTION_ABOUT = "settings:section:about"

    const val ROW = "settings:row"

    const val THEME_ROW = "settings:row:theme"
    const val THEME_VALUE = "settings:themeValue"
    const val THEME_DROPDOWN = "settings:themeDropdown"
    const val THEME_MENU = "settings:themeMenu"

    const val CONSENTS_ROW = "settings:row:consents"

    const val PRIVACY_ROW = "settings:row:privacy"
    const val LICENCES_ROW = "settings:row:licences"

    const val LICENCES_SCREEN = "settings:licences:screen"
    const val LICENCES_LIST = "settings:licences:list"

    /** Tag for one theme option inside the picker, keyed by the config it selects. */
    fun themeOption(config: DarkThemeConfig): String = "settings:themeOption:${config.name}"

    /** Tag for a row's chevron, marking it as opening a destination inside the app. */
    fun chevron(rowTag: String): String = "$rowTag:chevron"

    /** Tag for a row's external-link glyph, marking it as leaving the app. */
    fun externalLink(rowTag: String): String = "$rowTag:externalLink"
}
