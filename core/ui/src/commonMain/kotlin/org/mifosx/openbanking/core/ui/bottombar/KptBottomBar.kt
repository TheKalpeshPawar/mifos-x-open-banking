/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/mifos-x-open-banking/blob/dev/LICENSE
 */
package org.mifosx.openbanking.core.ui.bottombar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.mifosx.openbanking.core.ui.NavigationItem
import template.core.base.designsystem.theme.KptTheme

/**
 * Bottom navigation bar matching the screen previews: a `surfaceContainer` bar with a top
 * hairline, each item a centered icon-over-label column. The active item is enclosed
 * (icon + label together) in a rounded `primaryContainer` pill with `primary`-tinted content;
 * inactive items are `outline`-tinted. This is a custom layout because M3 NavigationBarItem
 * only highlights the icon, not the icon+label as the design requires.
 */
@Composable
fun KptBottomBar(
    navigationItems: List<NavigationItem>,
    selectedItem: NavigationItem?,
    onClick: (NavigationItem) -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.navigationBars,
) {
    Surface(
        color = KptTheme.colorScheme.surfaceContainer,
        contentColor = KptTheme.colorScheme.onSurface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(KptTheme.colorScheme.outlineVariant),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(windowInsets)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                navigationItems.forEach { navigationItem ->
                    KptBottomNavItem(
                        labelRes = navigationItem.labelRes,
                        contentDescriptionRes = navigationItem.contentDescriptionRes,
                        icon = if (selectedItem == navigationItem) {
                            navigationItem.selectedIcon
                        } else {
                            navigationItem.icon
                        },
                        isSelected = selectedItem == navigationItem,
                        onClick = { onClick(navigationItem) },
                        modifier = Modifier.testTag(tag = navigationItem.testTag),
                    )
                }
            }
        }
    }
}

@Composable
private fun KptBottomNavItem(
    labelRes: StringResource,
    contentDescriptionRes: StringResource,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (isSelected) KptTheme.colorScheme.primary else KptTheme.colorScheme.outline
    val containerColor = if (isSelected) {
        KptTheme.colorScheme.primaryContainer
    } else {
        KptTheme.colorScheme.surfaceContainer
    }
    val description = stringResource(contentDescriptionRes)
    Column(
        modifier = modifier
            .widthIn(min = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
        )
        Text(
            text = stringResource(labelRes),
            style = KptTheme.typography.labelSmall,
            color = contentColor,
        )
    }
}
