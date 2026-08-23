/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.designsystem.theme.DesignToken
import org.mifosx.openbanking.core.ui.NavigationItem
import template.core.base.designsystem.theme.KptTheme

/** Bottom-bar navigation item with icon and label resource slots. */
@Composable
fun RowScope.MifosNavigationBarItem(
    contentDescriptionRes: StringResource,
    selectedIconRes: ImageVector,
    label: StringResource,
    unselectedIconRes: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBarItem(
        icon = {
            Icon(
                imageVector = if (isSelected) selectedIconRes else unselectedIconRes,
                contentDescription = stringResource(contentDescriptionRes),
            )
        },
        label = {
            Text(
                modifier = Modifier.padding(KptTheme.spacing.xs),
                text = stringResource(label),
                style = KptTheme.typography.labelMedium,
                color = KptTheme.colorScheme.onSurface,
            )
        },
        selected = isSelected,
        alwaysShowLabel = true,
        onClick = onClick,
        modifier = modifier.padding(vertical = KptTheme.spacing.sm),
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = KptTheme.colorScheme.primary,
            unselectedIconColor = KptTheme.colorScheme.primary,
            indicatorColor = KptTheme.colorScheme.primary.copy(alpha = 0.3f),
        ),
    )
}

/** Navigation-rail item with icon and label resource slots. */
@Composable
fun ColumnScope.MifosNavigationRailItem(
    contentDescriptionRes: StringResource,
    selectedIconRes: ImageVector,
    label: StringResource,
    unselectedIconRes: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationRailItem(
        icon = {
            Icon(
                imageVector = if (isSelected) selectedIconRes else unselectedIconRes,
                contentDescription = stringResource(contentDescriptionRes),
            )
        },
        label = {
            Text(
                modifier = Modifier.padding(KptTheme.spacing.xs),
                text = stringResource(label),
                style = KptTheme.typography.labelMedium,
                color = KptTheme.colorScheme.onSurface,
            )
        },
        selected = isSelected,
        alwaysShowLabel = true,
        onClick = onClick,
        colors = NavigationRailItemDefaults.colors(
            selectedIconColor = KptTheme.colorScheme.primary,
            unselectedIconColor = KptTheme.colorScheme.primary,
            indicatorColor = KptTheme.colorScheme.primary.copy(alpha = 0.3f),
        ),
        modifier = modifier,
    )
}

/** Navigation rail with item list. */
@Composable
fun MifosNavigationRail(
    navigationItems: List<NavigationItem>,
    selectedItem: NavigationItem?,
    onClick: (NavigationItem) -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = NavigationRailDefaults.windowInsets,
) {
    Surface(
        color = KptTheme.colorScheme.surface,
        contentColor = KptTheme.colorScheme.onSurface,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .windowInsetsPadding(insets = windowInsets)
                .widthIn(min = DesignToken.sizes.navigationRailWidth)
                .padding(vertical = KptTheme.spacing.xs)
                .selectableGroup()
                .verticalScroll(state = rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                space = KptTheme.spacing.md,
                alignment = Alignment.CenterVertically,
            ),
        ) {
            navigationItems.forEach { navigationItem ->
                MifosNavigationRailItem(
                    contentDescriptionRes = navigationItem.contentDescriptionRes,
                    selectedIconRes = navigationItem.selectedIcon,
                    unselectedIconRes = navigationItem.icon,
                    isSelected = navigationItem == selectedItem,
                    label = navigationItem.labelRes,
                    onClick = { onClick(navigationItem) },
                    modifier = Modifier.testTag(tag = navigationItem.testTag),
                )
            }
        }
    }
}

/** Bottom app bar with item list. */
@Composable
fun MifosBottomBar(
    navigationItems: List<NavigationItem>,
    selectedItem: NavigationItem?,
    onClick: (NavigationItem) -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = BottomAppBarDefaults.windowInsets,
) {
    BottomAppBar(
        containerColor = KptTheme.colorScheme.surface,
        contentColor = KptTheme.colorScheme.onSurface,
        windowInsets = windowInsets,
        modifier = modifier
            .fillMaxWidth()
            .background(KptTheme.colorScheme.surface),
        tonalElevation = 0.dp,
    ) {
        navigationItems.forEach { navigationItem ->
            MifosNavigationBarItem(
                contentDescriptionRes = navigationItem.contentDescriptionRes,
                selectedIconRes = navigationItem.selectedIcon,
                unselectedIconRes = navigationItem.icon,
                label = navigationItem.labelRes,
                isSelected = selectedItem == navigationItem,
                onClick = { onClick(navigationItem) },
                modifier = Modifier.testTag(tag = navigationItem.testTag),
            )
        }
    }
}
