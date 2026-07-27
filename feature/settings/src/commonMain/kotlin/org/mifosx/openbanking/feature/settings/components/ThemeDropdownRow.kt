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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.model.user.DarkThemeConfig
import org.mifosx.openbanking.feature.settings.SettingsTestTags
import org.mifosx.openbanking.feature.settings.generated.resources.Res
import org.mifosx.openbanking.feature.settings.generated.resources.feature_settings_theme_option_accessibility
import org.mifosx.openbanking.feature.settings.generated.resources.feature_settings_theme_select_accessibility
import org.mifosx.openbanking.feature.settings.generated.resources.feature_settings_theme_title
import org.mifosx.openbanking.feature.settings.ui.labelResource

private val ChipHorizontalPadding = 12.dp
private val ChipVerticalPadding = 6.dp
private val ChipRadius = 8.dp
private val ChipGap = 4.dp
private val ChipIconSize = 18.dp

/**
 * The Theme row: the current value as the row's subtitle, and a dropdown chip repeating it.
 *
 * The value appears twice by design — the subtitle states what the theme is now, and the chip is
 * the control that changes it. A control labelled only "Theme" would leave the user opening the
 * menu to find out what is already set.
 *
 * Every option in [DarkThemeConfig] is offered. The list is the enum's own entries rather than a
 * hand-written three, so a config added to the model appears here without this file changing.
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
    val selectedLabel = stringResource(selected.labelResource())
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

@Composable
private fun ThemeChip(label: String, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(
        Res.string.feature_settings_theme_select_accessibility,
        label,
    )
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(ChipRadius),
        modifier = modifier
            .clickable(onClick = onToggle)
            .testTag(SettingsTestTags.THEME_DROPDOWN)
            .semantics { contentDescription = description },
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = ChipHorizontalPadding,
                vertical = ChipVerticalPadding,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ChipGap),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag(SettingsTestTags.THEME_VALUE),
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(ChipIconSize),
            )
        }
    }
}

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
            val label = stringResource(config.labelResource())
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
