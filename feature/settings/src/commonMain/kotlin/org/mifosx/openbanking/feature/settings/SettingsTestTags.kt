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
 * Stable `testTag` values for the settings screen, shared by the desktop, Robolectric and
 * instrumented suites so all three drive the same nodes.
 *
 * Append-only: a tag that an existing test references must not be renamed or removed without a
 * matching `uitest-tag-retire` note, or the suites silently stop asserting what they claim to.
 *
 * [SECTION] and [ROW] are carried by every section and every row in addition to that node's own
 * specific tag. Counting them is how the suites pin the screen's shape — a section or row added
 * back that this screen deliberately does not offer fails on the count rather than needing a tag
 * of its own for something that should not exist.
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
    const val APP_VERSION_ROW = "settings:row:appVersion"
    const val APP_VERSION_VALUE = "settings:appVersionValue"

    const val LICENCES_SCREEN = "settings:licences:screen"
    const val LICENCES_LIST = "settings:licences:list"

    const val EMPTY_STATE = "settings:emptyState"
    const val EMPTY_TITLE = "settings:emptyTitle"
    const val EMPTY_BODY = "settings:emptyBody"

    const val ERROR_STATE = "settings:errorState"
    const val ERROR_TITLE = "settings:errorTitle"
    const val ERROR_BODY = "settings:errorBody"
    const val RETRY_BUTTON = "settings:retryButton"

    /** Tag for one theme option inside the picker, keyed by the config it selects. */
    fun themeOption(config: DarkThemeConfig): String = "settings:themeOption:${config.name}"

    /** Tag for a row's chevron, marking it as opening a destination inside the app. */
    fun chevron(rowTag: String): String = "$rowTag:chevron"

    /** Tag for a row's external-link glyph, marking it as leaving the app. */
    fun externalLink(rowTag: String): String = "$rowTag:externalLink"
}
