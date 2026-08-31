/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.feature.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.core.model.user.DarkThemeConfig
import org.mifosx.openbanking.feature.settings.SettingsTestTags
import org.mifosx.openbanking.feature.settings.generated.resources.Res
import org.mifosx.openbanking.feature.settings.generated.resources.feature_settings_theme_option_accessibility
import org.mifosx.openbanking.feature.settings.generated.resources.feature_settings_theme_select_accessibility
import org.mifosx.openbanking.feature.settings.generated.resources.feature_settings_theme_title
import org.mifosx.openbanking.feature.settings.ui.themeLabel
import template.core.base.designsystem.theme.KptTheme

/**
 * The Theme row: the current value as the row's subtitle, and a dropdown chip repeating it.
 *
 * The picker offers every [DarkThemeConfig] entry.
 */
@Composable
internal fun ThemeDropdownRow(
    selected: DarkThemeConfig,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
    onSelect: (DarkThemeConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedLabel = stringResource(selected.themeLabel())
    SettingsRow(
        title = stringResource(Res.string.feature_settings_theme_title),
        testTag = SettingsTestTags.THEME_ROW,
        subtitle = selectedLabel,
        onClick = onToggle,
        modifier = modifier,
    ) {
        Box {
            ThemeChip(label = selectedLabel, onToggle = onToggle)
            ThemeMenu(
                currentSelection = selected,
                expanded = expanded,
                onDismiss = onDismiss,
                onSelect = onSelect,
            )
        }
    }
}

/** The control that opens the picker, labelled with the current theme. */
@Composable
private fun ThemeChip(label: String, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(
        Res.string.feature_settings_theme_select_accessibility,
        label,
    )
    Surface(
        color = KptTheme.colorScheme.surfaceContainerHighest,
        shape = KptTheme.shapes.small,
        modifier = modifier
            .clickable(onClick = onToggle)
            .testTag(SettingsTestTags.THEME_DROPDOWN)
            .semantics { contentDescription = description },
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = KptTheme.spacing.md,
                vertical = KptTheme.spacing.xs,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KptTheme.spacing.xs),
        ) {
            Text(
                text = label,
                style = KptTheme.typography.labelLarge,
                color = KptTheme.colorScheme.onSurface,
                modifier = Modifier.testTag(SettingsTestTags.THEME_VALUE),
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = KptTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(DesignToken.sizes.iconExtraSmall),
            )
        }
    }
}

/** The theme options, one per [DarkThemeConfig] entry. */
@Composable
private fun ThemeMenu(
    currentSelection: DarkThemeConfig,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSelect: (DarkThemeConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(SettingsTestTags.THEME_MENU),
    ) {
        DarkThemeConfig.entries.forEach { config ->
            val label = stringResource(config.themeLabel())
            val description = stringResource(
                Res.string.feature_settings_theme_option_accessibility,
                label,
            )
            DropdownMenuItem(
                text = { Text(text = label) },
                onClick = { onSelect(config) },
                modifier = Modifier
                    .testTag(SettingsTestTags.themeOption(config))
                    .semantics {
                        contentDescription = description
                        selected = config == currentSelection
                    },
            )
        }
    }
}
