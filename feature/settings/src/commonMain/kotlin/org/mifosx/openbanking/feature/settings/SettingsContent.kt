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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.model.user.DarkThemeConfig
import org.mifosx.openbanking.feature.settings.components.SettingsRow
import org.mifosx.openbanking.feature.settings.components.SettingsRowChevron
import org.mifosx.openbanking.feature.settings.components.SettingsRowExternalLink
import org.mifosx.openbanking.feature.settings.components.SettingsSection
import org.mifosx.openbanking.feature.settings.components.ThemeDropdownRow
import org.mifosx.openbanking.feature.settings.generated.resources.Res
import org.mifosx.openbanking.feature.settings.generated.resources.feature_settings_app_version_title
import org.mifosx.openbanking.feature.settings.generated.resources.feature_settings_consents_subtitle
import org.mifosx.openbanking.feature.settings.generated.resources.feature_settings_consents_title
import org.mifosx.openbanking.feature.settings.generated.resources.feature_settings_content_accessibility
import org.mifosx.openbanking.feature.settings.generated.resources.feature_settings_licences_title
import org.mifosx.openbanking.feature.settings.generated.resources.feature_settings_opens_externally_accessibility
import org.mifosx.openbanking.feature.settings.generated.resources.feature_settings_opens_in_app_accessibility
import org.mifosx.openbanking.feature.settings.generated.resources.feature_settings_privacy_title
import org.mifosx.openbanking.feature.settings.generated.resources.feature_settings_profile_subtitle
import org.mifosx.openbanking.feature.settings.generated.resources.feature_settings_profile_title
import org.mifosx.openbanking.feature.settings.generated.resources.feature_settings_section_about
import org.mifosx.openbanking.feature.settings.generated.resources.feature_settings_section_account
import org.mifosx.openbanking.feature.settings.generated.resources.feature_settings_section_appearance

/**
 * The settings body: Appearance, Account, and About & Legal, in that order.
 *
 * A plain scrolling column rather than a lazy list — the row count is fixed and small, and every
 * row is composed anyway, so laziness would buy nothing while costing the ability to assert on
 * off-screen rows without scrolling to them first.
 */
@Composable
@Suppress("LongParameterList")
internal fun SettingsContent(
    themeConfig: DarkThemeConfig,
    appVersionLabel: String,
    isThemeMenuExpanded: Boolean,
    onSelectTheme: (DarkThemeConfig) -> Unit,
    onToggleThemeMenu: () -> Unit,
    onDismissThemeMenu: () -> Unit,
    onNavigateToConsents: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onNavigateToLicences: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(Res.string.feature_settings_content_accessibility)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .testTag(SettingsTestTags.CONTENT)
            .semantics { contentDescription = description },
    ) {
        AppearanceSection(
            themeConfig = themeConfig,
            isThemeMenuExpanded = isThemeMenuExpanded,
            onSelectTheme = onSelectTheme,
            onToggleThemeMenu = onToggleThemeMenu,
            onDismissThemeMenu = onDismissThemeMenu,
        )
        AccountSection(
            onNavigateToConsents = onNavigateToConsents,
            onNavigateToProfile = onNavigateToProfile,
        )
        AboutSection(
            appVersionLabel = appVersionLabel,
            onOpenPrivacy = onOpenPrivacy,
            onNavigateToLicences = onNavigateToLicences,
        )
    }
}

@Composable
private fun AppearanceSection(
    themeConfig: DarkThemeConfig,
    isThemeMenuExpanded: Boolean,
    onSelectTheme: (DarkThemeConfig) -> Unit,
    onToggleThemeMenu: () -> Unit,
    onDismissThemeMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSection(
        title = stringResource(Res.string.feature_settings_section_appearance),
        testTag = SettingsTestTags.SECTION_APPEARANCE,
        modifier = modifier,
    ) {
        ThemeDropdownRow(
            selected = themeConfig,
            expanded = isThemeMenuExpanded,
            onToggle = onToggleThemeMenu,
            onDismiss = onDismissThemeMenu,
            onSelect = onSelectTheme,
        )
    }
}

@Composable
private fun AccountSection(
    onNavigateToConsents: () -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val opensInApp = stringResource(Res.string.feature_settings_opens_in_app_accessibility)
    SettingsSection(
        title = stringResource(Res.string.feature_settings_section_account),
        testTag = SettingsTestTags.SECTION_ACCOUNT,
        modifier = modifier,
    ) {
        SettingsRow(
            title = stringResource(Res.string.feature_settings_consents_title),
            testTag = SettingsTestTags.CONSENTS_ROW,
            icon = Icons.Filled.Policy,
            subtitle = stringResource(Res.string.feature_settings_consents_subtitle),
            onClick = onNavigateToConsents,
        ) {
            SettingsRowChevron(
                description = opensInApp,
                testTag = SettingsTestTags.chevron(SettingsTestTags.CONSENTS_ROW),
            )
        }
        SettingsRow(
            title = stringResource(Res.string.feature_settings_profile_title),
            testTag = SettingsTestTags.PROFILE_ROW,
            icon = Icons.Filled.AccountCircle,
            subtitle = stringResource(Res.string.feature_settings_profile_subtitle),
            onClick = onNavigateToProfile,
        ) {
            SettingsRowChevron(
                description = opensInApp,
                testTag = SettingsTestTags.chevron(SettingsTestTags.PROFILE_ROW),
            )
        }
    }
}

/**
 * Privacy leaves for a browser and says so with the external-link glyph; Licences opens in the app
 * and gets a chevron. App Version closes the group as a static readout.
 */
@Composable
private fun AboutSection(
    appVersionLabel: String,
    onOpenPrivacy: () -> Unit,
    onNavigateToLicences: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val opensExternally = stringResource(Res.string.feature_settings_opens_externally_accessibility)
    val opensInApp = stringResource(Res.string.feature_settings_opens_in_app_accessibility)
    SettingsSection(
        title = stringResource(Res.string.feature_settings_section_about),
        testTag = SettingsTestTags.SECTION_ABOUT,
        modifier = modifier,
    ) {
        SettingsRow(
            title = stringResource(Res.string.feature_settings_privacy_title),
            testTag = SettingsTestTags.PRIVACY_ROW,
            icon = Icons.Filled.PrivacyTip,
            onClick = onOpenPrivacy,
        ) {
            SettingsRowExternalLink(
                description = opensExternally,
                testTag = SettingsTestTags.externalLink(SettingsTestTags.PRIVACY_ROW),
            )
        }
        SettingsRow(
            title = stringResource(Res.string.feature_settings_licences_title),
            testTag = SettingsTestTags.LICENCES_ROW,
            icon = Icons.Filled.Info,
            onClick = onNavigateToLicences,
        ) {
            SettingsRowChevron(
                description = opensInApp,
                testTag = SettingsTestTags.chevron(SettingsTestTags.LICENCES_ROW),
            )
        }
        AppVersionRow(appVersionLabel = appVersionLabel)
    }
}

/**
 * The build identity. No icon, no trailing affordance and no click handler — it reports a value
 * and offers nothing, and anything that looked tappable here would lead nowhere.
 */
@Composable
private fun AppVersionRow(appVersionLabel: String, modifier: Modifier = Modifier) {
    SettingsRow(
        title = stringResource(Res.string.feature_settings_app_version_title),
        testTag = SettingsTestTags.APP_VERSION_ROW,
        modifier = modifier,
        subtitle = appVersionLabel,
        subtitleTestTag = SettingsTestTags.APP_VERSION_VALUE,
    )
}
